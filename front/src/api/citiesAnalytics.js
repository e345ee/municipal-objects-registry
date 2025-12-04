import api from "./client";


export const CitiesAnalyticsApi = {
  averageTelephoneCode: async () => {
    const { data } = await api.get("/api/cities/avg-telephone-code");
    return data; 
  },

  namesStartingWith: async (prefix) => {
    const { data } = await api.get("/api/cities/names-starting", {
      params: { prefix },
    });
    return data; 
  },

  uniqueMetersAboveSeaLevel: async () => {
    const { data } = await api.get("/api/cities/meters-above-sea-level/unique");
    return data; 
  },

  distanceToLargest: async (x, y) => {
    const { data } = await api.get("/api/cities/distance-to-largest", {
      params: { x, y },
    });
    return data; 
  },

  distanceFromOriginToOldest: async () => {
    const { data } = await api.get("/api/cities/distance-from-origin-to-oldest");
    return data; 
  },
};

export default CitiesAnalyticsApi;
