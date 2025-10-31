import api from "./client";

export const CoordinatesApi = {

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
    if (filters && filters.x !== undefined && filters.x !== null && `${filters.x}` !== "") {
      params.set("x", filters.x);
    }
    if (filters && filters.y !== undefined && filters.y !== null && `${filters.y}` !== "") {
      params.set("y", filters.y);
    }

    const { data } = await api.get(`/api/coordinates?${params.toString()}`);
    return data; 
  },

  create: async (dto) => {
    const { data } = await api.post(`/api/coordinates`, dto);
    return data;
  },

  update: async (id, dto) => {
    const { data } = await api.put(`/api/coordinates/${id}`, dto);
    return data;
  },

  remove: async (id) => {
    await api.delete(`/api/coordinates/${id}`);
  },

  get: async (id) => {
    const { data } = await api.get(`/api/coordinates/${id}`);
    return data;
  },
};
