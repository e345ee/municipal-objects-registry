import React from "react";
import Modal from "../components/Modal";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { HumansApi } from "../api/humans";

const FLOAT_MAX = 3.4028235e38;

const isSafeFloat32 = (n) => {
  const buf = new Float32Array(1);
  buf[0] = n;
  const asFloat = buf[0];

  if (!Number.isFinite(asFloat)) return false;

  if (asFloat === n) return true;

  const diff = Math.abs(asFloat - n);

  if (n === 0) {
    return diff === 0;
  }

  const rel = diff / Math.abs(n);
  return rel < 1e-6;
};

const schema = z.object({
  height: z.preprocess(
    (v) => {
      if (v === "" || v === null || v === undefined) return undefined;

      if (typeof v === "string") {
        const trimmed = v.trim();
        const normalized = trimmed.replace(",", ".");
        const lower = normalized.toLowerCase();

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

        const floatLike =
          /^[+-]?(?:\d+\.?\d*|\d*\.?\d+)(?:[eE][+-]?\d+)?$/;

        if (!floatLike.test(normalized)) {
          return NaN;
        }

        return Number(normalized);
      }

      if (typeof v !== "number") {
        return NaN;
      }

      return v;
    },
    z
      .number({
        required_error: "Рост обязателен",
        invalid_type_error: "Введите число",
      })
      .refine(Number.isFinite, "Недопустимое число")
      .positive("Рост должен быть > 0")
      .refine(
        (n) => Math.abs(n) <= FLOAT_MAX,
        "Рост не может выходить за пределы float"
      )
      .refine(
        (n) => isSafeFloat32(n),
        "Значение слишком точное для float (потеря точности)"
      )
  ),
});

export default function HumanCreateDialog({ open, onClose, onCreated }) {
  const { register, handleSubmit, formState, reset, watch } = useForm({
    resolver: zodResolver(schema),
    mode: "onChange",
    defaultValues: { height: "" },
  });

  const { errors, isValid, isSubmitting } = formState;
  const heightVal = watch("height");

  const onSubmit = async (values) => {
    try {
      await HumansApi.create({ height: values.height });
      reset({ height: "" });
      onCreated?.();
      onClose();
    } catch (e) {
      console.error(e);
      alert("Не удалось создать человека");
    }
  };

  const blockExpAndSigns = (e) => {
    if (["e", "E", "+", "-"].includes(e.key)) {
      e.preventDefault();
    }
  };

  const handlePaste = (e) => {
    let text = e.clipboardData.getData("text").trim();
    text = text.replace(",", "."); 

    const floatLike =
      /^[+-]?(?:\d+\.?\d*|\d*\.?\d+)(?:[eE][+-]?\d+)?$/;

    if (!floatLike.test(text)) {
      e.preventDefault();
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

  const numHeight = Number(heightVal);

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Создать человека"
      footer={footer}
    >
      <div style={{ display: "grid", gap: 10 }}>
        <label style={label}>
          <span>Рост (метры)</span>
          <input
            type="number"
            step="any"
            min={0.0001}
            inputMode="decimal"
            onKeyDown={blockExpAndSigns}
            onPaste={handlePaste}
            onWheel={(e) => e.currentTarget.blur()}
            placeholder="например, 1.80"
            {...register("height")}
            style={input(errors.height)}
          />
          {errors.height && (
            <div style={err}>{errors.height.message}</div>
          )}
        </label>

        {heightVal !== "" &&
          Number.isFinite(numHeight) &&
          numHeight <= 0 && (
            <div style={hint}>Рост должен быть больше 0</div>
          )}
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
