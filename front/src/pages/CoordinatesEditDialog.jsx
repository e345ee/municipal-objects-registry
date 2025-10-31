import React from "react";
import Modal from "../components/Modal";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { CoordinatesApi } from "../api/coordinates";


const schema = z.object({
  x: z.preprocess(
    v => (v === "" || v === null ? undefined : Number(v)),
    z.number({ invalid_type_error: "Введите число" })
     .finite("Недопустимое число")
     .max(460, "X не может быть > 460")
  ),
  y: z.preprocess(
    v => (v === "" || v === null ? undefined : Number(v)),
    z.number({ invalid_type_error: "Введите число" })
     .finite("Недопустимое число")
  ),
});

export default function CoordinatesEditDialog({ open, onClose, coord, onUpdated }) {
  const { register, handleSubmit, formState, reset, watch } = useForm({
    resolver: zodResolver(schema),
    mode: "onChange",
    defaultValues: { x: "", y: "" }
  });

  const { errors, isValid, isSubmitting } = formState;
  const xVal = watch("x");
  const yVal = watch("y");


  React.useEffect(() => {
    if (open && coord) {
      reset({
        x: coord.x ?? "",
        y: coord.y ?? "",
      });
    }
  }, [open, coord, reset]);

  const onSubmit = async (values) => {
    try {
      await CoordinatesApi.update(coord.id, { x: values.x, y: values.y });
      onUpdated?.();
      onClose();
    } catch (e) {
      console.error(e);
      alert("Не удалось обновить координаты");
    }
  };


  const blockExp = (e) => {
    if (["e", "E"].includes(e.key)) e.preventDefault();
  };

  const footer = (
    <>
      <button onClick={onClose} disabled={isSubmitting}>Отмена</button>
      <button
        onClick={handleSubmit(onSubmit)}
        disabled={!isValid || isSubmitting}
        style={{ padding: "6px 12px", borderRadius: 6 }}
      >
        {isSubmitting ? "Сохранение..." : "Сохранить"}
      </button>
    </>
  );

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`Изменить координаты${coord ? ` (ID ${coord.id})` : ""}`}
      footer={footer}
    >
      <div style={{ display: "grid", gap: 12 }}>
        <label style={label}>
          <span>X (≤ 460)</span>
          <input
            type="number"
            step="any"
            max={460}
            onKeyDown={blockExp}
            inputMode="decimal"
            placeholder="например, 120"
            {...register("x")}
            style={input(errors.x)}
          />
          {errors.x && <div style={err}>{errors.x.message}</div>}
          {xVal !== "" && Number(xVal) > 460 && (
            <div style={hint}>Значение X не может быть больше 460</div>
          )}
        </label>

        <label style={label}>
          <span>Y (обязательно)</span>
          <input
            type="number"
            step="any"
            onKeyDown={blockExp}
            inputMode="decimal"
            placeholder="например, 45"
            {...register("y")}
            style={input(errors.y)}
          />
          {errors.y && <div style={err}>{errors.y.message}</div>}
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
const hint = { color: "#92400e", background: "#fef3c7", padding: "6px 8px", borderRadius: 6, fontSize: 12 };
