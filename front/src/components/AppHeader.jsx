import { Link, useLocation } from "react-router-dom";

export default function AppHeader() {
  const { pathname } = useLocation();

  const linkStyle = (path) => ({
    padding: "8px 14px",
    textDecoration: "none",
    color: pathname === path ? "#fff" : "#0077cc",
    background: pathname === path ? "#0077cc" : "transparent",
    borderRadius: 6,
    fontWeight: 500
  });

  return (
    <header
      style={{
        display: "flex",
        gap: 12,
        padding: "12px 20px",
        borderBottom: "1px solid #ddd",
        marginBottom: 16,
        alignItems: "center"
      }}
    >
      <Link to="/cities" style={linkStyle("/cities")}>Города</Link>
      <Link to="/coordinates" style={linkStyle("/coordinates")}>Координаты</Link>
      <Link to="/humans" style={linkStyle("/humans")}>Люди</Link>
      <Link to="/features" style={linkStyle("/features")}>Доп. функции</Link>
      <Link to="/imports" style={linkStyle("/imports")}>Импорт</Link>
    </header>
  );
}