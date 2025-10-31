import React from "react";
import Modal from "../components/Modal";
import { CitiesApi } from "../api/cities";
import { CoordinatesApi } from "../api/coordinates";
import { HumansApi } from "../api/humans";

const CLIMATES = ["RAIN_FOREST","HUMIDSUBTROPICAL","TUNDRA"];
const GOVERNMENTS = ["DEMARCHY","KLEPTOCRACY","CORPORATOCRACY","PLUTOCRACY","THALASSOCRACY"];

export default function CityEditDialog({ open, onClose, city, onUpdated }) {
  
  const [name, setName] = React.useState("");
  const [area, setArea] = React.useState("");
  const [population, setPopulation] = React.useState("");
  const [establishmentDate, setEstablishmentDate] = React.useState("");
  const [capital, setCapital] = React.useState(false);
  const [metersAboveSeaLevel, setMeters] = React.useState("");
  const [telephoneCode, setTel] = React.useState("");
  const [climate, setClimate] = React.useState("");
  const [government, setGovernment] = React.useState("");

 
  const [coordMode, setCoordMode] = React.useState("keep"); 
  const [coordinatesId, setCoordinatesId] = React.useState("");
  const [coordX, setCoordX] = React.useState("");
  const [coordY, setCoordY] = React.useState("");


  const [govMode, setGovMode] = React.useState("keep"); 
  const [governorId, setGovernorId] = React.useState("");
  const [govHeight, setGovHeight] = React.useState("");


  const [loading, setLoading] = React.useState(false);
  const [err, setErr] = React.useState("");
  const [fieldErr, setFieldErr] = React.useState({});


  const [coordCheck, setCoordCheck] = React.useState({ state: "idle", text: "" });
  const [govCheck, setGovCheck] = React.useState({ state: "idle", text: "" });     

  React.useEffect(() => {
    if (open && city) {

      setName(city.name ?? "");
      setArea(numToStr(city.area));
      setPopulation(numToStr(city.population));
      setEstablishmentDate(toDateInput(city.establishmentDate ?? ""));
      setCapital(!!city.capital);
      setMeters(numToStr(city.metersAboveSeaLevel));
      setTel(numToStr(city.telephoneCode));
      setClimate(city.climate ?? "");
      setGovernment(city.government ?? ""); 
      setCoordMode("keep");
      setCoordinatesId(numToStr(city.coordinatesId ?? city.coordinates?.id));
      setCoordX(numToStr(city.coordinates?.x));
      setCoordY(numToStr(city.coordinates?.y));

      setGovMode("keep");
      setGovernorId(numToStr(city.governorId ?? city.governor?.id));
      setGovHeight(numToStr(city.governor?.height));

      setErr("");
      setFieldErr({});
      setLoading(false);
      setCoordCheck({ state: "idle", text: "" });
      setGovCheck({ state: "idle", text: "" });
    }
  }, [open, city]);

  if (!open || !city) return null;

  const onSubmit = async () => {
    const vErr = validateAll({
      name, area, population, establishmentDate, metersAboveSeaLevel, telephoneCode,
      climate, government, coordMode, coordinatesId, coordX, coordY, govMode, governorId, govHeight
    });
    setFieldErr(vErr);
    if (Object.keys(vErr).length) return;

    const dto = {
      name: name.trim(),
      area: Number(area),
      population: Number(population),
      capital: Boolean(capital),
      metersAboveSeaLevel: metersAboveSeaLevel === "" ? null : Number(metersAboveSeaLevel),
      telephoneCode: telephoneCode === "" ? null : Number(telephoneCode),
      climate,
      government: government === "" ? null : government, 
      establishmentDate: establishmentDate || null,       


      coordinatesSpecified: coordMode !== "keep",
      governorSpecified: govMode !== "keep",
    };

    if (coordMode === "byId") {
      dto.coordinatesId = Number(coordinatesId);
    } else if (coordMode === "create") {
      dto.coordinates = {
        x: Number(coordX),
        y: Number(coordY),
      };
    }


    if (govMode === "byId") {
      dto.governorId = Number(governorId);
    } else if (govMode === "create") {
      dto.governor = { height: Number(govHeight) };
    } else if (govMode === "null") {
      dto.governor = null;
    }

    setLoading(true);
    setErr("");
    try {
      await CitiesApi.update(city.id, dto);
      onUpdated?.(city.id);
      onClose?.();
    } catch (e) {
      const msg =
        e?.response?.data?.message ||
        e?.response?.data?.error ||
        e?.message ||
        "Не удалось обновить город";
      setErr(msg);
    } finally {
      setLoading(false);
    }
  };


  const doCheckCoordinatesId = async () => {
    const id = Number(String(coordinatesId || "").trim());
    if (!id) { setCoordCheck({ state: "fail", text: "Введите корректный ID" }); return; }
    try {
      setCoordCheck({ state: "loading", text: "Проверяем..." });
      const c = await CoordinatesApi.get(id);
      setCoordCheck({ state: "ok", text: `Найдены координаты: x=${c.x}, y=${c.y}` });
    } catch {
      setCoordCheck({ state: "fail", text: "Координаты не найдены" });
    }
  };

  const doCheckGovernorId = async () => {
    const id = Number(String(governorId || "").trim());
    if (!id) { setGovCheck({ state: "fail", text: "Введите корректный ID" }); return; }
    try {
      setGovCheck({ state: "loading", text: "Проверяем..." });
      const g = await HumansApi.get(id);
      setGovCheck({ state: "ok", text: `Найден губернатор: height=${g.height}` });
    } catch {
      setGovCheck({ state: "fail", text: "Губернатор не найден" });
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`Изменение города #${city.id}`}
      footer={(
        <>
          <button type="button" style={btnLight} onClick={onClose} disabled={loading}>Отмена</button>
          <button type="button" style={btnPrimary} onClick={onSubmit} disabled={loading}>
            {loading ? "Сохраняю…" : "Сохранить"}
          </button>
        </>
      )}
    >
      <div style={{ display: "grid", gap: 12 }}>

        <Grid>
          <Field label="Name *" error={fieldErr.name}>
            <input style={input} value={name} onChange={(e)=>setName(e.target.value)} placeholder="Город…" />
          </Field>
          <Field label="Area * (int > 0)" error={fieldErr.area}>
            <Num value={area} onChange={setArea} allowNegative={false} />
          </Field>
          <Field label="Population * (long > 0)" error={fieldErr.population}>
            <Num value={population} onChange={setPopulation} allowNegative={false} maxLen={18} />
          </Field>


          <Field label="Establishment Date (YYYY-MM-DD)" error={fieldErr.establishmentDate}>
            <input
              style={input}
              value={establishmentDate}
              onChange={(e)=> setEstablishmentDate(maskIsoDate(e.target.value))}
              onPaste={(e)=> {
                e.preventDefault();
                const txt = (e.clipboardData?.getData("text") ?? "");
                setEstablishmentDate(maskIsoDate(txt));
              }}
              inputMode="numeric"
              placeholder="YYYY-MM-DD"
              maxLength={10}
            />
          </Field>

          <Field label="Capital">
            <select style={input} value={capital ? "true" : "false"} onChange={(e)=>setCapital(e.target.value === "true")}>
              <option value="false">Нет</option>
              <option value="true">Да</option>
            </select>
          </Field>
          <Field label="Meters Above Sea Level (int)" error={fieldErr.metersAboveSeaLevel}>
            <Num value={metersAboveSeaLevel} onChange={setMeters} allowNegative={true} />
          </Field>
          <Field label="Telephone Code (1..100000)" error={fieldErr.telephoneCode}>
            <Num value={telephoneCode} onChange={setTel} allowNegative={false} max={100000} />
          </Field>
          <Field label="Climate *" error={fieldErr.climate}>
            <select style={input} value={climate} onChange={(e)=>setClimate(e.target.value)}>
              <option value="">— выберите —</option>
              {CLIMATES.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
          </Field>


          <Field label="Government (можно пусто = NULL)" error={fieldErr.government}>
            <select style={input} value={government} onChange={(e)=>setGovernment(e.target.value)}>
              <option value="">— нет (NULL) —</option>
              {GOVERNMENTS.map(g => <option key={g} value={g}>{g}</option>)}
            </select>
          </Field>
        </Grid>


        <section style={section}>
          <div style={sectionTitle}>Координаты</div>
          <div style={radioRow}>
            <label><input type="radio" name="coordMode" checked={coordMode==="keep"} onChange={()=>setCoordMode("keep")} /> Не менять</label>
            <label><input type="radio" name="coordMode" checked={coordMode==="byId"} onChange={()=>setCoordMode("byId")} /> Привязать по ID</label>
            <label><input type="radio" name="coordMode" checked={coordMode==="create"} onChange={()=>setCoordMode("create")} /> Обновить старую</label>
          </div>

          {coordMode === "byId" && (
            <Grid cols={2}>
              <Field label="Coordinates ID *" error={fieldErr.coordinatesId}>
                <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: 8, alignItems: "center" }}>
                  <Num value={coordinatesId} onChange={setCoordinatesId} allowNegative={false} />
                  <button type="button" style={btnLight} onClick={doCheckCoordinatesId}>Проверить</button>
                </div>
                {coordCheck.state !== "idle" && (
                  <div style={{ fontSize: 12, marginTop: 4, color: coordCheck.state==="ok" ? "#065F46" : (coordCheck.state==="fail" ? "#B91C1C" : "#374151") }}>
                    {coordCheck.text}
                  </div>
                )}
              </Field>
            </Grid>
          )}

          {coordMode === "create" && (
            <Grid cols={2}>
              <Field label="X (float, ≤ 460) *" error={fieldErr.coordX}>
                <Float value={coordX} onChange={setCoordX} max={460} />
              </Field>
              <Field label="Y (float, not null) *" error={fieldErr.coordY}>
                <Float value={coordY} onChange={setCoordY} />
              </Field>
            </Grid>
          )}

          {coordMode === "keep" && (
            <Note>
              Текущие: ID {city.coordinatesId ?? city.coordinates?.id ?? "—"}
              {city.coordinates ? ` (x=${city.coordinates.x}, y=${city.coordinates.y})` : ""}
            </Note>
          )}
        </section>


        <section style={section}>
          <div style={sectionTitle}>Губернатор</div>
          <div style={radioRow}>
            <label><input type="radio" name="govMode" checked={govMode==="keep"} onChange={()=>setGovMode("keep")} /> Не менять</label>
            <label><input type="radio" name="govMode" checked={govMode==="byId"} onChange={()=>setGovMode("byId")} /> Привязать по ID</label>
            <label><input type="radio" name="govMode" checked={govMode==="create"} onChange={()=>setGovMode("create")} /> Создать нового</label>
            <label><input type="radio" name="govMode" checked={govMode==="null"} onChange={()=>setGovMode("null")} /> Снять (NULL)</label>
          </div>

          {govMode === "byId" && (
            <Grid cols={2}>
              <Field label="Governor ID *" error={fieldErr.governorId}>
                <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: 8, alignItems: "center" }}>
                  <Num value={governorId} onChange={setGovernorId} allowNegative={false} />
                  <button type="button" style={btnLight} onClick={doCheckGovernorId}>Проверить</button>
                </div>
                {govCheck.state !== "idle" && (
                  <div style={{ fontSize: 12, marginTop: 4, color: govCheck.state==="ok" ? "#065F46" : (govCheck.state==="fail" ? "#B91C1C" : "#374151") }}>
                    {govCheck.text}
                  </div>
                )}
              </Field>
            </Grid>
          )}

          {govMode === "create" && (
            <Grid cols={2}>
              <Field label="Height (float > 0) *" error={fieldErr.govHeight}>
                <Float value={govHeight} onChange={setGovHeight} minExclusive={0} />
              </Field>
            </Grid>
          )}

          {govMode === "keep" && (
            <Note>
              Текущий: ID {city.governorId ?? city.governor?.id ?? "—"}
              {city.governor ? ` (height=${city.governor.height})` : ""}
            </Note>
          )}
        </section>

        {err && <div style={errBox}>{err}</div>}
      </div>
    </Modal>
  );
}


const numToStr = (v) => (v === null || v === undefined ? "" : String(v));

const sanitizeInt = (s, { allowNegative=false } = {}) =>
  String(s ?? "")
    .replace(allowNegative ? /[^0-9-]/g : /[^0-9]/g, "")
    .replace(/(?!^)-/g, "");

const sanitizeFloat = (s) =>
  String(s ?? "")
    .replace(/,/g, ".")
    .replace(/[^0-9.\-]/g, "")
    .replace(/(?!^)-/g, "")
    .replace(/(\..*)\./g, "$1"); 


function maskIsoDate(raw) {
  const digits = String(raw || "").replace(/\D/g, "").slice(0, 8); // YYYY MM DD = 8 цифр
  const y = digits.slice(0, 4);
  const m = digits.slice(4, 6);
  const d = digits.slice(6, 8);
  let out = y;
  if (m) out += "-" + m;
  if (d) out += "-" + d;
  return out;
}
function isRealIsoDate(s) {
  if (!s) return true; // пусто — ок
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return false;
  const [y, m, d] = s.split("-").map(Number);
  if (m < 1 || m > 12) return false;
  if (d < 1 || d > 31) return false;
  const dt = new Date(Date.UTC(y, m - 1, d));
  return (
    dt.getUTCFullYear() === y &&
    dt.getUTCMonth() + 1 === m &&
    dt.getUTCDate() === d
  );
}
function toDateInput(d) {
  const s = String(d || "");
  if (!s) return "";
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;
  const dt = new Date(s);
  if (isNaN(dt.getTime())) return "";
  const y = dt.getUTCFullYear();
  const m = String(dt.getUTCMonth() + 1).padStart(2, "0");
  const day = String(dt.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function validateAll(v) {
  const e = {};
  const isInt = (s) => /^-?\d+$/.test(s);
  const isFloat = (s) => /^-?\d+(\.\d+)?$/.test(s);

  if (!v.name?.trim()) e.name = "Обязательно";

  if (!isInt(v.area) || Number(v.area) <= 0) e.area = "int > 0";
  if (!isInt(v.population) || Number(v.population) <= 0) e.population = "long > 0";


  if (v.establishmentDate && !/^\d{4}-\d{2}-\d{2}$/.test(v.establishmentDate)) {
    e.establishmentDate = "YYYY-MM-DD";
  } else if (v.establishmentDate && !isRealIsoDate(v.establishmentDate)) {
    e.establishmentDate = "Несуществующая дата";
  }

  if (v.metersAboveSeaLevel !== "" && !isInt(v.metersAboveSeaLevel)) e.metersAboveSeaLevel = "int";
  if (v.telephoneCode !== "" && (!isInt(v.telephoneCode) || Number(v.telephoneCode) <= 0 || Number(v.telephoneCode) > 100000)) e.telephoneCode = "1..100000";

  if (!CLIMATES.includes(v.climate)) e.climate = "обязательно";


  if (v.government !== "" && !GOVERNMENTS.includes(v.government)) e.government = "некорректное значение";

  if (v.coordMode === "byId") {
    if (!isInt(v.coordinatesId) || Number(v.coordinatesId) <= 0) e.coordinatesId = "ID ≥ 1";
  }
  if (v.coordMode === "create") {
    if (!isFloat(v.coordX)) e.coordX = "float";
    else if (Number(v.coordX) > 460) e.coordX = "≤ 460";
    if (!isFloat(v.coordY)) e.coordY = "float";
  }

  if (v.govMode === "byId") {
    if (!isInt(v.governorId) || Number(v.governorId) <= 0) e.governorId = "ID ≥ 1";
  }
  if (v.govMode === "create") {
    if (!isFloat(v.govHeight) || Number(v.govHeight) <= 0) e.govHeight = "> 0";
  }
  return e;
}


function Grid({ children, cols = 3 }) {
  return <div style={{ display: "grid", gridTemplateColumns: `repeat(${cols}, minmax(180px, 1fr))`, gap: 8 }}>{children}</div>;
}
function Field({ label, error, children }) {
  return (
    <div style={{ display: "grid", gap: 4 }}>
      <div style={{ fontSize: 12, color: "#374151" }}>{label}</div>
      {children}
      {error && <div style={{ color: "crimson", fontSize: 12 }}>{error}</div>}
    </div>
  );
}
function Num({ value, onChange, allowNegative=false, max, maxLen }) {
  return (
    <input
      style={input}
      value={value}
      onChange={(e) => {
        let s = sanitizeInt(e.target.value, { allowNegative });
        if (maxLen) s = s.slice(0, maxLen);
        if (max !== undefined && s !== "" && Number(s) > max) s = String(max);
        onChange(s);
      }}
      inputMode="numeric"
      placeholder="0"
    />
  );
}
function Float({ value, onChange, minExclusive, max }) {
  return (
    <input
      style={input}
      value={value}
      onChange={(e) => {
        let s = sanitizeFloat(e.target.value);
        if (s && (s === "-" || s === "." || s === "-.")) { onChange(s); return; }
        if (s !== "" && !isNaN(Number(s))) {
          const n = Number(s);
          if (minExclusive !== undefined && n <= minExclusive) return;
          if (max !== undefined && n > max) s = String(max);
        }
        onChange(s);
      }}
      inputMode="decimal"
      placeholder="0.0"
    />
  );
}

const input = { padding: "6px 10px", border: "1px solid #d1d5db", borderRadius: 8, outline: "none" };
const btnLight = { padding: "8px 12px", borderRadius: 8, background: "#fff", border: "1px solid #d1d5db", cursor: "pointer" };
const btnPrimary = { ...btnLight, background: "#eef2ff", borderColor: "#c7d2fe", color: "#1e3a8a" };
const section = { border: "1px solid #e5e7eb", borderRadius: 10, padding: 10, background: "#fff" };
const sectionTitle = { fontSize: 13, fontWeight: 600, color: "#374151", marginBottom: 8 };
const radioRow = { display: "flex", gap: 16, color: "#374151", fontSize: 14 };
const Note = ({ children }) => <div style={{ fontSize: 12, color: "#6b7280" }}>{children}</div>;
const errBox = { background: "#FEF2F2", color: "#991B1B", border: "1px solid #FCA5A5", padding: 8, borderRadius: 8, fontSize: 12 };
