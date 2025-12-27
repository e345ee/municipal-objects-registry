import React from "react";
import Modal from "../components/Modal";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { CitiesApi } from "../api/cities";
import { CoordinatesApi } from "../api/coordinates";
import { HumansApi } from "../api/humans";

const CLIMATES = ["RAIN_FOREST","HUMIDSUBTROPICAL","TUNDRA"];
const GOVERNMENTS = ["DEMARCHY","KLEPTOCRACY","CORPORATOCRACY","PLUTOCRACY","THALASSOCRACY"];

const INT_MAX  =  2147483647;
const INT_MIN  = -2147483648;
const LONG_MAX_STR = "9223372036854775807";
const TEL_MAX = 100000;
const X_MAX = 460;
const FLOAT_MAX = 3.4028235e38; 



function prospectiveValue(input, insertText) {
  const s = input.selectionStart ?? input.value.length;
  const e = input.selectionEnd ?? input.value.length;
  return input.value.slice(0, s) + insertText + input.value.slice(e);
}

function guardInteger(e, { allowNegative = false, maxLen = 19 } = {}) {
  const data = e.data ?? "";
  if (e.inputType && e.inputType.startsWith("delete")) return;
  if (data === "") return;
  if (!/[\d-]/.test(data)) { e.preventDefault(); return; }
  const next = prospectiveValue(e.target, data);
  const re = allowNegative ? /^-?\d*$/ : /^\d*$/;
  if (!re.test(next)) { e.preventDefault(); return; }
  const digits = next.replace(/-/g, "");
  if (digits.length > maxLen) { e.preventDefault(); return; }
}

function onPasteInteger(e, { allowNegative = false, maxLen = 19 } = {}) {
  const txt = (e.clipboardData?.getData("text") ?? "").trim();
  const re = allowNegative ? /^-?\d+$/ : /^\d+$/;
  if (!re.test(txt)) { e.preventDefault(); return; }
  const next = prospectiveValue(e.target, txt);
  const digits = next.replace(/-/g, "");
  if (digits.length > maxLen) e.preventDefault();
}


function guardFloatInput(e) {
  const data = e.data ?? "";
  if (e.inputType && e.inputType.startsWith("delete")) return;
  if (data === "") return;


  if (!/[\d.-]/.test(data)) { e.preventDefault(); return; }

  const input = e.target;
  const next = prospectiveValue(input, data);
  const { selectionStart } = input;


  if (data === "-") {
    if (selectionStart !== 0 || next.indexOf("-") > 0) {
      e.preventDefault();
      return;
    }
  }


  if (data === ".") {
    const parts = next.split(".");
    if (parts.length > 2) {
      e.preventDefault();
      return;
    }
  }
}

function onPasteFloat(e) {
  const txt = (e.clipboardData?.getData("text") ?? "").trim();


  if (/inf(inity)?/i.test(txt) || /\bnan\b/i.test(txt)) {
    e.preventDefault();
    return;
  }


  let cleaned = txt.replace(/[^\d.-]/g, "");
  cleaned = cleaned.replace(/(?!^)-/g, "");     
  cleaned = cleaned.replace(/(\..*)\./g, "$1"); 

  if (cleaned !== txt) e.preventDefault();
  if (cleaned) {
    const input = e.target;
    const next = prospectiveValue(input, cleaned);
    input.value = next;
    const evt = new Event("input", { bubbles: true });
    input.dispatchEvent(evt);
  }
}


const isPositiveDecimalString = (s) => /^[1-9]\d*$/.test(s);
function decimalLE(a, b) {
  if (a.length !== b.length) return a.length < b.length;
  return a <= b;
}

const floatLike = /^[+-]?(?:\d+\.?\d*|\d*\.?\d+)(?:[eE][+-]?\d+)?$/;

const strictFloat = z.preprocess(
  (v) => {
    if (v === "" || v === null || v === undefined) return NaN;

    if (typeof v === "string") {
      const trimmed = v.trim();
      const lower = trimmed.toLowerCase();

      if (
        lower === "infinity" ||
        lower === "+infinity" ||
        lower === "-infinity" ||
        lower === "inf" ||
        lower === "+inf" ||
        lower === "-inf" ||
        lower === "nan"
      ) {
        return NaN;
      }

      if (!floatLike.test(trimmed)) {
        return NaN;
      }

      return Number(trimmed);
    }

    if (typeof v !== "number") return NaN;
    return v;
  },
  z
    .number({ invalid_type_error: "Введите число" })
    .refine(Number.isFinite, "Недопустимое число")
    .refine((n) => Math.abs(n) <= FLOAT_MAX, "Число не может выходить за пределы float")
);

const nullableFloat = strictFloat.nullable();

function isRealIsoDate(s) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return false;
  const [y, m, d] = s.split("-").map(Number);
  const date = new Date(Date.UTC(y, m - 1, d));
  return (
    date.getUTCFullYear() === y &&
    date.getUTCMonth() === m - 1 &&
    date.getUTCDate() === d
  );
}

const GovernmentNullable = z.preprocess(
  (v) => (v === "" || v === undefined ? null : v),
  z.enum(GOVERNMENTS).nullable()
);


const baseSchema = z.object({
  name: z.string().trim().min(1, "Название обязательно"),

  area: z.preprocess(
    v => v === "" ? undefined : Number(v),
    z.number({ invalid_type_error: "Введите целое число" })
      .int("Только целое")
      .positive("Площадь > 0")
      .max(INT_MAX, `Максимум ${INT_MAX}`)
  ),

  population: z.string()
    .regex(/^\d+$/, "Только цифры")
    .refine((s) => isPositiveDecimalString(s), "Должно быть > 0")
    .refine((s) => s.length <= 19, "Не более 19 цифр (Long)")
    .refine((s) => decimalLE(s, LONG_MAX_STR), "Слишком большое значение (Long)"),

  establishmentDate: z
    .string()
    .optional()
    .refine(
      (s) => !s || isRealIsoDate(s),
      { message: "Некорректная дата" }
    ),

  capital: z.boolean().optional().default(false),

  metersAboveSeaLevel: z.preprocess(
    v => v === "" ? null : Number(v),
    z.number({ invalid_type_error: "Введите целое число" })
      .int("Только целое")
      .min(INT_MIN, `Минимум ${INT_MIN}`)
      .max(INT_MAX, `Максимум ${INT_MAX}`)
      .nullable()
  ),

  telephoneCode: z.preprocess(
    v => v === "" ? undefined : Number(v),
    z.number({ invalid_type_error: "Введите целое число" })
      .int("Только целое")
      .positive("Код > 0")
      .max(TEL_MAX, `Макс ${TEL_MAX}`)
  ),

  climate: z.enum(CLIMATES, { required_error: "Выберите климат" }),
  government: GovernmentNullable,

  coordsMode: z.enum(["id","new"]),
  govMode: z.enum(["none","id","new"]),

  coordinatesId: z.preprocess(
    v => v === "" ? null : Number(v),
    z.number().int().positive().nullable()
  ),

  coordinatesX: z.preprocess(
    v => (v === "" || v === undefined) ? null : v,
    nullableFloat
  ).optional(),

  coordinatesY: z.preprocess(
    v => (v === "" || v === undefined) ? null : v,
    nullableFloat
  ).optional(),

  governorId: z.preprocess(
    v => v === "" ? null : Number(v),
    z.number().int().positive().nullable()
  ),

  governorHeight: z.preprocess(
    v => (v === "" || v === undefined) ? null : v,
    nullableFloat
  ).optional(),
});

const schema = baseSchema.superRefine((val, ctx) => {
  if (val.coordsMode === "id") {
    if (val.coordinatesId == null) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["coordinatesId"], message: "Укажите ID координат" });
    }
  } else {
    if (val.coordinatesX === null) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["coordinatesX"], message: "Укажите X" });
    } else if (!Number.isFinite(val.coordinatesX) || val.coordinatesX > X_MAX) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["coordinatesX"], message: `X ≤ ${X_MAX}` });
    }
    if (val.coordinatesY === null) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["coordinatesY"], message: "Укажите Y" });
    } else if (!Number.isFinite(val.coordinatesY)) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["coordinatesY"], message: "Некорректный Y" });
    }
  }

  if (val.govMode === "id") {
    if (val.governorId == null) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["governorId"], message: "Укажите Governor ID" });
    }
  } else if (val.govMode === "new") {
    if (val.governorHeight === null) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["governorHeight"], message: "Укажите рост" });
    } else if (!Number.isFinite(val.governorHeight) || val.governorHeight <= 0) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["governorHeight"], message: "Рост > 0" });
    }
  }

  const numericFields = [
    "area","metersAboveSeaLevel","telephoneCode",
    "coordinatesId","coordinatesX","coordinatesY","governorId","governorHeight"
  ];
  for (const f of numericFields) {
    const v = val[f];
    if (v === null || v === undefined || v === "") continue;
    if (typeof v !== "number" || !Number.isFinite(v)) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: [f], message: "Недопустимое число" });
    }
  }
});


export default function CityCreateDialog({ open, onClose, onCreated }) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
    reset,
    clearErrors,
    setValue,
  } = useForm({
    resolver: zodResolver(schema),
    mode: "onChange",          
    reValidateMode: "onChange",
    shouldUnregister: false,
    defaultValues: {
      name: "",
      area: "",
      population: "",
      establishmentDate: "",
      capital: false,
      metersAboveSeaLevel: "",
      telephoneCode: "",
      climate: "HUMIDSUBTROPICAL",
      government: "",
      coordsMode: "id",
      coordinatesId: "",
      coordinatesX: "",
      coordinatesY: "",
      govMode: "none",
      governorId: "",
      governorHeight: "",
    },
  });

  React.useEffect(() => {
    if (!open) reset();
  }, [open, reset]);

  const coordsMode = watch("coordsMode");
  const govMode = watch("govMode");

  React.useEffect(() => {
    if (coordsMode === "id") {
      setValue("coordinatesX", "");
      setValue("coordinatesY", "");
      clearErrors(["coordinatesX","coordinatesY"]);
    } else {
      setValue("coordinatesId", "");
      clearErrors(["coordinatesId"]);
    }
  }, [coordsMode, setValue, clearErrors]);

  React.useEffect(() => {
    if (govMode === "none") {
      setValue("governorId", "");
      setValue("governorHeight", "");
      clearErrors(["governorId","governorHeight"]);
    } else if (govMode === "id") {
      setValue("governorHeight", "");
      clearErrors(["governorHeight"]);
    } else if (govMode === "new") {
      setValue("governorId", "");
      clearErrors(["governorId"]);
    }
  }, [govMode, setValue, clearErrors]);

  const [coordCheck, setCoordCheck] = React.useState({ state: "idle", text: "" });
  const [govCheck, setGovCheck] = React.useState({ state: "idle", text: "" });

  const onCheckCoordinatesId = async (idStr) => {
    const id = Number(String(idStr || "").trim());
    if (!id) { setCoordCheck({ state:"fail", text:"Введите корректный ID" }); return; }
    try {
      setCoordCheck({ state:"loading", text:"Проверяем..." });
      const c = await CoordinatesApi.get(id);
      setCoordCheck({ state:"ok", text:`Найдены: x=${c.x}, y=${c.y}` });
    } catch {
      setCoordCheck({ state:"fail", text:"Координаты не найдены" });
    }
  };

  const onCheckGovernorId = async (idStr) => {
    const id = Number(String(idStr || "").trim());
    if (!id) { setGovCheck({ state:"fail", text:"Введите корректный ID" }); return; }
    try {
      setGovCheck({ state:"loading", text:"Проверяем..." });
      const g = await HumansApi.get(id);
      setGovCheck({ state:"ok", text:`Найден: height=${g.height}` });
    } catch {
      setGovCheck({ state:"fail", text:"Губернатор не найден" });
    }
  };

  const onSubmit = async (values) => {
    try {
      const dto = {
        name: values.name.trim(),
        area: Number(values.area),
        population: Number(values.population),
        establishmentDate: values.establishmentDate ? new Date(values.establishmentDate) : null,
        capital: !!values.capital,
        metersAboveSeaLevel: values.metersAboveSeaLevel === "" ? null : Number(values.metersAboveSeaLevel),
        telephoneCode: Number(values.telephoneCode),
        climate: values.climate,
        government: values.government === "" ? null : values.government,
        coordinatesId: null,
        coordinates: null,
        governorId: null,
        governor: null,
      };

      if (values.coordsMode === "id") {
        dto.coordinatesId = Number(values.coordinatesId);
      } else {
        dto.coordinates = {
          x: Number(values.coordinatesX),
          y: Number(values.coordinatesY),
        };
      }

      if (values.govMode === "id") {
        dto.governorId = Number(values.governorId);
      } else if (values.govMode === "new") {
        dto.governor = { height: Number(values.governorHeight) };
      }

      const created = await CitiesApi.create(dto);
      onCreated?.(created);
      onClose?.();
    } catch (e) {
      alert(e?.response?.data?.message || e.message || "Ошибка создания города");
    }
  };

  const firstError = Object.values(errors)[0];
  const firstErrorMsg =
    firstError?.message || (firstError?._errors && firstError._errors[0]);

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Создание города"
      footer={(
        <>
          <button type="button" onClick={onClose} style={btnLight}>Отмена</button>
          <button type="submit" form="city-create-form" style={btnPrimary}>Создать</button>
        </>
      )}
    >
      <form
        id="city-create-form"
        onSubmit={handleSubmit(onSubmit)}
        style={{ display: "grid", gap: 10 }}
      >
        {firstErrorMsg && (
          <div
            style={{
              background:"#FEF2F2",
              color:"#991B1B",
              border:"1px solid #FCA5A5",
              padding:8,
              borderRadius:8,
              fontSize:12,
            }}
          >
            {firstErrorMsg}
          </div>
        )}

        <label style={label}>
          <span>Название *</span>
          <input
            {...register("name")}
            style={input(!!errors.name)}
            placeholder="New City"
          />
          {errors.name && <span style={err}>{errors.name.message}</span>}
        </label>

        <div style={twoCols}>
          <label style={label}>
            <span>Площадь *</span>
            <input
              {...register("area")}
              style={input(!!errors.area)}
              placeholder="500"
              inputMode="numeric"
              onBeforeInput={(e)=>guardInteger(e, { allowNegative:false, maxLen:10 })}
              onPaste={(e)=>onPasteInteger(e, { allowNegative:false, maxLen:10 })}
            />
            {errors.area && <span style={err}>{errors.area.message}</span>}
          </label>
          <label style={label}>
            <span>Население *</span>
            <input
              {...register("population")}
              style={input(!!errors.population)}
              placeholder="1200000"
              inputMode="numeric"
              onBeforeInput={(e)=>guardInteger(e, { allowNegative:false, maxLen:30 })}
              onPaste={(e)=>onPasteInteger(e, { allowNegative:false, maxLen:30 })}
            />
            {errors.population && <span style={err}>{errors.population.message}</span>}
          </label>
        </div>

        <div style={twoCols}>
          <label style={label}>
            <span>Дата основания</span>
            <input
              type="date"
              {...register("establishmentDate")}
              style={input(!!errors.establishmentDate)}
            />
            {errors.establishmentDate && (
              <span style={err}>{errors.establishmentDate.message}</span>
            )}
          </label>
          <label style={{ ...label, alignItems: "center", width: "100%" }}>
            <span>Столица</span>
            <input type="checkbox" {...register("capital")} />
          </label>
        </div>

        <div style={twoCols}>
          <label style={label}>
            <span>Высота над уровнем моря</span>
            <input
              {...register("metersAboveSeaLevel")}
              style={input(!!errors.metersAboveSeaLevel)}
              placeholder="200"
              inputMode="numeric"
              onBeforeInput={(e)=>guardInteger(e, { allowNegative:true, maxLen:10 })}
              onPaste={(e)=>onPasteInteger(e, { allowNegative:true, maxLen:10 })}
            />
            {errors.metersAboveSeaLevel && (
              <span style={err}>{errors.metersAboveSeaLevel.message}</span>
            )}
          </label>
          <label style={label}>
            <span>Телефонный код *</span>
            <input
              {...register("telephoneCode")}
              style={input(!!errors.telephoneCode)}
              placeholder="777"
              inputMode="numeric"
              onBeforeInput={(e)=>guardInteger(e, { allowNegative:false, maxLen:6 })}
              onPaste={(e)=>onPasteInteger(e, { allowNegative:false, maxLen:6 })}
            />
            {errors.telephoneCode && (
              <span style={err}>{errors.telephoneCode.message}</span>
            )}
          </label>
        </div>

        <div style={twoCols}>
          <label style={label}>
            <span>Климат *</span>
            <select {...register("climate")} style={input(!!errors.climate)}>
              {CLIMATES.map((c)=>(<option key={c} value={c}>{c}</option>))}
            </select>
            {errors.climate && <span style={err}>{errors.climate.message}</span>}
          </label>
          <label style={label}>
            <span>Форма правления</span>
            <select {...register("government")} style={input(!!errors.government)}>
              <option value="">—</option>
              {GOVERNMENTS.map((g)=>(<option key={g} value={g}>{g}</option>))}
            </select>
            {errors.government && <span style={err}>{errors.government.message}</span>}
          </label>
        </div>

        <fieldset style={box}>
          <legend style={legend}>Координаты (обязательно выбрать один способ)</legend>

          <div style={row}>
            <label style={radioLabel}>
              <input type="radio" value="id" {...register("coordsMode")} />
              <span>Привязать по ID</span>
            </label>
            <label style={radioLabel}>
              <input type="radio" value="new" {...register("coordsMode")} />
              <span>Создать новые</span>
            </label>
          </div>

          {coordsMode === "id" ? (
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr auto",
                gap: 8,
                alignItems: "end",
              }}
            >
              <label style={label}>
                <span>ID координат *</span>
                <input
                  {...register("coordinatesId")}
                  style={input(!!errors.coordinatesId)}
                  placeholder="например, 4"
                  inputMode="numeric"
                  onBeforeInput={(e)=>guardInteger(e, { allowNegative:false, maxLen:19 })}
                  onPaste={(e)=>onPasteInteger(e, { allowNegative:false, maxLen:19 })}
                />
                {errors.coordinatesId && (
                  <span style={err}>{errors.coordinatesId.message}</span>
                )}
              </label>
              <button
                type="button"
                style={btnLight}
                onClick={()=>onCheckCoordinatesId(watch("coordinatesId"))}
              >
                Проверить
              </button>
              {(coordCheck.state === "ok" ||
                coordCheck.state === "fail" ||
                coordCheck.state === "loading") && (
                <div
                  style={{
                    gridColumn: "1 / -1",
                    fontSize: 12,
                    color:
                      coordCheck.state==="ok"
                        ? "#065F46"
                        : coordCheck.state==="fail"
                          ? "#B91C1C"
                          : "#374151",
                  }}
                >
                  {coordCheck.text}
                </div>
              )}
            </div>
          ) : (
            <div style={twoCols}>
              <label style={label}>
                <span>X (≤ 460) *</span>
                <input
                  {...register("coordinatesX")}
                  style={input(!!errors.coordinatesX)}
                  placeholder="150"
                  inputMode="decimal"
                  onBeforeInput={guardFloatInput}
                  onPaste={onPasteFloat}
                />
                {errors.coordinatesX && (
                  <span style={err}>{errors.coordinatesX.message}</span>
                )}
              </label>
              <label style={label}>
                <span>Y *</span>
                <input
                  {...register("coordinatesY")}
                  style={input(!!errors.coordinatesY)}
                  placeholder="80"
                  inputMode="decimal"
                  onBeforeInput={guardFloatInput}
                  onPaste={onPasteFloat}
                />
                {errors.coordinatesY && (
                  <span style={err}>{errors.coordinatesY.message}</span>
                )}
              </label>
            </div>
          )}
        </fieldset>

        <fieldset style={box}>
          <legend style={legend}>Губернатор (необязательно)</legend>

          <div style={row}>
            <label style={radioLabel}>
              <input type="radio" value="none" {...register("govMode")} />
              <span>Не указывать</span>
            </label>
            <label style={radioLabel}>
              <input type="radio" value="id" {...register("govMode")} />
              <span>Привязать по ID</span>
            </label>
            <label style={radioLabel}>
              <input type="radio" value="new" {...register("govMode")} />
              <span>Создать нового</span>
            </label>
          </div>

          {govMode === "id" && (
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr auto",
                gap: 8,
                alignItems: "end",
              }}
            >
              <label style={label}>
                <span>Governor ID *</span>
                <input
                  {...register("governorId")}
                  style={input(!!errors.governorId)}
                  placeholder="например, 1"
                  inputMode="numeric"
                  onBeforeInput={(e)=>guardInteger(e, { allowNegative:false, maxLen:19 })}
                  onPaste={(e)=>onPasteInteger(e, { allowNegative:false, maxLen:19 })}
                />
                {errors.governorId && (
                  <span style={err}>{errors.governorId.message}</span>
                )}
              </label>
              <button
                type="button"
                style={btnLight}
                onClick={()=>onCheckGovernorId(watch("governorId"))}
              >
                Проверить
              </button>
              {(govCheck.state === "ok" ||
                govCheck.state === "fail" ||
                govCheck.state === "loading") && (
                <div
                  style={{
                    gridColumn: "1 / -1",
                    fontSize: 12,
                    color:
                      govCheck.state==="ok"
                        ? "#065F46"
                        : govCheck.state==="fail"
                          ? "#B91C1C"
                          : "#374151",
                  }}
                >
                  {govCheck.text}
                </div>
              )}
            </div>
          )}

          {govMode === "new" && (
            <div style={{ display: "grid", gridTemplateColumns: "1fr", gap: 8 }}>
              <label style={label}>
                <span>Рост (height) *</span>
                <input
                  {...register("governorHeight")}
                  style={input(!!errors.governorHeight)}
                  placeholder="15"
                  inputMode="decimal"
                  onBeforeInput={guardFloatInput}
                  onPaste={onPasteFloat}
                />
                {errors.governorHeight && (
                  <span style={err}>{errors.governorHeight.message}</span>
                )}
              </label>
            </div>
          )}
        </fieldset>

        <div style={hint}>
          id и creationDate генерируются на сервере. Отправляется один объект City:
          <code> coordinatesId</code> <i>или</i> <code>coordinates</code>,
          и <code>governorId</code>/<code>governor</code>/<code>пусто</code>. Поле <code>government</code> может быть null.
        </div>
      </form>
    </Modal>
  );
}


const twoCols = {
  display: "grid",
  gridTemplateColumns: "repeat(2, minmax(220px, 1fr))",
  gap: 10,
};
const row = {
  display: "flex",
  gap: 12,
  alignItems: "center",
  marginBottom: 8,
  flexWrap: "wrap",
};
const radioLabel = { display: "inline-flex", gap: 6, alignItems: "center" };

const label = { display: "grid", gap: 6, fontSize: 14, width: "100%" };
const input = (error) => ({
  width: "100%",
  display: "block",
  boxSizing: "border-box",
  padding: "8px 10px",
  borderRadius: 8,
  border: `1px solid ${error ? "#ef4444" : "#d1d5db"}`,
  outline: "none",
});
const err = { color: "#b91c1c", fontSize: 12 };
const hint = {
  color: "#92400e",
  background: "#fef3c7",
  padding: "6px 8px",
  borderRadius: 6,
  fontSize: 12,
};
const box = { border: "1px solid #e5e7eb", borderRadius: 8, padding: 10 };
const legend = { padding: "0 6px", fontSize: 12, color: "#6b7280" };
const btnLight = {
  padding: "8px 12px",
  borderRadius: 8,
  background: "#fff",
  border: "1px solid #d1d5db",
  cursor: "pointer",
};
const btnPrimary = {
  ...btnLight,
  background: "#4f46e5",
  color: "#fff",
  borderColor: "#4f46e5",
};
