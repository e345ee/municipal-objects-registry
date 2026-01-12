import api from "./client";


export const CitiesApi = {
  list: async ({
    page = 0,
    size = 25,
    sortBy = "id",
    dir = "asc",
    filters = {},
  } = {}) => {
    const params = new URLSearchParams();
    params.set("page", String(page));
    params.set("size", String(size));
    params.set("sortBy", String(sortBy));
    params.set("dir", String(dir).toLowerCase() === "desc" ? "desc" : "asc");

    Object.entries(filters || {}).forEach(([k, v]) => {
      if (v === undefined || v === null) return;
      const s = String(v).trim();
      if (!s) return;
      params.append(k, s);
    });

    const qs = params.toString();

    console.log("[CitiesApi] GET /api/cities?" + qs);

    const { data } = await api.get(`/api/cities?${qs}`);
    return data;
  },

  get: async (id) => {
    const { data } = await api.get(`/api/cities/${id}`);
    return data;
  },


  create: async (dto) => {
    const payload = { ...dto };
    delete payload.id;
    const { data } = await api.post(`/api/cities`, payload);
    return data;
  },

  update: async (id, dto) => {
    const payload = { ...dto };
    delete payload.id;
    const { data } = await api.put(`/api/cities/${id}`, payload);
    return data;
  },

  remove: async (
    id,
    opts = { deleteGovernorIfOrphan: false, deleteCoordinatesIfOrphan: false }
  ) => {
    const params = new URLSearchParams();
    Object.entries(opts).forEach(([k, v]) => params.append(k, String(v)));
    await api.delete(`/api/cities/${id}?${params.toString()}`);
  },

  importJson: async (file) => {
  const fd = new FormData();
  fd.append("file", file);
  const { data } = await api.post(`/api/cities/import`, fd, {
  headers: { "Content-Type": "multipart/form-data" }
   });
   return data;
 },

};

export default CitiesApi;
