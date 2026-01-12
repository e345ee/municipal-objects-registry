import api from "./client";

export const ImportsApi = {
  list: async ({ page = 0, size = 25 } = {}) => {
    const params = new URLSearchParams();
    params.set("page", String(page));
    params.set("size", String(size));

    const { data } = await api.get(`/api/imports?${params.toString()}`);
    return data;
  },
};

export default ImportsApi;
