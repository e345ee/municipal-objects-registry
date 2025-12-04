import axios from "axios";

const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE || "",
  headers: { "Content-Type": "application/json" }
});

api.interceptors.response.use(
  (r) => r,
  (err) => {
    const resp = err.response;
    if (resp && resp.data && resp.data.message) {
      console.error("API error:", resp.data.message, resp.data);
    } else {
      console.error("Network/API error:", err.message);
    }
    return Promise.reject(err);
  }
);

export default api;