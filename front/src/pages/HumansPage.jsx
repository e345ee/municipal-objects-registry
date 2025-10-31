import React from "react";
import { HumansApi } from "../api/humans";
import { createStomp } from "../realtime/bus";
import HumanCreateDialog from "./HumanCreationDialog";
import HumanEditDialog from "./HumanEditDialog";
import Modal from "../components/Modal";

export default function HumansPage() {
  const [rows, setRows] = React.useState([]);
  const [page, setPage] = React.useState(0);
  const [size, setSize] = React.useState(25);
  const [totalPages, setTotalPages] = React.useState(0);
  const [totalElements, setTotalElements] = React.useState(0);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState("");

  const [createOpen, setCreateOpen] = React.useState(false);


  const [linkInfo, setLinkInfo] = React.useState(null);


  const [editOpen, setEditOpen] = React.useState(false);
  const [editing, setEditing] = React.useState(null); // {id, height}


  const [filters, setFilters] = React.useState({ id: "", height: "" });
  const [sort, setSort] = React.useState(["id,asc"]); // дефолт как раньше (id,asc)

  const fetchPage = React.useCallback(
    async (p = page, s = size, f = filters, so = sort) => {
      setLoading(true);
      setError("");
      try {
        const resp = await HumansApi.list({ page: p, size: s, sort: so, filters: f });
        if (resp.totalPages > 0 && p >= resp.totalPages) {
          const last = resp.totalPages - 1;
          const retry = await HumansApi.list({ page: last, size: s, sort: so, filters: f });
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
        setError("Не удалось загрузить людей");
      } finally {
        setLoading(false);
      }
    },
    [page, size, filters, sort]
  );

  React.useEffect(() => { fetchPage(page, size, filters, sort); }, [fetchPage, page, size, filters, sort]);

  React.useEffect(() => {
    const client = createStomp(() => { fetchPage(page, size, filters, sort); }, ["/topic/humans"]);
    client.activate();
    return () => client.deactivate();
  }, [fetchPage, page, size, filters, sort]);

  const handleCreated = () => fetchPage(page, size, filters, sort);
  const handleUpdated = () => fetchPage(page, size, filters, sort);

  const handleEditClick = (row) => {
    setEditing(row);
    setEditOpen(true);
  };

  const handleDelete = async (id) => {
    const person = rows.find(r => r.id === id);
    const nameInTitle = `ID ${id}${person?.height ? ` (height: ${person.height})` : ""}`;
    if (!window.confirm(`Удалить человека ${nameInTitle}?`)) return;

    try {
      await HumansApi.remove(id);
      fetchPage(page, size, filters, sort);
    } catch (err) {
      const resp = err?.response?.data;
      const usage =
        resp?.usage ?? resp?.count ?? resp?.affected ?? resp?.total ?? null;
      const cityIds =
        resp?.cityIds ?? resp?.ids ?? resp?.relatedIds ?? resp?.cities ?? null;
      const message =
        resp?.message ??
        "Невозможно удалить: человек связан с объектами. Перепривяжите города к другому человеку.";
      setLinkInfo({ message, usage, cityIds: Array.isArray(cityIds) ? cityIds : null, humanId: id });
    }
  };


  const fmt = (v) => (v === null || v === undefined || v === "" ? "-" : String(v));
  const isFirst = page <= 0;
  const isLast = totalPages === 0 || page + 1 >= totalPages;
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);


  const applyFilters = () => {
    setPage(0);
    fetchPage(0, size, filters, sort);
  };
  const resetFilters = () => {
    const cleared = { id: "", height: "" };
    setFilters(cleared);
    setPage(0);
    fetchPage(0, size, cleared, sort);
  };


  const toggleSort = (field) => {
    setSort((prev) => {
      const cur = Array.isArray(prev) && prev.length ? prev[0] : "";
      const [f = "", dir = "asc"] = cur.split(",");
      let next;
      if (f === field) {
        const nextDir = dir === "asc" ? "desc" : "asc";
        next = [`${field},${nextDir}`];
      } else {
        next = [`${field},asc`];
      }
      setPage(0);
      fetchPage(0, size, filters, next);
      return next;
    });
  };
  const renderSortIcon = (field) => {
    const cur = Array.isArray(sort) && sort.length ? sort[0] : "";
    const [f = "", dir = "asc"] = cur.split(",");
    if (f !== field) return <span style={{ opacity: 0.35 }}>↕</span>;
    return <span>{dir === "asc" ? "↑" : "↓"}</span>;
  };

  return (
    <div style={{ padding: 24, maxWidth: 900, margin: "0 auto" }}>
      <header style={{ display: "flex", gap: 12, marginBottom: 12, alignItems: "center", flexWrap: "wrap" }}>
        <h2 style={{ margin: 0 }}>Люди</h2>
        <button onClick={() => setCreateOpen(true)} style={createBtn}>Создать человека</button>


        <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
          <input
            type="number"
            placeholder="ID ="
            value={filters.id}
            onChange={(e) => setFilters((f) => ({ ...f, id: e.target.value }))}
            style={input}
            title="Точный фильтр по ID"
          />
          <input
            type="number"
            placeholder="Height ="
            value={filters.height}
            onChange={(e) => setFilters((f) => ({ ...f, height: e.target.value }))}
            style={input}
            title="Точный фильтр по росту (height)"
          />
          <button onClick={applyFilters} style={btn}>Применить</button>
          <button onClick={resetFilters} style={btnLight}>Сброс</button>
        </div>

        <div style={{ marginLeft: "auto", display: "flex", gap: 8, alignItems: "center" }}>
          <span style={{ opacity: 0.7, fontSize: 13 }}>
            Показаны {from}–{to} из {totalElements}
          </span>
          <select
            value={size}
            onChange={(e) => { setPage(0); setSize(Number(e.target.value)); }}
            title="Размер страницы"
          >
            <option value={10}>10</option>
            <option value={25}>25</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
          <button onClick={() => fetchPage(page, size, filters, sort)} disabled={loading}>Обновить</button>
        </div>
      </header>

      {loading && <div>Загрузка…</div>}
      {error && <div style={{ color: "crimson", marginBottom: 8 }}>{error}</div>}

      <div style={{ overflowX: "auto", border: "1px solid #e5e7eb", borderRadius: 8 }}>
        <table style={table}>
          <thead>
            <tr>
              <th
                style={{ ...th, width: 120, cursor: "pointer", userSelect: "none" }}
                onClick={() => toggleSort("id")}
                title="Сортировать по ID"
              >
                ID {renderSortIcon("id")}
              </th>
              <th
                style={{ ...th, width: 160, textAlign: "right", cursor: "pointer", userSelect: "none" }}
                onClick={() => toggleSort("height")}
                title="Сортировать по Height"
              >
                Height {renderSortIcon("height")}
              </th>
              <th style={{ ...th, width: 220, textAlign: "right" }}>Действия</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={r.id} style={i % 2 ? trZebra : undefined}>
                <td style={td}>{fmt(r.id)}</td>
                <td style={{ ...td, textAlign: "right" }}>{fmt(r.height)}</td>
                <td style={{ ...td, textAlign: "right" }}>
                  <div style={actionsWrap}>
                    <button style={btn} onClick={() => handleEditClick(r)}>Изменить</button>
                    <button style={btnDanger} onClick={() => handleDelete(r.id)}>Удалить</button>
                  </div>
                </td>
              </tr>
            ))}
            {!loading && rows.length === 0 && (
              <tr>
                <td style={tdEmpty} colSpan={3}>Пусто</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div style={pager}>
        <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={isFirst || loading}>
          Назад
        </button>
        <div>Стр. {totalPages === 0 ? 0 : page + 1} / {totalPages}</div>
        <button onClick={() => setPage((p) => p + 1)} disabled={isLast || loading}>
          Вперёд
        </button>
      </div>

      <HumanCreateDialog
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={handleCreated}
      />

      <HumanEditDialog
        open={editOpen}
        onClose={() => setEditOpen(false)}
        human={editing}
        onUpdated={handleUpdated}
      />


      <Modal
        open={!!linkInfo}
        onClose={() => setLinkInfo(null)}
        title="Нельзя удалить человека"
        footer={<button onClick={() => setLinkInfo(null)}>Понятно</button>}
      >
        {linkInfo && (
          <div style={{ display: "grid", gap: 8 }}>
            <div style={{ whiteSpace: "pre-wrap" }}>{linkInfo.message}</div>
            {Number.isFinite(linkInfo.usage) && (
              <div style={{ opacity: 0.8, fontSize: 13 }}>
                Кол-во связанных городов: {linkInfo.usage}
              </div>
            )}
            {Array.isArray(linkInfo.cityIds) && linkInfo.cityIds.length > 0 && (
              <div>
                <div style={{ opacity: 0.8, fontSize: 13, marginBottom: 4 }}>
                  Идентификаторы городов:
                </div>
                <div style={{
                  border: "1px solid #eee", borderRadius: 6, padding: 8,
                  fontFamily: "monospace", fontSize: 13
                }}>
                  {linkInfo.cityIds.join(", ")}
                </div>
              </div>
            )}
            <div style={{ marginTop: 4, fontSize: 13 }}>
              Перепривяжите эти города к другому человеку и повторите удаление.
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}


const input = {
  padding: "6px 10px",
  border: "1px solid #d1d5db",
  borderRadius: 8,
  minWidth: 120,
  outline: "none",
};
const table = {
  width: "100%",
  borderCollapse: "separate",
  borderSpacing: 0,
  tableLayout: "fixed",
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
};
const td = {
  borderBottom: "1px solid #f0f2f5",
  padding: "6px 10px",
  verticalAlign: "middle",
  whiteSpace: "nowrap",
};
const tdEmpty = { ...td, textAlign: "center", color: "#6b7280" };
const trZebra = { background: "#fafafa" };
const actionsWrap = { display: "inline-flex", gap: 6, justifyContent: "flex-end" };
const btn = {
  padding: "4px 10px",
  fontSize: 13,
  cursor: "pointer",
  borderRadius: 6,
  border: "1px solid #d1d5db",
  background: "#f9fafb",
};
const btnLight = { ...btn, background: "#fff" };
const btnDanger = { ...btn, borderColor: "#fecaca", background: "#fff1f2" };
const pager = { display: "flex", gap: 8, marginTop: 12, alignItems: "center", justifyContent: "flex-end" };
const createBtn = {
  padding: "6px 12px",
  borderRadius: 8,
  border: "1px solid #d1d5db",
  background: "#eef2ff",
  cursor: "pointer",
};
