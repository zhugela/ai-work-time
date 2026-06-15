// frontend/assets/api.js —— Axios 风格极简 HTTP 客户端 + 拦截器
const Api = (() => {
  const BASE = window.JZ_API_BASE || 'http://127.0.0.1:8080';
  function getToken() { try { return localStorage.getItem('jz_token') || ''; } catch (_) { return ''; } }
  function setToken(t) { try { localStorage.setItem('jz_token', t || ''); } catch (_) {} }

  async function request(method, path, body, opts = {}) {
    const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    if (opts.idempotencyKey) headers['Idempotency-Key'] = opts.idempotencyKey;
    const res = await fetch(BASE + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    const ct = res.headers.get('content-type') || '';
    const data = ct.includes('application/json') ? await res.json() : await res.blob();
    if (!res.ok || (data && data.code !== 0 && data.code !== undefined)) {
      const err = new Error((data && data.msg) || ('HTTP ' + res.status));
      err.code = (data && data.code) || res.status;
      err.data = data;
      throw err;
    }
    return data;
  }
  return {
    BASE, getToken, setToken,
    get: (p) => request('GET', p),
    post: (p, b, o) => request('POST', p, b, o),
    put: (p, b) => request('PUT', p, b),
    del: (p) => request('DELETE', p),
    upload: async (path, formData) => {
      const token = getToken();
      const res = await fetch(BASE + path, {
        method: 'POST',
        headers: token ? { 'Authorization': 'Bearer ' + token } : {},
        body: formData,
      });
      return res.json();
    },
    download: async (path, filename) => {
      const token = getToken();
      const res = await fetch(BASE + path, {
        method: 'GET',
        headers: token ? { 'Authorization': 'Bearer ' + token } : {},
      });
      const blob = await res.blob();
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = filename;
      a.click();
      URL.revokeObjectURL(a.href);
    },
  };
})();

window.Api = Api;
