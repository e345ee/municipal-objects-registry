import api from "./client";

const AdminApi = {
  getInfraOverview: async () => {
    const { data } = await api.get("/api/admin/infra/overview");
    return data;
  },

  setSimulatedFailure: async (target, enabled) => {
    const params = new URLSearchParams();
    params.set("target", String(target));
    params.set("enabled", String(!!enabled));
    const { data } = await api.post(`/api/admin/infra/simulated-failure?${params.toString()}`);
    return data;
  },

  setL2StatsLogging: async (enabled) => {
    const params = new URLSearchParams();
    params.set("enabled", String(!!enabled));
    const { data } = await api.post(`/api/admin/cache/l2-stats-logging?${params.toString()}`);
    return data;
  },

  purgeAllObjects: async () => {
    await api.delete("/api/purge");
  },

  listMinioFiles: async ({ page = 0, size = 10 } = {}) => {
    const params = new URLSearchParams();
    params.set("page", String(page));
    params.set("size", String(size));
    const { data } = await api.get(`/api/admin/storage/files?${params.toString()}`);
    return data;
  },

  reimportMinioFile: async (key, { debugFailStage } = {}) => {
    const params = new URLSearchParams();
    params.set("key", String(key));
    if (debugFailStage) params.set("debugFailStage", String(debugFailStage));
    const { data } = await api.post(`/api/admin/storage/reimport?${params.toString()}`);
    return data;
  },
};

export default AdminApi;
