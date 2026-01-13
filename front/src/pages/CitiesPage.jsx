import React from "react";
import { CitiesApi } from "../api/cities";
import { createStomp } from "../realtime/bus";
import CityCreateDialog from "./CityCreateDialog";
import CityDeleteDialog from "./CityDeleteDialog";
import CityEditDialog from "./CityEditDialog";

const ALL_SORT_FIELDS = new Set([
  "id", "name", "creationDate", "area", "population",
  "establishmentDate", "capital", "metersAboveSeaLevel",
  "telephoneCode", "climate", "government",
  "coordinatesId", "coordinatesX", "coordinatesY",
  "governorId", "governorHeight",
]);

const CLIMATES = ["RAIN_FOREST","HUMIDSUBTROPICAL","TUNDRA"];
const GOVERNMENTS = ["DEMARCHY","KLEPTOCRACY","CORPORATOCRACY","PLUTOCRACY","THALASSOCRACY"];

export default function CitiesPage() {
  const [rows, setRows] = React.useState([]);
  const [page, setPage] = React.useState(0);
  const [size, setSize] = React.useState(25);
  const [totalPages, setTotalPages] = React.useState(0);
  const [totalElements, setTotalElements] = React.useState(0);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState("");

  const [infoOpen, setInfoOpen] = React.useState(false);
  const [infoTitle, setInfoTitle] = React.useState("");
  const [infoMessage, setInfoMessage] = React.useState("");
  const [infoDetails, setInfoDetails] = React.useState("");

  const showInfo = React.useCallback(({ title, message, details = "" }) => {
    setInfoTitle(title ?? "Сообщение");
    setInfoMessage(message ?? "");
    setInfoDetails(details ?? "");
    setInfoOpen(true);
  }, []);

  const [draft, setDraft] = React.useState({
    name: "", climate: "", government: "",
    area: "", population: "", metersAboveSeaLevel: "",
    telephoneCode: "", capital: "",
    coordinatesId: "", governorId: "", governorIdIsNull: "",
    creationDate: "", establishmentDate: "",
  });

  const [errors, setErrors] = React.useState({});
  const [filters, setFilters] = React.useState({});

  const [sortBy, setSortBy] = React.useState("id");
  const [dir, setDir] = React.useState("asc");

  const [openCreate, setOpenCreate] = React.useState(false);
  const [delOpen, setDelOpen] = React.useState(false);
  const [delCity, setDelCity] = React.useState(null);
  const [editOpen, setEditOpen] = React.useState(false);
  const [editCity, setEditCity] = React.useState(null);

  const fetchPage = React.useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const resp = await CitiesApi.list({ page, size, sortBy, dir, filters });
      if (resp?.totalPages > 0 && page >= resp.totalPages) {
        const last = resp.totalPages - 1;
        const retry = await CitiesApi.list({ page: last, size, sortBy, dir, filters });
        setRows(retry.content ?? []);
        setPage(last);
        setTotalPages(retry.totalPages ?? 0);
        setTotalElements(retry.totalElements ?? 0);
      } else {
        setRows(resp.content ?? []);
        setTotalPages(resp.totalPages ?? 0);
        setTotalElements(resp.totalElements ?? 0);
      }
    } catch (e) {
      console.error(e);
      const msg = "Не удалось загрузить список городов";
      setError(msg);

      showInfo({
        title: "Ошибка загрузки",
        message: msg,
        details: e?.message ? String(e.message) : "",
      });
    } finally {
      setLoading(false);
    }
  }, [page, size, filters, sortBy, dir, showInfo]);

  React.useEffect(() => { fetchPage(); }, [fetchPage]);

  React.useEffect(() => {
    const client = createStomp(() => { fetchPage(); }, ["/topic/cities", "/topic/coordinates", "/topic/humans"]);
    client.activate();
    return () => client.deactivate();
  }, [fetchPage]);

  const fmt = (v) => (v === null || v === undefined || v === "" ? "-" : String(v));
  const isFirst = page <= 0;
  const isLast  = totalPages === 0 || page + 1 >= totalPages;
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to   = Math.min((page + 1) * size, totalElements);

  const validators = {
    area: (s) => isInt(s, {min: 0}) || "Area: целое число ≥ 0",
    population: (s) => isInt(s, {min: 0}) || "Population: целое число ≥ 0",
    metersAboveSeaLevel: (s) => isInt(s, {min: -100000, max: 100000}) || "Meters: целое число (можно отриц.)",
    telephoneCode: (s) => isInt(s, {min: 0, max: 999999}) || "Tel. code: целое число ≥ 0",
    coordinatesId: (s) => isInt(s, {min: 1}) || "Coordinates ID: целое число ≥ 1",
    governorId: (s) => isInt(s, {min: 1}) || "Governor ID: целое число ≥ 1",
    climate: (s) => (!s || CLIMATES.includes(s)) || "Неверный climate",
    government: (s) => (!s || GOVERNMENTS.includes(s)) || "Неверный government",
    capital: (s) => (!s || s === "true" || s === "false") || "Capital: true/false",
    creationDate: (s) => (!s || isIsoDateStrict(s)) || "creationDate: YYYY-MM-DD",
    establishmentDate: (s) => (!s || isIsoDateStrict(s)) || "establishmentDate: YYYY-MM-DD",
    name: (_) => true,
    governorIdIsNull: (s) => (!s || s==="true" || s==="false") || "Значение true/false",
  };

  const validateDraft = (d) => {
    const errs = {};
    for (const [k, v] of Object.entries(d)) {
      const s = String(v ?? "").trim();
      if (!checkNotEmpty(s)) continue;
      const vfn = validators[k];
      if (vfn) {
        const ok = vfn(s);
        if (ok !== true) errs[k] = ok;
      }
    }

    if (String(d.governorId ?? "").trim() && String(d.governorIdIsNull ?? "") === "true") {
      errs.governorId = "Нельзя вместе с 'Governor is NULL? = Да'";
      errs.governorIdIsNull = "Нельзя вместе с 'Governor ID'";
    }
    return errs;
  };

  const onDraftChange = (k, raw) => {
    const val = sanitize(k, raw);
    setDraft((prev) => {
      const next = { ...prev, [k]: val };
      setErrors(validateDraft(next));
      return next;
    });
  };

  const applyFilters = () => {
    const errs = validateDraft(draft);
    setErrors(errs);
    if (Object.keys(errs).length) return;

    const cleaned = normalizeExclusive(draft);
    const next = toQueryFilters(cleaned);
    const changed = JSON.stringify(next) !== JSON.stringify(filters);

    setFilters(next);
    if (page !== 0) setPage(0);
    else if (changed) fetchPage();
  };

  const resetFilters = () => {
    const cleared = {
      name: "", climate: "", government: "",
      area: "", population: "", metersAboveSeaLevel: "",
      telephoneCode: "", capital: "",
      coordinatesId: "", governorId: "", governorIdIsNull: "",
      creationDate: "", establishmentDate: "",
    };
    setDraft(cleared);
    setErrors({});
    setFilters({});
    setPage(0);
  };

  const toggleSort = (field) => {
    if (!ALL_SORT_FIELDS.has(field)) return;
    setPage(0);
    setDir((prev) => (sortBy === field ? (prev === "asc" ? "desc" : "asc") : "asc"));
    setSortBy(field);
  };

  const isActive = (field) => sortBy === field;
  const renderSortIcon = (field) => {
    if (!isActive(field)) return <span style={{ opacity: .35 }}>↕</span>;
    return <span>{dir === "asc" ? "↑" : "↓"}</span>;
  };

  const hasErrors = Object.keys(errors).length > 0;

  const askDelete = (city) => {
    setDelCity(city);
    setDelOpen(true);
  };
  const onDeleted = () => {
    fetchPage();
    showInfo({ title: "Удаление", message: "Город удалён." });
  };

  const askEdit = (city) => {
    setEditCity(city);
    setEditOpen(true);
  };
  const onUpdated = () => {
    fetchPage();
    showInfo({ title: "Изменение", message: "Изменения сохранены." });
  };

  return (
    <div style={{ padding: 24, maxWidth: 1600, margin: "0 auto" }}>
      <header style={{ display: "flex", gap: 12, marginBottom: 12, alignItems: "center", flexWrap: "wrap" }}>
        <h2 style={{ margin: 0 }}>Города</h2>
        <button style={btn} onClick={()=>setOpenCreate(true)}>+ Создать город</button>

        <details style={filtersWrap}>
          <summary style={{ cursor: "pointer", userSelect: "none" }}>Фильтры</summary>

          <div style={filtersGrid}>
            <Input
              placeholder="Name содержит…"
              value={draft.name}
              onChange={(e)=>onDraftChange("name", e.target.value)}
              error={errors.name}
            />

            <Select
              value={draft.climate}
              onChange={(e)=>onDraftChange("climate", e.target.value)}
              error={errors.climate}
              options={[["", "Climate: любой"], ...CLIMATES.map(v=>[v,v])]}
            />

            <Select
              value={draft.government}
              onChange={(e)=>onDraftChange("government", e.target.value)}
              error={errors.government}
              options={[["", "Government: любое"], ...GOVERNMENTS.map(v=>[v,v])]}
            />

            <InputNum
              placeholder="Area ="
              value={draft.area}
              onChange={(v)=>onDraftChange("area", v)}
              error={errors.area}
            />
            <InputNum
              placeholder="Population ="
              value={draft.population}
              onChange={(v)=>onDraftChange("population", v)}
              error={errors.population}
            />
            <InputNum
              placeholder="MetersAboveSeaLevel ="
              value={draft.metersAboveSeaLevel}
              onChange={(v)=>onDraftChange("metersAboveSeaLevel", v)}
              error={errors.metersAboveSeaLevel}
            />
            <InputNum
              placeholder="TelephoneCode ="
              value={draft.telephoneCode}
              onChange={(v)=>onDraftChange("telephoneCode", v)}
              error={errors.telephoneCode}
            />

            <Select
              value={draft.capital}
              onChange={(e)=>onDraftChange("capital", e.target.value)}
              error={errors.capital}
              options={[["", "Capital: любой"], ["true","Да"], ["false","Нет"]]}
            />

            <InputNum
              placeholder="Coordinates ID ="
              value={draft.coordinatesId}
              onChange={(v)=>onDraftChange("coordinatesId", v)}
              error={errors.coordinatesId}
            />
            <InputNum
              placeholder="Governor ID ="
              value={draft.governorId}
              onChange={(v)=>onDraftChange("governorId", v)}
              error={errors.governorId}
            />
            <Select
              value={draft.governorIdIsNull}
              onChange={(e)=>onDraftChange("governorIdIsNull", e.target.value)}
              error={errors.governorIdIsNull}
              options={[["", "Governor is NULL?"], ["true","Да"], ["false","Нет"]]}
            />

            <Input
              placeholder="creationDate (YYYY-MM-DD)"
              value={draft.creationDate}
              onChange={(e)=>onDraftChange("creationDate", e.target.value)}
              error={errors.creationDate}
            />
            <Input
              placeholder="establishmentDate (YYYY-MM-DD)"
              value={draft.establishmentDate}
              onChange={(e)=>onDraftChange("establishmentDate", e.target.value)}
              error={errors.establishmentDate}
            />
          </div>

          <div style={{ display: "flex", gap: 8, marginTop: 8, alignItems: "center" }}>
            <button style={btn} onClick={applyFilters} disabled={hasErrors}>Применить</button>
            <button style={btnLight} onClick={resetFilters}>Сброс</button>
            {hasErrors && (
              <span style={{ color: "crimson", fontSize: 13 }}>
                Исправьте ошибки фильтров
              </span>
            )}
          </div>
        </details>

        <div style={{ marginLeft: "auto", display: "flex", gap: 8, alignItems: "center" }}>
          <span style={{ opacity: 0.7, fontSize: 13 }}>
            Показаны {from}–{to} из {totalElements}
          </span>
          <select value={size} onChange={(e) => { setPage(0); setSize(Number(e.target.value)); }} title="Размер страницы">
            <option value={10}>10</option><option value={25}>25</option><option value={50}>50</option><option value={100}>100</option>
          </select>
          <button onClick={() => fetchPage()} disabled={loading}>Обновить</button>
        </div>
      </header>

      {loading && <div>Загрузка…</div>}
      {error && <div style={{ color: "crimson", marginBottom: 8 }}>{error}</div>}

      <div style={{ overflowX: "auto", border: "1px solid #e5e7eb", borderRadius: 8 }}>
        <table style={table}>
          <thead>
            <tr>
              <TH w={90}  right onClick={()=>toggleSort("id")}>ID {renderSortIcon("id")}</TH>
              <TH w={220} onClick={()=>toggleSort("name")}>Name {renderSortIcon("name")}</TH>
              <TH w={150} onClick={()=>toggleSort("creationDate")}>Creation Date {renderSortIcon("creationDate")}</TH>
              <TH w={110} right onClick={()=>toggleSort("area")}>Area {renderSortIcon("area")}</TH>
              <TH w={130} right onClick={()=>toggleSort("population")}>Population {renderSortIcon("population")}</TH>
              <TH w={170} onClick={()=>toggleSort("establishmentDate")}>Est. Date {renderSortIcon("establishmentDate")}</TH>
              <TH w={90}  onClick={()=>toggleSort("capital")}>Capital {renderSortIcon("capital")}</TH>
              <TH w={150} right onClick={()=>toggleSort("metersAboveSeaLevel")}>Meters {renderSortIcon("metersAboveSeaLevel")}</TH>
              <TH w={140} right onClick={()=>toggleSort("telephoneCode")}>Tel. Code {renderSortIcon("telephoneCode")}</TH>
              <TH w={160} onClick={()=>toggleSort("climate")}>Climate {renderSortIcon("climate")}</TH>
              <TH w={180} onClick={()=>toggleSort("government")}>Government {renderSortIcon("government")}</TH>

              <TH w={120} right onClick={()=>toggleSort("coordinatesId")}>Coords ID {renderSortIcon("coordinatesId")}</TH>
              <TH w={110} right onClick={()=>toggleSort("coordinatesX")}>X {renderSortIcon("coordinatesX")}</TH>
              <TH w={110} right onClick={()=>toggleSort("coordinatesY")}>Y {renderSortIcon("coordinatesY")}</TH>

              <TH w={130} right onClick={()=>toggleSort("governorId")}>Gov ID {renderSortIcon("governorId")}</TH>
              <TH w={150} right onClick={()=>toggleSort("governorHeight")}>Gov Height {renderSortIcon("governorHeight")}</TH>

              <TH w={200} right>Действия</TH>
            </tr>
          </thead>
          <tbody>
            {rows.map((c, i) => {
              const coords = c.coordinates || {};
              const gov = c.governor || {};
              return (
                <tr key={c.id} style={i % 2 ? trZebra : undefined}>
                  <TD right noEllipsis>{fmt(c.id)}</TD>
                  <TD>{fmt(c.name)}</TD>
                  <TD>{fmt(c.creationDate)}</TD>
                  <TD right noEllipsis>{fmt(c.area)}</TD>
                  <TD right noEllipsis>{fmt(c.population)}</TD>
                  <TD>{fmt(c.establishmentDate)}</TD>
                  <TD>{c.capital ? "Да" : "Нет"}</TD>
                  <TD right noEllipsis>{fmt(c.metersAboveSeaLevel)}</TD>
                  <TD right noEllipsis>{fmt(c.telephoneCode)}</TD>
                  <TD>{fmt(c.climate)}</TD>
                  <TD>{fmt(c.government)}</TD>

                  <TD right noEllipsis>{fmt(c.coordinatesId ?? coords.id)}</TD>
                  <TD right noEllipsis>{fmt(coords.x)}</TD>
                  <TD right noEllipsis>{fmt(coords.y)}</TD>

                  <TD right noEllipsis>{fmt(c.governorId ?? gov.id)}</TD>
                  <TD right noEllipsis>{fmt(gov.height)}</TD>

                  <TD right>
                    <div style={actionsWrap}>
                      <button style={btn} onClick={()=>askEdit(c)}>Изменить</button>
                      <button style={btnDanger} onClick={()=>askDelete(c)}>Удалить</button>
                    </div>
                  </TD>
                </tr>
              );
            })}
            {!loading && rows.length === 0 && (
              <tr><td style={tdEmpty} colSpan={18}>Пусто</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div style={pager}>
        <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={isFirst || loading}>Назад</button>
        <div>Стр. {totalPages === 0 ? 0 : page + 1} / {totalPages}</div>
        <button onClick={() => setPage((p) => p + 1)} disabled={isLast || loading}>Вперёд</button>
      </div>

      <CityCreateDialog
        open={openCreate}
        onClose={()=>setOpenCreate(false)}
        onCreated={()=>{
          fetchPage();
          showInfo({ title: "Создание", message: "Город создан." });
        }}
      />

      <CityDeleteDialog
        open={delOpen}
        onClose={()=>{ setDelOpen(false); setDelCity(null); }}
        city={delCity}
        onDeleted={onDeleted}
      />

      <CityEditDialog
        open={editOpen}
        onClose={()=>{ setEditOpen(false); setEditCity(null); }}
        city={editCity}
        onUpdated={onUpdated}
      />

      <InfoDialog
        open={infoOpen}
        title={infoTitle}
        message={infoMessage}
        details={infoDetails}
        onClose={()=>setInfoOpen(false)}
      />
    </div>
  );
}

function checkNotEmpty(s) {
  return !(s === "" || s === null || s === undefined);
}

function isInt(s, {min = -Infinity, max = Infinity} = {}) {
  if (s === "" || s === null || s === undefined) return true;
  if (!/^-?\d+$/.test(String(s))) return false;
  const n = Number(s);
  return Number.isInteger(n) && n >= min && n <= max;
}
function isIsoDateStrict(s) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(s));
  if (!m) return false;
  const y = Number(m[1]), mo = Number(m[2]), d = Number(m[3]);
  if (mo < 1 || mo > 12) return false;
  if (d < 1 || d > 31) return false;
  const dt = new Date(Date.UTC(y, mo - 1, d));
  return dt.getUTCFullYear() === y && (dt.getUTCMonth() + 1) === mo && dt.getUTCDate() === d;
}
function sanitize(field, raw) {
  const t = String(raw ?? "");
  switch (field) {
    case "area":
    case "population":
    case "telephoneCode":
    case "coordinatesId":
    case "governorId":
      return t.replace(/[^\d]/g, "");
    case "metersAboveSeaLevel":
      return t.replace(/[^\d-]/g, "").replace(/(?!^)-/g, "");
    default:
      return t;
  }
}

function normalizeExclusive(f) {
  const out = { ...f };
  if (String(out.governorId ?? "").trim()) out.governorIdIsNull = "";
  if (String(out.governorIdIsNull ?? "") === "true") out.governorId = "";
  return out;
}
function toQueryFilters(f) {
  const out = {};
  const isEmpty = (v) => v === null || v === undefined || String(v).trim() === "";
  for (const [k, v] of Object.entries(f)) {
    if (isEmpty(v)) continue;

    if ((k === "creationDate" || k === "establishmentDate") && !isIsoDateStrict(v)) continue;
    out[k] = String(v).trim();
  }
  return out;
}

const Input = ({ value, onChange, placeholder, error }) => (
  <div style={{ display: "grid", gap: 4 }}>
    <input
      style={{ ...input, borderColor: error ? "#fca5a5" : input.borderColor }}
      value={value}
      onChange={onChange}
      placeholder={placeholder}
    />
    {error && <span style={{ color: "crimson", fontSize: 12 }}>{error}</span>}
  </div>
);

const InputNum = ({ value, onChange, placeholder, error }) => (
  <Input
    value={value}
    onChange={(e)=>onChange(e.target.value)}
    placeholder={placeholder}
    error={error}
  />
);

const Select = ({ value, onChange, options, error }) => (
  <div style={{ display: "grid", gap: 4 }}>
    <select
      style={{ ...input, borderColor: error ? "#fca5a5" : input.borderColor }}
      value={value}
      onChange={onChange}
    >
      {options.map(([val, label]) => (
        <option key={val} value={val}>{label}</option>
      ))}
    </select>
    {error && <span style={{ color: "crimson", fontSize: 12 }}>{error}</span>}
  </div>
);

const TH = ({ children, w, right, onClick }) => (
  <th
    style={{
      ...th,
      width: w,
      textAlign: right ? "right" : "left",
      cursor: onClick ? "pointer" : "default",
      userSelect: "none",
    }}
    onClick={onClick}
  >
    {children}
  </th>
);

const TD = ({ children, right, noEllipsis }) => (
  <td
    style={{
      ...(noEllipsis ? tdNoEllipsis : td),
      textAlign: right ? "right" : "left",
    }}
  >
    {children}
  </td>
);

function InfoDialog({ open, title, message, details, onClose }) {
  if (!open) return null;

  const onBackdrop = (e) => {
    if (e.target === e.currentTarget) onClose?.();
  };

  return (
    <div style={modalBackdrop} onMouseDown={onBackdrop}>
      <div style={modalCard} role="dialog" aria-modal="true" aria-label={title || "Диалог"}>
        <div style={modalHeader}>
          <div style={{ fontWeight: 700 }}>{title || "Сообщение"}</div>
          <button style={modalCloseBtn} onClick={onClose} aria-label="Закрыть">✕</button>
        </div>

        {message && <div style={{ marginTop: 10 }}>{message}</div>}

        {details ? (
          <details style={modalDetails}>
            <summary style={{ cursor: "pointer" }}>Подробности</summary>
            <pre style={modalPre}>{String(details)}</pre>
          </details>
        ) : null}

        <div style={modalFooter}>
          <button style={btn} onClick={onClose}>Ок</button>
        </div>
      </div>
    </div>
  );
}

const table = {
  width: "100%",
  borderCollapse: "separate",
  borderSpacing: 0,
  fontSize: 14,
};

const th = {
  position: "sticky",
  top: 0,
  background: "#fff",
  zIndex: 1,
  textAlign: "left",
  borderBottom: "1px solid #e5e7eb",
  padding: "8px 10px",
  whiteSpace: "nowrap",
  overflow: "hidden",
  textOverflow: "ellipsis",
};

const td = {
  borderBottom: "1px solid #f0f2f5",
  padding: "6px 10px",
  verticalAlign: "middle",
  whiteSpace: "nowrap",
  overflow: "hidden",
  textOverflow: "ellipsis",
};

const tdNoEllipsis = {
  ...td,
  overflow: "visible",
  textOverflow: "clip",
};

const tdEmpty = { ...td, textAlign: "center", color: "#6b7280" };
const trZebra = { background: "#fafafa" };
const actionsWrap = { display: "inline-flex", gap: 6, justifyContent: "flex-end" };
const btn = { padding: "6px 12px", fontSize: 13, cursor: "pointer", borderRadius: 6, border: "1px solid #d1d5db", background: "#eef2ff" };
const btnLight = { ...btn, background: "#fff" };
const btnDanger = { ...btn, borderColor: "#fecaca", background: "#fff1f2" };
const pager = { display: "flex", gap: 8, marginTop: 12, alignItems: "center", justifyContent: "flex-end" };
const filtersWrap = { padding: 12, border: "1px solid #e5e7eb", borderRadius: 10, background: "#fff", minWidth: 260 };
const filtersGrid = { display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: 8, marginTop: 10 };
const input = { padding: "6px 10px", border: "1px solid #d1d5db", borderRadius: 8, outline: "none" };

const modalBackdrop = {
  position: "fixed",
  inset: 0,
  background: "rgba(0,0,0,0.35)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: 16,
  zIndex: 1000,
};

const modalCard = {
  width: "min(560px, 100%)",
  background: "#fff",
  borderRadius: 12,
  border: "1px solid #e5e7eb",
  boxShadow: "0 18px 40px rgba(0,0,0,0.15)",
  padding: 14,
};

const modalHeader = {
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: 12,
};

const modalFooter = {
  display: "flex",
  justifyContent: "flex-end",
  marginTop: 14,
};

const modalCloseBtn = {
  cursor: "pointer",
  border: "1px solid #e5e7eb",
  background: "#fff",
  borderRadius: 8,
  width: 32,
  height: 32,
  lineHeight: "30px",
};

const modalDetails = {
  marginTop: 10,
  border: "1px solid #e5e7eb",
  borderRadius: 10,
  padding: 10,
  background: "#fafafa",
};

const modalPre = {
  margin: "10px 0 0",
  whiteSpace: "pre-wrap",
  wordBreak: "break-word",
  fontSize: 12,
  color: "#374151",
};