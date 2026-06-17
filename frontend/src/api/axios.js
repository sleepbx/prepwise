import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('prepwise_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';

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
