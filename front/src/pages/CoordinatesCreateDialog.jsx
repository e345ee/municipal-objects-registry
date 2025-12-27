import React from "react";
import Modal from "../components/Modal";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { CoordinatesApi } from "../api/coordinates";

const FLOAT_MAX = 3.4028235e38; 

const floatLike = /^[+-]?(?:\d+\.?\d*|\d*\.?\d+)(?:[eE][+-]?\d+)?$/;

const floatSchema = z.preprocess(
  (v) => {
    if (v === "" || v === null || v === undefined) return undefined;

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
    .number({
      required_error: "Число обязательно",
      invalid_type_error: "Введите число",
    })
    .refine(Number.isFinite, "Недопустимое число") 
    .refine(
      (n) => Math.abs(n) <= FLOAT_MAX,
      "Число не может выходить за пределы float"
    )
);


const schema = z.object({
  x: floatSchema.refine((v) => v <= 460, "X не может быть больше 460"),
  y: floatSchema, 
});

export default function CoordinatesCreateDialog({ open, onClose, onCreated }) {
  const { register, handleSubmit, formState, reset, watch, setValue } = useForm({
    resolver: zodResolver(schema),
    mode: "onChange",
    defaultValues: { x: "", y: "" },
  });

  const { errors, isValid, isSubmitting } = formState;
  const yVal = watch("y");

  const blockBadKeys = (e) => {
    const allowedControl = [
      "Backspace",
      "Delete",
      "ArrowLeft",
      "ArrowRight",
      "ArrowUp",
      "ArrowDown",
      "Tab",
      "Home",
      "End",
    ];
    if (allowedControl.includes(e.key)) return;


    if (!/[0-9.-]/.test(e.key)) {
      e.preventDefault();
      return;
    }

    const input = e.currentTarget;
    const { selectionStart, value } = input;


    if (e.key === "-") {
      if (selectionStart !== 0 || value.includes("-")) {
        e.preventDefault();
      }
    }


    if (e.key === ".") {
      if (value.includes(".")) {
        e.preventDefault();
      }
    }
  };

  const sanitizePaste = (field) => (e) => {
    const txt = (e.clipboardData || window.clipboardData).getData("text") ?? "";


    if (/inf(inity)?/i.test(txt) || /\bnan\b/i.test(txt)) {
      e.preventDefault();
      return;
    }


    const cleaned = txt
      .replace(/[^\d.-]/g, "")
      .replace(/(?!^)-/g, "")       
      .replace(/(\..*)\./g, "$1");  
    if (cleaned !== txt) e.preventDefault();
    if (cleaned) {
      setValue(field, cleaned, { shouldValidate: true, shouldDirty: true });
    }
  };

  const onSubmit = async (values) => {
    try {
      await CoordinatesApi.create({ x: values.x, y: values.y });
      reset({ x: "", y: "" });
      onCreated?.();
      onClose();
    } catch (e) {
      console.error(e);
      alert("Не удалось создать координаты");
    }
  };

  const footer = (
    <>
      <button onClick={onClose} disabled={isSubmitting}>
        Отмена
      </button>
      <button
        onClick={handleSubmit(onSubmit)}
        disabled={!isValid || isSubmitting}
        style={{ padding: "6px 12px", borderRadius: 6 }}
      >
        {isSubmitting ? "Создание..." : "Создать"}
      </button>
    </>
  );

  return (
    <Modal open={open} onClose={onClose} title="Создать координаты" footer={footer}>
      <div style={{ display: "grid", gap: 12 }}>
        <label style={label}>
          <span>X (≤ 460)</span>
          <input
            type="text"
            inputMode="decimal"
            onKeyDown={blockBadKeys}
            onPaste={sanitizePaste("x")}
            onWheel={(e) => e.currentTarget.blur()}
            placeholder="например, 120.5"
            {...register("x")}
            style={input(errors.x)}
          />
          {errors.x && <div style={err}>{errors.x.message}</div>}
        </label>

        <label style={label}>
          <span>Y (обязательно)</span>
          <input
            type="text"
            inputMode="decimal"
            onKeyDown={blockBadKeys}
            onPaste={sanitizePaste("y")}
            onWheel={(e) => e.currentTarget.blur()}
            placeholder="например, -35.2"
            {...register("y")}
            style={input(errors.y)}
          />
          {errors.y && <div style={err}>{errors.y.message}</div>}
          {yVal === "" && !errors.y && (
            <div style={hint}>Y обязательно для заполнения</div>
          )}
        </label>
      </div>
    </Modal>
  );
}

const label = { display: "grid", gap: 6, fontSize: 14 };
const input = (error) => ({
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
