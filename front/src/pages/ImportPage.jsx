import React from "react";
import CitiesApi from "../api/cities";
import ImportsApi from "../api/imports";
import AdminApi from "../api/admin";
import { createStomp } from "../realtime/bus";

const CLIMATES = ["RAIN_FOREST", "HUMIDSUBTROPICAL", "TUNDRA"];
const API_BASE = process.env.REACT_APP_API_BASE || "";

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
  const [infra, setInfra] = React.useState(null);
  const [infraLoading, setInfraLoading] = React.useState(false);
  const [infraError, setInfraError] = React.useState("");
  const [adminActionMsg, setAdminActionMsg] = React.useState("");
  const [purging, setPurging] = React.useState(false);

  const [minioFiles, setMinioFiles] = React.useState([]);
  const [filesLoading, setFilesLoading] = React.useState(false);
  const [filesError, setFilesError] = React.useState("");
  const [filesPage, setFilesPage] = React.useState(0);
  const [filesSize, setFilesSize] = React.useState(10);
  const [filesTotalPages, setFilesTotalPages] = React.useState(0);
  const [filesTotalElements, setFilesTotalElements] = React.useState(0);
  const [reimportingKey, setReimportingKey] = React.useState("");

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
  const refreshInfraRef = React.useRef(null);
  const refreshMinioFilesRef = React.useRef(null);
  React.useEffect(() => {
    refreshRef.current = refreshHistory;
  }, [refreshHistory]);

  React.useEffect(() => {
    const client = createStomp(
      () => {
        refreshRef.current?.();
        refreshMinioFilesRef.current?.();
        refreshInfraRef.current?.();
      },
      ["/topic/imports", "/topic/minio-files"]
    );

    client.activate();
    return () => client.deactivate();
  }, []);


  const refreshInfra = React.useCallback(async () => {
    setInfraLoading(true);
    setInfraError("");
    try {
      const resp = await AdminApi.getInfraOverview();
      setInfra(resp);
    } catch (e) {
      setInfraError(extractApiMessage(e) || "Ошибка получения статуса инфраструктуры");
    } finally {
      setInfraLoading(false);
    }
  }, []);

  const refreshMinioFiles = React.useCallback(async () => {
    setFilesLoading(true);
    setFilesError("");
    try {
      const resp = await AdminApi.listMinioFiles({ page: filesPage, size: filesSize });
      setMinioFiles(resp?.content ?? []);
      setFilesTotalPages(resp?.totalPages ?? 0);
      setFilesTotalElements(resp?.totalElements ?? 0);

      if ((resp?.totalPages ?? 0) > 0 && filesPage >= resp.totalPages) {
        setFilesPage(Math.max(0, resp.totalPages - 1));
      }
    } catch (e) {
      setFilesError(extractApiMessage(e) || "Ошибка загрузки файлов MinIO");
    } finally {
      setFilesLoading(false);
    }
  }, [filesPage, filesSize]);

  React.useEffect(() => {
    refreshInfra();
  }, [refreshInfra]);

  React.useEffect(() => {
    refreshInfraRef.current = refreshInfra;
  }, [refreshInfra]);

  React.useEffect(() => {
    refreshMinioFiles();
  }, [refreshMinioFiles]);

  React.useEffect(() => {
    refreshMinioFilesRef.current = refreshMinioFiles;
  }, [refreshMinioFiles]);

  const runAdminAction = React.useCallback(async (action, successMessage) => {
    setAdminActionMsg("");
    try {
      await action();
      if (successMessage) setAdminActionMsg(successMessage);
      await Promise.allSettled([refreshInfra(), refreshMinioFiles(), refreshHistory()]);
    } catch (e) {
      setAdminActionMsg(extractApiMessage(e) || "Не удалось выполнить действие");
    }
  }, [refreshInfra, refreshMinioFiles, refreshHistory]);

  const toggleL2Logging = async (enabled) => {
    await runAdminAction(() => AdminApi.setL2StatsLogging(enabled), enabled ? "Логирование L2 включено" : "Логирование L2 выключено");
  };

  const toggleInfraFailure = async (target, enabled) => {
    const targetName = target === "minio" ? "MinIO" : "PostgreSQL";
    await runAdminAction(
      () => AdminApi.setSimulatedFailure(target, enabled),
      enabled ? `Симуляция отказа ${targetName} включена` : `Симуляция отказа ${targetName} выключена`
    );
  };

  const purgeAllObjects = async () => {
    const ok = window.confirm("Удалить все объекты (города, координаты, люди)?");
    if (!ok) return;

    setPurging(true);
    try {
      await runAdminAction(() => AdminApi.purgeAllObjects(), "Все объекты удалены");
    } finally {
      setPurging(false);
    }
  };

  const reimportFromMinio = async (f) => {
    if (!f?.objectKey) return;
    setReimportingKey(f.objectKey);
    setUploadResult(null);
    setUploadError(null);
    setAdminActionMsg("");
    try {
      const resp = await AdminApi.reimportMinioFile(f.objectKey);
      setUploadResult(resp);
      setAdminActionMsg(`Повторный импорт выполнен: ${f.fileName || f.objectKey}`);
      await Promise.allSettled([refreshHistory(), refreshMinioFiles(), refreshInfra()]);
    } catch (e) {
      const resp = e?.response?.data;
      if (resp?.error === "validation_failed" && resp?.details?.items) {
        setUploadError({
          type: "validation_failed",
          message: resp?.message || "Повторный импорт отклонён из-за ошибок валидации",
          items: resp.details.items,
        });
      } else {
        setUploadError({
          type: "error",
          message: extractApiMessage(e) || "Ошибка повторного импорта",
        });
      }
    } finally {
      setReimportingKey("");
    }
  };

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

      <div style={{ ...card, marginBottom: 16 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
          <h3 style={{ margin: 0 }}>Мониторинг и админ-действия</h3>
          <button onClick={refreshInfra} disabled={infraLoading}>
            {infraLoading ? "Проверка..." : "Проверить инфраструктуру"}
          </button>
          <button onClick={purgeAllObjects} disabled={purging}>
            {purging ? "Удаление..." : "Удалить все объекты"}
          </button>
          {adminActionMsg && <span style={{ fontSize: 13, opacity: 0.9 }}>{adminActionMsg}</span>}
        </div>

        {infraError && (
          <div style={{ ...banner, ...bannerError }}>
            <div style={{ fontWeight: 600 }}>Ошибка статуса инфраструктуры</div>
            <div>{infraError}</div>
          </div>
        )}

        {infra && (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 12, marginTop: 12 }}>
            <div style={subCard}>
              <div style={subTitle}>Пул соединений PostgreSQL (DBCP2)</div>
              <div style={kvList}>
                <div>Занято: <strong>{infra?.dbPool?.active ?? "—"}</strong></div>
                <div>Idle: <strong>{infra?.dbPool?.idle ?? "—"}</strong></div>
                <div>Свободно (до maxTotal): <strong>{infra?.dbPool?.freeCapacity ?? "—"}</strong></div>
                <div>maxTotal: <strong>{infra?.dbPool?.maxTotal ?? "—"}</strong></div>
              </div>
            </div>

            <div style={subCard}>
              <div style={subTitle}>L2 Cache / логирование</div>
              <div style={kvList}>
                <div>Логирование L2: <strong>{boolLabel(infra?.cache?.l2StatsLoggingEnabled)}</strong></div>
                <div>Hibernate statistics: <strong>{boolLabel(infra?.cache?.statisticsEnabled)}</strong></div>
                <div>Hits / Misses / Puts: <strong>{infra?.cache?.hits ?? 0}</strong> / <strong>{infra?.cache?.misses ?? 0}</strong> / <strong>{infra?.cache?.puts ?? 0}</strong></div>
              </div>
              <div style={{ display: "flex", gap: 8, marginTop: 8, flexWrap: "wrap" }}>
                <button onClick={() => toggleL2Logging(true)} disabled={infraLoading || infra?.cache?.l2StatsLoggingEnabled === true}>Включить логирование</button>
                <button onClick={() => toggleL2Logging(false)} disabled={infraLoading || infra?.cache?.l2StatsLoggingEnabled === false}>Выключить логирование</button>
              </div>
            </div>

            <div style={subCard}>
              <div style={subTitle}>Доступность зависимостей</div>
              <div style={kvList}>
                <div>PostgreSQL: <StatusBadge ok={!!infra?.postgres?.effectiveAvailable} label={infra?.postgres?.effectiveAvailable ? "доступен" : "недоступен"} /></div>
                <div>MinIO: <StatusBadge ok={!!infra?.minio?.effectiveAvailable} label={infra?.minio?.effectiveAvailable ? "доступен" : "недоступен"} /></div>
                <div style={{ fontSize: 12, opacity: 0.85 }}>
                  Физически: PG={boolShort(infra?.postgres?.actualAvailable)}, MinIO={boolShort(infra?.minio?.actualAvailable)}; симуляция: PG={boolShort(infra?.postgres?.simulatedFailure)}, MinIO={boolShort(infra?.minio?.simulatedFailure)}
                </div>
              </div>
              {(infra?.postgres?.message || infra?.minio?.message) && (
                <div style={{ marginTop: 6, fontSize: 12, opacity: 0.85 }}>
                  {infra?.postgres?.message ? `PG: ${infra.postgres.message}` : ""}
                  {infra?.postgres?.message && infra?.minio?.message ? " • " : ""}
                  {infra?.minio?.message ? `MinIO: ${infra.minio.message}` : ""}
                </div>
              )}
              <div style={{ display: "flex", gap: 8, marginTop: 8, flexWrap: "wrap" }}>
                <button onClick={() => toggleInfraFailure("postgres", true)} disabled={infra?.postgres?.simulatedFailure}>Симулировать отказ PostgreSQL</button>
                <button onClick={() => toggleInfraFailure("postgres", false)} disabled={!infra?.postgres?.simulatedFailure}>Восстановить PostgreSQL</button>
                <button onClick={() => toggleInfraFailure("minio", true)} disabled={infra?.minio?.simulatedFailure}>Симулировать отказ MinIO</button>
                <button onClick={() => toggleInfraFailure("minio", false)} disabled={!infra?.minio?.simulatedFailure}>Восстановить MinIO</button>
              </div>
            </div>
          </div>
        )}
      </div>

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
          <button onClick={onUpload} disabled={!canUpload} style={canUpload ? btnPrimary : btnDisabled}>
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
            <div style={{ marginTop: 8, maxHeight: 240, overflow: "auto" }}>{renderErrors(validationErrors)}</div>
          </div>
        )}

        {uploadResult && (
          <div style={{ ...banner, ...bannerOk }}>
            <div style={{ fontWeight: 600 }}>Импорт завершён успешно</div>
            <div>Создано объектов: {uploadResult.created}</div>
            {Array.isArray(uploadResult.cityIds) && uploadResult.cityIds.length > 0 && (
              <div style={{ marginTop: 6, fontSize: 13 }}>IDs: {uploadResult.cityIds.join(", ")}</div>
            )}
          </div>
        )}

        {uploadError && uploadError.type === "validation_failed" && (
          <div style={{ ...banner, ...bannerError }}>
            <div style={{ fontWeight: 600 }}>Импорт отклонён сервером (валидация)</div>
            <div style={{ marginTop: 6 }}>{uploadError.message}</div>
            <div style={{ marginTop: 8, maxHeight: 240, overflow: "auto" }}>{renderServerItemErrors(uploadError.items)}</div>
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
          <button onClick={refreshHistory} disabled={histLoading}>
            Обновить
          </button>

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
                <option key={n} value={n}>
                  {n}
                </option>
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
                <TH>Файл</TH>
              </tr>
            </thead>
            <tbody>
              {history.map((op) => (
                <tr key={op.id}>
                  <TD noEllipsis>{op.id}</TD>
                  <TD>
                    <span style={statusPill(op.status)}>{op.status}</span>
                  </TD>
                  <TD right noEllipsis>
                    {op.addedCount ?? "—"}
                  </TD>
                  <TD noEllipsis>{fmtDateTime(op.startedAt)}</TD>
                  <TD noEllipsis>{op.finishedAt ? fmtDateTime(op.finishedAt) : "—"}</TD>
                  <TD noEllipsis>
                    {op.downloadUrl ? (
                      <a href={`${API_BASE}${op.downloadUrl}`} target="_blank" rel="noreferrer">
                        {op.sourceFilename || "Скачать JSON"}
                      </a>
                    ) : (
                      op.sourceFilename || "—"
                    )}
                  </TD>
                </tr>
              ))}
              {!histLoading && history.length === 0 && (
                <tr>
                  <td style={tdEmpty} colSpan={6}>
                    История пуста
                  </td>
                </tr>
              )}
              {histLoading && (
                <tr>
                  <td style={tdEmpty} colSpan={6}>
                    Загрузка…
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div style={pager}>
          <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={isFirst || histLoading}>
            Назад
          </button>
          <div>
            Стр. {totalPages === 0 ? 0 : page + 1} / {totalPages}
          </div>
          <button onClick={() => setPage((p) => p + 1)} disabled={isLast || histLoading}>
            Вперёд
          </button>
        </div>

        <div style={{ marginTop: 10, fontSize: 13, opacity: 0.85, lineHeight: 1.35 }}></div>
      </div>


      <h2 style={{ margin: "20px 0 12px" }}>Файлы в MinIO</h2>

      <div style={card}>
        <div style={{ display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
          <button onClick={refreshMinioFiles} disabled={filesLoading}>
            Обновить список файлов
          </button>
          <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
            Размер страницы:
            <select
              value={filesSize}
              onChange={(e) => {
                const next = Number(e.target.value);
                setFilesSize(next);
                if (filesPage !== 0) setFilesPage(0);
              }}
            >
              {[5, 10, 25, 50].map((n) => (
                <option key={`f-${n}`} value={n}>
                  {n}
                </option>
              ))}
            </select>
          </label>

          <div style={{ marginLeft: "auto", fontSize: 13, opacity: 0.9 }}>
            {filesTotalElements === 0 ? "Файлов нет" : `Показаны ${filesPage * filesSize + 1}–${Math.min((filesPage + 1) * filesSize, filesTotalElements)} из ${filesTotalElements}`}
          </div>
        </div>

        {filesError && (
          <div style={{ ...banner, ...bannerError }}>
            <div style={{ fontWeight: 600 }}>Ошибка списка MinIO</div>
            <div>{filesError}</div>
          </div>
        )}

        <div style={{ overflow: "auto", marginTop: 12 }}>
          <table style={table}>
            <thead>
              <tr>
                <TH>Файл</TH>
                <TH>Ключ MinIO</TH>
                <TH right>Размер</TH>
                <TH>Изменен</TH>
                <TH>Скачать</TH>
                <TH>Импорт</TH>
              </tr>
            </thead>
            <tbody>
              {minioFiles.map((f) => (
                <tr key={f.objectKey}>
                  <TD noEllipsis>{f.fileName || "—"}</TD>
                  <TD noEllipsis><code>{f.objectKey}</code></TD>
                  <TD right noEllipsis>{formatBytes(f.sizeBytes)}</TD>
                  <TD noEllipsis>{fmtDateTime(f.lastModified)}</TD>
                  <TD noEllipsis>
                    {f.downloadUrl ? (
                      <a href={`${API_BASE}${f.downloadUrl}`} target="_blank" rel="noreferrer">Скачать</a>
                    ) : "—"}
                  </TD>
                  <TD noEllipsis>
                    <button
                      onClick={() => reimportFromMinio(f)}
                      disabled={!!reimportingKey || filesLoading}
                      style={(reimportingKey && reimportingKey === f.objectKey) ? btnDisabled : undefined}
                    >
                      {reimportingKey === f.objectKey ? "Импорт..." : "Импортировать"}
                    </button>
                  </TD>
                </tr>
              ))}
              {!filesLoading && minioFiles.length === 0 && (
                <tr>
                  <td style={tdEmpty} colSpan={6}>Файлы не найдены</td>
                </tr>
              )}
              {filesLoading && (
                <tr>
                  <td style={tdEmpty} colSpan={6}>Загрузка…</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div style={pager}>
          <button onClick={() => setFilesPage((p) => Math.max(0, p - 1))} disabled={filesLoading || filesPage <= 0}>
            Назад
          </button>
          <div>
            Стр. {filesTotalPages === 0 ? 0 : filesPage + 1} / {filesTotalPages}
          </div>
          <button onClick={() => setFilesPage((p) => p + 1)} disabled={filesLoading || filesTotalPages === 0 || filesPage + 1 >= filesTotalPages}>
            Вперёд
          </button>
        </div>
      </div>
      {parsed && Array.isArray(parsed) && parsed.length > 0 && (
        <div style={{ ...card, marginTop: 20 }}>
          <div style={{ display: "flex", alignItems: "baseline", gap: 12, flexWrap: "wrap" }}>
            <h3 style={{ margin: 0 }}>Предпросмотр (первые 3 записи)</h3>
            <div style={{ fontSize: 13, opacity: 0.85 }}>Всего записей: {parsed.length}</div>
          </div>
          <pre style={pre}>{JSON.stringify(parsed.slice(0, 3), null, 2)}</pre>
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
  return renderErrors(
    items.map((x) => ({
      index: x.index,
      field: x.field,
      message: x.message,
    }))
  );
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
  const y = Number(m[1]),
    mo = Number(m[2]),
    d = Number(m[3]);
  if (mo < 1 || mo > 12) return false;
  if (d < 1 || d > 31) return false;
  const dt = new Date(Date.UTC(y, mo - 1, d));
  return dt.getUTCFullYear() === y && dt.getUTCMonth() + 1 === mo && dt.getUTCDate() === d;
}

function fmtDateTime(v) {
  if (!v) return "—";
  const s = String(v);
  return s.replace("T", " ");
}

function boolLabel(v) {
  return v ? "включено" : "выключено";
}

function boolShort(v) {
  return v ? "да" : "нет";
}

function formatBytes(bytes) {
  const n = Number(bytes);
  if (!Number.isFinite(n) || n < 0) return "—";
  if (n < 1024) return `${n} B`;
  const kb = n / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;
  const mb = kb / 1024;
  if (mb < 1024) return `${mb.toFixed(1)} MB`;
  const gb = mb / 1024;
  return `${gb.toFixed(2)} GB`;
}

function StatusBadge({ ok, label }) {
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        padding: "2px 10px",
        borderRadius: 999,
        border: `1px solid ${ok ? "#b7e3c0" : "#f2b8b5"}`,
        background: ok ? "#eefaf1" : "#fff0ef",
        fontSize: 12,
      }}
    >
      <span style={{ width: 8, height: 8, borderRadius: 999, background: ok ? "#22c55e" : "#ef4444", display: "inline-block" }} />
      {label}
    </span>
  );
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
  if (status === "IN_PROGRESS") return { ...base, borderColor: "#c9d7ff", background: "#f0f5ff" };
  if (status === "FILE_PREPARED") return { ...base, borderColor: "#c9d7ff", background: "#f0f5ff" };
  if (status === "FILE_COMMITTED") return { ...base, borderColor: "#bcd8ff", background: "#eef5ff" };
  if (status === "DB_COMMITTED") return { ...base, borderColor: "#cde8d0", background: "#f2fbf4" };
  if (status === "COMPENSATED") return { ...base, borderColor: "#f1d59b", background: "#fff7e6" };
  return base;
}

const subCard = {
  border: "1px solid #e5e7eb",
  borderRadius: 10,
  padding: 10,
  background: "#fafafa",
};

const subTitle = {
  fontSize: 13,
  fontWeight: 600,
  marginBottom: 8,
};

const kvList = {
  display: "flex",
  flexDirection: "column",
  gap: 6,
  fontSize: 13,
  lineHeight: 1.3,
};

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
