import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

// --- Cold-start handling -------------------------------------------------
// The backend runs on a free tier that spins down after idle. The first
// request to a sleeping instance returns 429/502/503/504 or times out while
// it wakes (can take ~1 min). Those statuses mean the request never actually
// processed, so it is safe to retry transparently — including POSTs like
// login/register. We emit a `prepwise:waking` event so the UI can show a
// "waking up the server" state instead of a false error.
const COLD_START_STATUSES = [429, 502, 503, 504];
const MAX_RETRIES = 12;

const emitWaking = (isWaking) => {
  window.dispatchEvent(new CustomEvent('prepwise:waking', { detail: isWaking }));
};

const isColdStartError = (error) => {
  const status = error.response?.status;
  if (status && COLD_START_STATUSES.includes(status)) return true;
  // No response + not an explicit client cancel => network drop or timeout
  if (!error.response && error.code !== 'ERR_CANCELED') return true;
  return false;
};

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('prepwise_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => {
    emitWaking(false);
    return res;
  },
  async (error) => {
    const config = error.config || {};
    const status = error.response?.status;
    const url = config.url || '';

    // Retry cold-start failures with backoff before surfacing an error.
    if (isColdStartError(error)) {
      config.__retryCount = config.__retryCount || 0;
      if (config.__retryCount < MAX_RETRIES) {
        config.__retryCount += 1;
        emitWaking(true);
        // Backoff: 3s, 6s, 9s, then capped at 10s (~100s total coverage).
        await delay(Math.min(3000 * config.__retryCount, 10000));
        return api(config);
      }
    }

    // Done retrying (or not retryable) — clear the waking state.
    emitWaking(false);

    // Only force logout on explicit auth failure — not on AI/analytics 403 or server errors
    if (status === 401 && !url.includes('/auth/')) {
      const hadToken = !!localStorage.getItem('prepwise_token');
      if (hadToken) {
        localStorage.removeItem('prepwise_token');
        localStorage.removeItem('prepwise_user');
        const path = window.location.pathname;
        if (!path.startsWith('/login') && !path.startsWith('/register')) {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
