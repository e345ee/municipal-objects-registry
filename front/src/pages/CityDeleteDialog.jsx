import React from "react";
import Modal from "../components/Modal";
import { CitiesApi } from "../api/cities";

export default function CityDeleteDialog({ open, onClose, city, onDeleted }) {
  const [deleteGovernorIfOrphan, setDelGov] = React.useState(false);
  const [deleteCoordinatesIfOrphan, setDelCoords] = React.useState(false);
  const [loading, setLoading] = React.useState(false);
  const [err, setErr] = React.useState("");

  React.useEffect(() => {
    if (open) {
      setDelGov(false);
      setDelCoords(false);
      setErr("");
      setLoading(false);
    }
  }, [open]);

  if (!open || !city) return null;

  const coords = city.coordinates || {};
  const gov = city.governor || {};

  const onSubmit = async () => {
    setLoading(true);
    setErr("");
    try {
      await CitiesApi.remove(city.id, {
        deleteGovernorIfOrphan,
        deleteCoordinatesIfOrphan,
      });
      onDeleted?.(city.id);
      onClose?.();
    } catch (e) {
      const msg =
        e?.response?.data?.message ||
        e?.response?.data?.error ||
        e?.message ||
        "Не удалось удалить город";
      setErr(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Удаление города"
      footer={(
        <>
          <button type="button" style={btnLight} onClick={onClose} disabled={loading}>Отмена</button>
          <button type="button" style={btnDanger} onClick={onSubmit} disabled={loading}>
            {loading ? "Удаляю…" : "Удалить"}
          </button>
        </>
      )}
    >
      <div style={{ display: "grid", gap: 10 }}>
        <div style={warn}>
          Вы собираетесь удалить город <b>#{city.id}</b>{city.name ? ` (${city.name})` : ""}.
        </div>

        <div style={box}>
          <div style={{ fontSize: 13, marginBottom: 6, color: "#374151" }}>
            Связанные сущности:
          </div>
          <ul style={{ margin: 0, paddingLeft: 18, color: "#374151" }}>
            <li>Координаты: {city.coordinatesId ?? coords.id ?? "—"}{coords.id ? ` (x=${coords.x}, y=${coords.y})` : ""}</li>
            <li>Губернатор: {city.governorId ?? gov.id ?? "—"}{gov.id ? ` (height=${gov.height})` : ""}</li>
          </ul>
        </div>

        <label style={check}>
          <input
            type="checkbox"
            checked={deleteCoordinatesIfOrphan}
            onChange={(e)=>setDelCoords(e.target.checked)}
          />
          <span>Удалить координаты, <i>если не используются другими городами</i></span>
        </label>
        <label style={check}>
          <input
            type="checkbox"
            checked={deleteGovernorIfOrphan}
            onChange={(e)=>setDelGov(e.target.checked)}
          />
          <span>Удалить человека, <i>если не используется другими городами</i></span>
        </label>

        <div style={note}>
          Связнные сущности не удаляться, если они привязаны к другим сущностям.
        </div>

        {err && <div style={errBox}>{err}</div>}
      </div>
    </Modal>
  );
}


const btnLight = { padding: "8px 12px", borderRadius: 8, background: "#fff", border: "1px solid #d1d5db", cursor: "pointer" };
const btnDanger = { ...btnLight, background: "#fff1f2", borderColor: "#fecaca", color: "#991b1b" };
const warn = { background: "#fffbeb", border: "1px solid #fde68a", color: "#92400e", padding: 8, borderRadius: 8, fontSize: 13 };
const note = { background: "#eff6ff", border: "1px solid #bfdbfe", color: "#1e3a8a", padding: 8, borderRadius: 8, fontSize: 12 };
const box = { border: "1px solid #e5e7eb", borderRadius: 8, padding: 8, background: "#fff" };
const check = { display: "flex", alignItems: "center", gap: 8, fontSize: 14 };
const errBox = { background: "#FEF2F2", color: "#991B1B", border: "1px solid #FCA5A5", padding: 8, borderRadius: 8, fontSize: 12 };
