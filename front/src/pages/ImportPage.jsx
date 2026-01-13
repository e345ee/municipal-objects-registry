import React from "react";
import CitiesApi from "../api/cities";
import ImportsApi from "../api/imports";
import { createStomp } from "../realtime/bus";

const CLIMATES = ["RAIN_FOREST", "HUMIDSUBTROPICAL", "TUNDRA"];
const GOVERNMENTS = [
  "DEMARCHY",
  "KLEPTOCRACY",
  "CORPORATOCRACY",
  "PLUTOCRACY",
  "THALASSOCRACY",
];

export default function ImportPage() {
  const fileInputRef = React.useRef(null);

  const [file, setFile] = React.useState(null);
  const [fileText, setFileText] = React.useState("");
  const [parsed, setParsed] = React.useState(null);
  const [parseError, setParseError] = React.useState("");
  const [validationErrors, setValidationErrors] = React.useState([]);

  const [uploading, setUploading] = React.useState(false);
  const [uploadResult, setUploadResult] = React.useState(null);
  const [uploadError, setUploadError] = React.useState(null);

  const [history, setHistory] = React.useState([]);
  const [histLoading, setHistLoading] = React.useState(false);
  const [histError, setHistError] = React.useState("");
  const [page, setPage] = React.useState(0);
  const [size, setSize] = React.useState(10);
  const [totalPages, setTotalPages] = React.useState(0);
  const [totalElements, setTotalElements] = React.useState(0);

  const resetImportState = React.useCallback(() => {
    setFileText("");
    setParsed(null);
    setParseError("");
    setValidationErrors([]);
    setUploadResult(null);
    setUploadError(null);
  }, []);

  React.useEffect(() => {
    if (!file) return;

    resetImportState();

    if (!/\.json$/i.test(file.name)) {
      setParseError("Файл должен иметь расширение .json");
      return;
    }

    if (file.size === 0) {
      setParseError("Файл пустой");
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const text = String(reader.result ?? "");
      setFileText(text);

      let data;
      try {
        data = JSON.parse(text);
      } catch (e) {
        setParseError(`Некорректный JSON: ${safeMsg(e?.message)}`);
        return;
      }

      if (!Array.isArray(data)) {
        setParseError("Ожидается JSON-массив объектов City");
        return;
      }

      setParsed(data);
      const errs = validateCities(data);
      setValidationErrors(errs);
    };
    reader.onerror = () => {
      setParseError("Не удалось прочитать файл");
    };
    reader.readAsText(file);
  }, [file, resetImportState]);

  const refreshHistory = React.useCallback(async () => {
    setHistLoading(true);
    setHistError("");
    try {
      const resp = await ImportsApi.list({ page, size });
      setHistory(resp?.content ?? []);
      setTotalPages(resp?.totalPages ?? 0);
      setTotalElements(resp?.totalElements ?? 0);

      if ((resp?.totalPages ?? 0) > 0 && page >= resp.totalPages) {
        const last = Math.max(0, resp.totalPages - 1);
        setPage(last);
      }
    } catch (e) {
      setHistError(extractApiMessage(e) || "Ошибка загрузки истории импорта");
    } finally {
      setHistLoading(false);
    }
  }, [page, size]);

  React.useEffect(() => {
    refreshHistory();
  }, [refreshHistory]);

  const refreshRef = React.useRef(null);
  React.useEffect(() => {
    refreshRef.current = refreshHistory;
  }, [refreshHistory]);

  React.useEffect(() => {
    const client = createStomp({
      topics: ["/topic/imports"],
      onMessage: () => {
        refreshRef.current?.();
      },
    });

    client.activate();
    return () => client.deactivate();
  }, []);

  const onUpload = async () => {
    setUploading(true);
    setUploadResult(null);
    setUploadError(null);

    try {
      const resp = await CitiesApi.importJson(file);
      setUploadResult(resp);
    } catch (e) {
      const resp = e?.response?.data;
      if (resp?.error === "validation_failed" && resp?.details?.items) {
        setUploadError({
          type: "validation_failed",
          message: resp?.message || "Импорт отклонён из-за ошибок валидации",
          items: resp.details.items,
        });
      } else {
        setUploadError({
          type: "error",
          message: extractApiMessage(e) || "Ошибка импорта",
        });
      }
    } finally {
      refreshHistory();
      setUploading(false);
    }
  };

  const canUpload = !!file && !parseError && validationErrors.length === 0 && !uploading;

  const isFirst = page <= 0;
  const isLast = totalPages === 0 || page + 1 >= totalPages;
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  return (
    <div style={{ padding: "0 20px 20px" }}>
      <h2 style={{ margin: "0 0 12px" }}>Импорт городов из JSON</h2>

      <div style={card}>
        <div style={{ display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
          <input
            ref={fileInputRef}
            type="file"
            accept="application/json,.json"
            onChange={(e) => {
              resetImportState();
              const f = e.target.files?.[0] || null;
              setFile(f);
              e.target.value = "";
            }}
          />
          <button
            onClick={onUpload}
            disabled={!canUpload}
            style={canUpload ? btnPrimary : btnDisabled}
          >
            Загрузить на сервер
          </button>
          <button
            onClick={() => {
              setFile(null);
              resetImportState();
              if (fileInputRef.current) fileInputRef.current.value = "";
            }}
            disabled={!file && !fileText}
          >
            Сбросить
          </button>
        </div>

        <div style={{ marginTop: 12, fontSize: 13, opacity: 0.9, lineHeight: 1.35 }}>
          Файл должен содержать JSON-массив объектов City. Поля <code>id</code> и <code>creationDate</code> должны отсутствовать.
          Для вложенных сущностей укажи либо <code>coordinatesId</code>, либо <code>coordinates</code> (обязательно), а также
          <code>governorId</code>/<code>governor</code> (опционально).
        </div>

        {parseError && (
          <div style={{ ...banner, ...bannerError }}>
            <div style={{ fontWeight: 600 }}>Ошибка файла</div>
            <div>{parseError}</div>
          </div>
        )}

        {validationErrors.length > 0 && (
          <div style={{ ...banner, ...bannerWarn }}>
            <div style={{ fontWeight: 600 }}>Клиентская валидация: найдено ошибок {validationErrors.length}</div>
            <div style={{ marginTop: 8, maxHeight: 240, overflow: "auto" }}>
              {renderErrors(validationErrors)}
            </div>
          </div>
        )}

        {uploadResult && (
          <div style={{ ...banner, ...bannerOk }}>
            <div style={{ fontWeight: 600 }}>Импорт завершён успешно</div>
            <div>Создано объектов: {uploadResult.created}</div>
            {Array.isArray(uploadResult.cityIds) && uploadResult.cityIds.length > 0 && (
              <div style={{ marginTop: 6, fontSize: 13 }}>
                IDs: {uploadResult.cityIds.join(", ")}
              </div>
            )}
          </div>
        )}

        {uploadError && uploadError.type === "validation_failed" && (
          <div style={{ ...banner, ...bannerError }}>
            <div style={{ fontWeight: 600 }}>Импорт отклонён сервером (валидация)</div>
            <div style={{ marginTop: 6 }}>{uploadError.message}</div>
            <div style={{ marginTop: 8, maxHeight: 240, overflow: "auto" }}>
              {renderServerItemErrors(uploadError.items)}
            </div>
          </div>
        )}

        {uploadError && uploadError.type !== "validation_failed" && (
          <div style={{ ...banner, ...bannerError }}>
            <div style={{ fontWeight: 600 }}>Ошибка импорта</div>
            <div>{uploadError.message}</div>
          </div>
        )}
      </div>

      <h2 style={{ margin: "20px 0 12px" }}>История импорта</h2>

      <div style={card}>
        <div style={{ display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
          <button onClick={refreshHistory} disabled={histLoading}>Обновить</button>

          <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
            Размер страницы:
            <select
              value={size}
              onChange={(e) => {
                const next = Number(e.target.value);
                setSize(next);
                if (page !== 0) setPage(0);
              }}
            >
              {[5, 10, 25, 50].map((n) => (
                <option key={n} value={n}>{n}</option>
              ))}
            </select>
          </label>

          <div style={{ marginLeft: "auto", fontSize: 13, opacity: 0.9 }}>
            {totalElements === 0 ? "Пусто" : `Показаны ${from}–${to} из ${totalElements}`}
          </div>
        </div>

        {histError && (
          <div style={{ ...banner, ...bannerError }}>
            <div style={{ fontWeight: 600 }}>Ошибка</div>
            <div>{histError}</div>
          </div>
        )}

        <div style={{ overflow: "auto", marginTop: 12 }}>
          <table style={table}>
            <thead>
              <tr>
                <TH>ID операции</TH>
                <TH>Статус</TH>
                <TH right>Добавлено</TH>
                <TH>Начало</TH>
                <TH>Окончание</TH>
              </tr>
            </thead>
            <tbody>
              {history.map((op) => (
                <tr key={op.id}>
                  <TD noEllipsis>{op.id}</TD>
                  <TD>
                    <span style={statusPill(op.status)}>{op.status}</span>
                  </TD>
                  <TD right noEllipsis>{op.addedCount ?? "—"}</TD>
                  <TD noEllipsis>{fmtDateTime(op.startedAt)}</TD>
                  <TD noEllipsis>{op.finishedAt ? fmtDateTime(op.finishedAt) : "—"}</TD>
                </tr>
              ))}
              {!histLoading && history.length === 0 && (
                <tr><td style={tdEmpty} colSpan={5}>История пуста</td></tr>
              )}
              {histLoading && (
                <tr><td style={tdEmpty} colSpan={5}>Загрузка…</td></tr>
              )}
            </tbody>
          </table>
        </div>

        <div style={pager}>
          <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={isFirst || histLoading}>Назад</button>
          <div>Стр. {totalPages === 0 ? 0 : page + 1} / {totalPages}</div>
          <button onClick={() => setPage((p) => p + 1)} disabled={isLast || histLoading}>Вперёд</button>
        </div>

        <div style={{ marginTop: 10, fontSize: 13, opacity: 0.85, lineHeight: 1.35 }}>
        </div>
      </div>

      {parsed && Array.isArray(parsed) && parsed.length > 0 && (
        <div style={{ ...card, marginTop: 20 }}>
          <div style={{ display: "flex", alignItems: "baseline", gap: 12, flexWrap: "wrap" }}>
            <h3 style={{ margin: 0 }}>Предпросмотр (первые 3 записи)</h3>
            <div style={{ fontSize: 13, opacity: 0.85 }}>Всего записей: {parsed.length}</div>
          </div>
          <pre style={pre}>
            {JSON.stringify(parsed.slice(0, 3), null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}

function validateCities(list) {
  const errors = [];
  for (let i = 0; i < list.length; i++) {
    const item = list[i];
    if (item === null || item === undefined) {
      errors.push({ index: i, field: "$", message: "Запись равна null" });
      continue;
    }
    if (typeof item !== "object" || Array.isArray(item)) {
      errors.push({ index: i, field: "$", message: "Ожидается объект" });
      continue;
    }
    errors.push(...validateCity(item, i));
  }
  return errors;
}

function validateCity(c, index) {
  const errs = [];

  if (c.id !== undefined && c.id !== null) {
    errs.push({ index, field: "id", message: "Поле id должно отсутствовать (генерируется на сервере)" });
  }
  if (c.creationDate !== undefined && c.creationDate !== null) {
    errs.push({ index, field: "creationDate", message: "Поле creationDate должно отсутствовать (генерируется на сервере)" });
  }

  if (!isNonEmptyString(c.name)) {
    errs.push({ index, field: "name", message: "name: строка не может быть пустой" });
  }

  if (!isInteger(c.area) || Number(c.area) <= 0) {
    errs.push({ index, field: "area", message: "area: целое число > 0" });
  }

  if (c.population === undefined || c.population === null) {
    errs.push({ index, field: "population", message: "population: поле не может быть null" });
  } else if (!isInteger(c.population) || Number(c.population) <= 0) {
    errs.push({ index, field: "population", message: "population: целое число > 0" });
  }

  if (typeof c.capital !== "boolean") {
    errs.push({ index, field: "capital", message: "capital: требуется boolean" });
  }

  if (!isInteger(c.metersAboveSeaLevel)) {
    errs.push({ index, field: "metersAboveSeaLevel", message: "metersAboveSeaLevel: требуется целое число" });
  }

  if (c.telephoneCode === undefined || c.telephoneCode === null) {
    errs.push({ index, field: "telephoneCode", message: "telephoneCode: поле обязательно" });
  } else if (!isInteger(c.telephoneCode) || Number(c.telephoneCode) <= 0 || Number(c.telephoneCode) > 100000) {
    errs.push({ index, field: "telephoneCode", message: "telephoneCode: целое число в диапазоне 1..100000" });
  }

  if (!isNonEmptyString(c.climate) || !CLIMATES.includes(String(c.climate).trim())) {
    errs.push({ index, field: "climate", message: `climate: одно из ${CLIMATES.join(", ")}` });
  }

  if (c.government !== undefined && c.government !== null && String(c.government).trim() !== "") {
    if (!GOVERNMENTS.includes(String(c.government).trim())) {
      errs.push({ index, field: "government", message: `government: одно из ${GOVERNMENTS.join(", ")} либо null` });
    }
  }

  if (c.establishmentDate !== undefined && c.establishmentDate !== null && String(c.establishmentDate).trim() !== "") {
    if (!isIsoDateStrict(String(c.establishmentDate).trim())) {
      errs.push({ index, field: "establishmentDate", message: "establishmentDate: дата в формате yyyy-MM-dd" });
    }
  }

  const hasCoordinatesId = c.coordinatesId !== undefined && c.coordinatesId !== null && String(c.coordinatesId).trim() !== "";
  const hasCoordinatesObj = c.coordinates !== undefined && c.coordinates !== null;
  if (hasCoordinatesId && hasCoordinatesObj) {
    errs.push({ index, field: "coordinates", message: "Укажи либо coordinatesId, либо coordinates (не оба сразу)" });
  }
  if (!hasCoordinatesId && !hasCoordinatesObj) {
    errs.push({ index, field: "coordinates", message: "coordinates: требуется указать coordinatesId или coordinates" });
  }
  if (hasCoordinatesId) {
    if (!isInteger(c.coordinatesId) || Number(c.coordinatesId) <= 0) {
      errs.push({ index, field: "coordinatesId", message: "coordinatesId: целое число > 0" });
    }
  }
  if (hasCoordinatesObj) {
    errs.push(...validateCoordinates(c.coordinates, index));
  }

  const hasGovernorId = c.governorId !== undefined && c.governorId !== null && String(c.governorId).trim() !== "";
  const hasGovernorObj = c.governor !== undefined && c.governor !== null;
  if (hasGovernorId && hasGovernorObj) {
    errs.push({ index, field: "governor", message: "Укажи либо governorId, либо governor (не оба сразу)" });
  }
  if (hasGovernorId) {
    if (!isInteger(c.governorId) || Number(c.governorId) <= 0) {
      errs.push({ index, field: "governorId", message: "governorId: целое число > 0" });
    }
  }
  if (hasGovernorObj) {
    errs.push(...validateHuman(c.governor, index));
  }

  return errs;
}

function validateCoordinates(coords, index) {
  const errs = [];
  if (typeof coords !== "object" || coords === null || Array.isArray(coords)) {
    errs.push({ index, field: "coordinates", message: "coordinates: ожидается объект" });
    return errs;
  }

  if (coords.id !== undefined && coords.id !== null) {
    errs.push({ index, field: "coordinates.id", message: "coordinates.id должен отсутствовать" });
  }

  if (coords.x === undefined || coords.x === null || coords.x === "") {
    errs.push({ index, field: "coordinates.x", message: "coordinates.x обязателен" });
  } else if (!isNumber(coords.x) || Number(coords.x) > 460) {
    errs.push({ index, field: "coordinates.x", message: "coordinates.x: число <= 460" });
  }

  if (coords.y === undefined || coords.y === null || coords.y === "") {
    errs.push({ index, field: "coordinates.y", message: "coordinates.y обязателен" });
  } else if (!isNumber(coords.y)) {
    errs.push({ index, field: "coordinates.y", message: "coordinates.y: число" });
  }

  return errs;
}

function validateHuman(h, index) {
  const errs = [];
  if (typeof h !== "object" || h === null || Array.isArray(h)) {
    errs.push({ index, field: "governor", message: "governor: ожидается объект" });
    return errs;
  }

  if (h.id !== undefined && h.id !== null) {
    errs.push({ index, field: "governor.id", message: "governor.id должен отсутствовать" });
  }

  if (h.height === undefined || h.height === null || h.height === "") {
    errs.push({ index, field: "governor.height", message: "governor.height обязателен" });
  } else if (!isNumber(h.height) || Number(h.height) <= 0) {
    errs.push({ index, field: "governor.height", message: "governor.height: число > 0" });
  }

  return errs;
}

function renderErrors(errors) {
  const grouped = new Map();
  for (const e of errors) {
    const key = e.index;
    if (!grouped.has(key)) grouped.set(key, []);
    grouped.get(key).push(e);
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
      {[...grouped.entries()].sort((a, b) => a[0] - b[0]).map(([idx, list]) => (
        <div key={idx} style={errBlock}>
          <div style={{ fontWeight: 600 }}>Запись #{idx}</div>
          <ul style={{ margin: "6px 0 0", paddingLeft: 18 }}>
            {list.map((x, i) => (
              <li key={i} style={{ marginBottom: 4 }}>
                <code>{x.field}</code>: {x.message}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  );
}

function renderServerItemErrors(items) {
  if (!Array.isArray(items)) return null;
  return renderErrors(items.map((x) => ({
    index: x.index,
    field: x.field,
    message: x.message
  })));
}

function extractApiMessage(err) {
  const resp = err?.response;
  if (resp?.data?.message) return String(resp.data.message);
  if (resp?.status) return `HTTP ${resp.status}`;
  return err?.message ? String(err.message) : "";
}

function safeMsg(s, max = 240) {
  const t = String(s ?? "");
  return t.length > max ? t.slice(0, max) + "…" : t;
}

function isNonEmptyString(v) {
  return typeof v === "string" && v.trim().length > 0;
}

function isNumber(v) {
  if (v === "" || v === null || v === undefined) return false;
  const n = Number(v);
  return Number.isFinite(n);
}

function isInteger(v) {
  if (v === "" || v === null || v === undefined) return false;
  const n = Number(v);
  return Number.isFinite(n) && Number.isInteger(n);
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

function fmtDateTime(v) {
  if (!v) return "—";
  const s = String(v);
  return s.replace("T", " ");
}

function statusPill(status) {
  const base = {
    display: "inline-block",
    padding: "3px 10px",
    borderRadius: 999,
    fontSize: 12,
    border: "1px solid #ddd",
    background: "#fff",
  };
  if (status === "SUCCESS") return { ...base, borderColor: "#b7e3c0", background: "#eefaf1" };
  if (status === "FAILED") return { ...base, borderColor: "#f2b8b5", background: "#fff0ef" };
  if (status === "RUNNING") return { ...base, borderColor: "#c9d7ff", background: "#f0f5ff" };
  return base;
}

const card = {
  background: "#fff",
  border: "1px solid #ddd",
  borderRadius: 10,
  padding: 14,
};

const banner = {
  marginTop: 12,
  padding: 12,
  borderRadius: 10,
  border: "1px solid #ddd",
  lineHeight: 1.35,
};
const bannerOk = { borderColor: "#b7e3c0", background: "#eefaf1" };
const bannerWarn = { borderColor: "#f1d59b", background: "#fff7e6" };
const bannerError = { borderColor: "#f2b8b5", background: "#fff0ef" };

const btnPrimary = {
  padding: "8px 14px",
  borderRadius: 8,
  border: "1px solid #0077cc",
  background: "#0077cc",
  color: "#fff",
  cursor: "pointer",
};

const btnDisabled = {
  ...btnPrimary,
  opacity: 0.55,
  cursor: "not-allowed",
};

const table = {
  width: "100%",
  borderCollapse: "collapse",
};

function TH({ children, right }) {
  return (
    <th
      style={{
        borderBottom: "1px solid #ddd",
        padding: "8px 10px",
        textAlign: right ? "right" : "left",
        fontSize: 13,
        background: "#fafafa",
        position: "sticky",
        top: 0,
      }}
    >
      {children}
    </th>
  );
}

function TD({ children, right, noEllipsis }) {
  return (
    <td
      style={{
        borderBottom: "1px solid #eee",
        padding: "8px 10px",
        textAlign: right ? "right" : "left",
        fontSize: 13,
        whiteSpace: noEllipsis ? "normal" : "nowrap",
        overflow: "hidden",
        textOverflow: noEllipsis ? "clip" : "ellipsis",
        maxWidth: 420,
      }}
    >
      {children}
    </td>
  );
}

const tdEmpty = {
  padding: 14,
  textAlign: "center",
  opacity: 0.7,
};

const pager = {
  display: "flex",
  gap: 8,
  marginTop: 12,
  alignItems: "center",
  justifyContent: "flex-end",
};

const errBlock = {
  background: "#fff",
  border: "1px solid rgba(0,0,0,0.08)",
  borderRadius: 10,
  padding: 10,
};

const pre = {
  marginTop: 12,
  background: "#0b1020",
  color: "#e7e7e7",
  padding: 12,
  borderRadius: 10,
  overflow: "auto",
  maxHeight: 340,
  fontSize: 12,
  lineHeight: 1.35,
};
