import React from "react";
import Modal from "../components/Modal";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { HumansApi } from "../api/humans";


const schema = z.object({
  height: z.preprocess(
    (v) => (v === "" || v === null ? undefined : Number(v)),
    z
      .number({ invalid_type_error: "Введите число" })
      .finite("Недопустимое число")          
      .positive("Рост должен быть > 0")
      .max(10000, "Рост не может быть > 10000")
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
    if (["e", "E", "+", "-"].includes(e.key)) e.preventDefault();
  };

  const footer = (
    <>
      <button onClick={onClose} disabled={isSubmitting}>Отмена</button>
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
    <Modal open={open} onClose={onClose} title="Создать человека" footer={footer}>
      <div style={{ display: "grid", gap: 10 }}>
        <label style={label}>
          <span>Рост (метры)</span>
          <input
            type="number"
            step="any"
            min={0.0001}
            max={10000}
            inputMode="decimal"
            onKeyDown={blockExpAndSigns}
            placeholder="например, 180"
            {...register("height")}
            style={input(errors.height)}
          />
          {errors.height && <div style={err}>{errors.height.message}</div>}
        </label>

        {heightVal !== "" && Number(heightVal) <= 0 && (
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
const hint = { color: "#92400e", background: "#fef3c7", padding: "6px 8px", borderRadius: 6, fontSize: 12 };