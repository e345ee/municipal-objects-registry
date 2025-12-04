import React from "react";
import { CitiesAnalyticsApi } from "../api/citiesAnalytics";

export default function FeaturesPage() {
  const [avgTel, setAvgTel] = React.useState(null);
  const [avgTelErr, setAvgTelErr] = React.useState("");

  const [uniqueMeters, setUniqueMeters] = React.useState([]);
  const [uniqueErr, setUniqueErr] = React.useState("");

  const [namesPrefix, setNamesPrefix] = React.useState("");
  const [names, setNames] = React.useState([]);
  const [namesErr, setNamesErr] = React.useState("");


  const [point, setPoint] = React.useState({ x: "", y: "" });
  const [xErr, setXErr] = React.useState("");
  const [yErr, setYErr] = React.useState("");
  const [distLargest, setDistLargest] = React.useState(null);
  const [distLargestErr, setDistLargestErr] = React.useState("");

  const [distOldest, setDistOldest] = React.useState(null);
  const [distOldestErr, setDistOldestErr] = React.useState("");

  const [loading, setLoading] = React.useState(false);

  const load = React.useCallback(async () => {
    setLoading(true);
    setAvgTelErr(""); setUniqueErr(""); setDistOldestErr("");
    try {
      const [avg, meters, dOld] = await Promise.allSettled([
        CitiesAnalyticsApi.averageTelephoneCode(),
        CitiesAnalyticsApi.uniqueMetersAboveSeaLevel(),
        CitiesAnalyticsApi.distanceFromOriginToOldest(),
      ]);

      if (avg.status === "fulfilled") { setAvgTel(avg.value ?? null); }
      else { setAvgTel(null); setAvgTelErr(humanizeAxiosError(avg.reason, "Не удалось получить средний код")); }

      if (meters.status === "fulfilled") { setUniqueMeters(Array.isArray(meters.value) ? meters.value : []); }
      else { setUniqueMeters([]); setUniqueErr(humanizeAxiosError(meters.reason, "Не удалось получить уникальные высоты")); }

      if (dOld.status === "fulfilled") { setDistOldest(dOld.value ?? null); }
      else {
        if (is404(dOld.reason)) setDistOldestErr("Нет городов с установленным establishmentDate");
        else setDistOldestErr(humanizeAxiosError(dOld.reason, "Не удалось рассчитать дистанцию до самого старого города"));
        setDistOldest(null);
      }
    } finally { setLoading(false); }
  }, []);

  React.useEffect(() => { load(); }, [load]);


  const searchNames = async () => {
    setNamesErr("");
    try {
      if (!namesPrefix) { setNames([]); return; }
      const data = await CitiesAnalyticsApi.namesStartingWith(namesPrefix);
      setNames(Array.isArray(data) ? data : []);
    } catch (e) {
      setNames([]); setNamesErr(humanizeAxiosError(e, "Ошибка при загрузке имён"));
    }
  };


  const NUM_RE = /^-?\d{1,7}(\.\d{1,6})?$/; 
  const sanitize = (str) => str.replace(/[eE]/g, ""); 

  const onXChange = (v) => {
    const val = sanitize(v);
    setPoint(p => ({ ...p, x: val }));
    setXErr(val === "" || NUM_RE.test(val) ? "" : "Формат: до 7 цифр, до 6 после точки");
  };
  const onYChange = (v) => {
    const val = sanitize(v);
    setPoint(p => ({ ...p, y: val }));
    setYErr(val === "" || NUM_RE.test(val) ? "" : "Формат: до 7 цифр, до 6 после точки");
  };
  const xyValid = point.x !== "" && point.y !== "" && NUM_RE.test(point.x) && NUM_RE.test(point.y);

  const calcDistanceToLargest = async () => {
    setDistLargestErr("");
    if (!xyValid) { setDistLargestErr("Заполните X и Y корректно"); return; }
    try {
      const d = await CitiesAnalyticsApi.distanceToLargest(Number(point.x), Number(point.y));
      setDistLargest(d ?? null);
    } catch (e) {
      setDistLargest(null);
      setDistLargestErr(humanizeAxiosError(e, "Ошибка при расчёте расстояния"));
    }
  };

  const fmtNum = (v) => (typeof v === "number" && Number.isFinite(v) ? v : "—");
  const to2 = (v) => (typeof v === "number" && Number.isFinite(v) ? v.toFixed(2) : "—");

  return (
    <div style={wrap}>
      <h2 style={{ margin: 0 }}>Аналитика городов</h2>

      <section style={grid}>

        <div style={card}>
          <h3 style={h3}>Средний telephoneCode</h3>
          <div style={big}>{loading ? "Загрузка…" : fmtNum(avgTel)}</div>
          {avgTelErr && <div style={errBox}>{avgTelErr}</div>}
          <div style={hint}>Рассчитать среднее значение поля telephoneCode для всех объектов.</div>
        </div>


        <div style={card}>
          <h3 style={h3}>Уникальные metersAboveSeaLevel</h3>
          <div style={chipsWrap}>
            {uniqueMeters.length
              ? uniqueMeters.map((v, i) => (
                  <div key={i} style={chip}>
                    <span style={{ fontSize: String(v).length > 3 ? 12 : 14 }}>{String(v)}</span>
                  </div>
                ))
              : <span style={{ opacity: .7 }}>{loading ? "Загрузка…" : "нет значений"}</span>}
          </div>
          {uniqueErr && <div style={errBox}>{uniqueErr}</div>}
          <div style={hint}>Вернуть массив уникальных значений поля metersAboveSeaLevel по всем объектам.</div>
        </div>


        <div style={card}>
          <h3 style={h3}>Имена городов по префиксу</h3>
          <div style={searchRow}>
            <input
              placeholder="например, New"
              value={namesPrefix}
              onChange={(e) => setNamesPrefix(e.target.value)}
              style={searchInput}
              title="Префикс имени города"
            />
            <button onClick={searchNames} style={btn}>Искать</button>
            <button onClick={()=>{ setNamesPrefix(""); setNames([]); setNamesErr(""); }} style={btnLight}>Сброс</button>
          </div>
          {namesErr && <div style={errBox}>{namesErr}</div>}
          <ul style={{ margin: "10px 0 0 18px", maxHeight: 220, overflow: "auto" }}>
            {names.map(c => (
              <li key={c.id}>
                {c.name} <span style={{ opacity: .6 }}>#{c.id}</span>
              </li>
            ))}
            {!names.length && namesPrefix && !namesErr && <li style={{ opacity: .7 }}>Ничего не найдено</li>}
          </ul>
          <div style={hint}>Вернуть массив объектов, значение поля name которых начинается с заданной подстроки.</div>
        </div>


        <div style={card}>
          <h3 style={h3}>Расстояния</h3>
          <div style={{ display: "grid", gap: 12 }}>
            <div>
              <div style={{ marginBottom: 6, fontWeight: 600 }}>До крупнейшего по площади</div>
              <div style={rowCenter}>
                <label style={lbl}>
                  <span style={lblCap}>X</span>
                  <input
                    value={point.x}
                    onChange={(e)=>onXChange(e.target.value)}
                    inputMode="decimal"
                    maxLength={16}
                    style={{ ...inputSm, ...(xErr ? invalid : null) }}
                    placeholder="например, 10.5"
                  />
                  {xErr && <span style={fieldErr}>{xErr}</span>}
                </label>
                <label style={lbl}>
                  <span style={lblCap}>Y</span>
                  <input
                    value={point.y}
                    onChange={(e)=>onYChange(e.target.value)}
                    inputMode="decimal"
                    maxLength={16}
                    style={{ ...inputSm, ...(yErr ? invalid : null) }}
                    placeholder="например, -3.2"
                  />
                  {yErr && <span style={fieldErr}>{yErr}</span>}
                </label>
                <button onClick={calcDistanceToLargest} style={{ ...btn, marginLeft: 4 }} disabled={!xyValid}>
                  Рассчитать
                </button>
                <span style={{ marginLeft: "auto", fontWeight: 600 }}>{to2(distLargest)}</span>
              </div>
              {distLargestErr && <div style={{ ...errBox, marginTop: 8 }}>{distLargestErr}</div>}
              <div style={hint}>Рассчитать длину маршрута до города с наибольшей площадью.</div>
            </div>

            <div style={{ borderTop: "1px dashed #e5e7eb", paddingTop: 10 }}>
              <div style={{ marginBottom: 6, fontWeight: 600 }}>От (0,0) до самого старого</div>
              <div style={rowCenter}>
                <span>Дистанция:</span>
                <span style={{ fontWeight: 600, marginLeft: 6 }}>{to2(distOldest)}</span>
                <button onClick={load} style={{ ...btnLight, marginLeft: "auto" }}>Обновить</button>
              </div>
              {distOldestErr && <div style={{ ...errBox, marginTop: 8 }}>{distOldestErr}</div>}
              <div style={hint}>Рассчитать длину маршрута от точки (0,0) до города, основанного раньше остальных.</div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}


function isAxiosError(e) { return !!(e && e.isAxiosError); }
function is404(e) { return isAxiosError(e) && e.response && e.response.status === 404; }
function humanizeAxiosError(e, fallback) {
  if (!isAxiosError(e)) return fallback;
  const status = e.response?.status;
  const msg = e.response?.data?.message || e.message;
  return status ? `${status}: ${msg || fallback}` : (msg || fallback);
}


const wrap = { padding: 24, maxWidth: 1100, margin: "0 auto", display: "grid", gap: 16 };
const grid = { display: "grid", gap: 12, gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))" };
const card = { padding: 16, border: "1px solid #e5e7eb", borderRadius: 12, background: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,.04)", display: "grid", gap: 10 };
const h3 = { margin: 0 };
const big = { fontSize: 28, fontWeight: 700 };

const chipsWrap = { display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center" };
const chip = { width: 40, height: 40, borderRadius: "50%", display: "grid", placeItems: "center", border: "1px solid #e5e7eb", background: "#f8fafc" };

const searchRow = { display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" };
const searchInput = { padding: "6px 10px", borderRadius: 8, border: "1px solid #d1d5db", outline: "none", flex: "0 1 200px", maxWidth: 260 };

const rowCenter = { display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" };

const inputSm = { padding: "6px 10px", borderRadius: 8, border: "1px solid #d1d5db", outline: "none", minWidth: 120 };
const invalid = { borderColor: "#fca5a5", background: "#fff7f7" };
const lbl = { display: "grid", gap: 4 };
const lblCap = { fontSize: 12, opacity: .7 };
const btn = { padding: "6px 12px", borderRadius: 8, border: "1px solid #d1d5db", background: "#f3f4f6", cursor: "pointer" };
const btnLight = { ...btn, background: "#fff" };
const hint = { fontSize: 12, opacity: .7 };
const errBox = { padding: 8, background: "#FEF2F2", border: "1px solid #FECACA", color: "#991B1B", borderRadius: 8, fontSize: 12 };
const fieldErr = {
  marginTop: 2,
  fontSize: 12,
  color: "#991B1B",
  lineHeight: 1.2,
};