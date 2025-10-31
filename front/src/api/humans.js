import api from "./client";

export const HumansApi = {

  list: async ({ page = 0, size = 10, sort = ["id,asc"], filters = {} } = {}) => {
    const params = new URLSearchParams();
    params.set("page", page);
    params.set("size", size);

    (Array.isArray(sort) ? sort : [sort]).forEach((s) => {
      if (s) params.append("sort", s);
    });

    if (filters && filters.id !== undefined && filters.id !== null && `${filters.id}` !== "") {
      params.set("id", filters.id);
    }
    if (filters && filters.height !== undefined && filters.height !== null && `${filters.height}` !== "") {
      params.set("height", filters.height);
    }

    const { data } = await api.get(`/api/humans?${params.toString()}`);
    return data;
  },

  create: async (dto) => {
    const { data } = await api.post(`/api/humans`, dto);
    return data;
  },

  update: async (id, dto) => {
    const { data } = await api.put(`/api/humans/${id}`, dto);
    return data;
  },

  remove: async (id) => {
    await api.delete(`/api/humans/${id}`);
  },

  get: async (id) => {
    const { data } = await api.get(`/api/humans/${id}`);
    return data;
  },
};
