import React from "react";

export default function Modal({ open, title, onClose, children, footer }) {
  if (!open) return null;
  return (
    <div style={backdrop} onClick={onClose}>
      <div style={panel} onClick={(e) => e.stopPropagation()}>
        <div style={header}>
          <div style={{ fontWeight: 600 }}>{title}</div>
          <button onClick={onClose} style={xbtn} aria-label="Закрыть">×</button>
        </div>
        <div style={{ padding: "12px 16px" }}>{children}</div>
        {footer && <div style={footerBox}>{footer}</div>}
      </div>
    </div>
  );
}

const backdrop = {
  position: "fixed", inset: 0, background: "rgba(0,0,0,0.35)",
  display: "flex", alignItems: "center", justifyContent: "center", zIndex: 9999,
};

const panel = {
  width: "min(700px, 92vw)",     
  maxHeight: "min(85vh, 1000px)",
  overflow: "auto",
  background: "#fff",
  borderRadius: 12,
  boxShadow: "0 10px 30px rgba(0,0,0,0.2)"
};

const header = {
  display: "flex", alignItems: "center", justifyContent: "space-between",
  padding: "10px 16px", borderBottom: "1px solid #eee"
};
const xbtn = {
  border: "none", background: "transparent", fontSize: 22, lineHeight: 1,
  cursor: "pointer", padding: 4
};
const footerBox = {
  padding: "10px 16px", borderTop: "1px solid #eee", display: "flex",
  justifyContent: "flex-end", gap: 8
};
