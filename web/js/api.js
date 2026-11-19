const API_BASE = 'http://localhost:8080/api/v1';

const api = {
  token() {
    return localStorage.getItem('ce_token') || '';
  },

  async request(path, method, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = this.token();
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }
    const response = await fetch(API_BASE + path, {
      method: method || 'GET',
      headers: headers,
      body: body ? JSON.stringify(body) : undefined
    });
    if (!response.ok) {
      throw new Error('服务异常（' + response.status + '）');
    }
    const result = await response.json();
    if (result.code !== 0) {
      throw new Error(result.message || '操作失败');
    }
    return result.data;
  },

  get(path) {
    return this.request(path, 'GET');
  },

  post(path, body) {
    return this.request(path, 'POST', body);
  },

  put(path, body) {
    return this.request(path, 'PUT', body);
  },

  del(path) {
    return this.request(path, 'DELETE');
  }
};

function toIso(value) {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(value);
  const pad = (n) => String(n).padStart(2, '0');
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) +
    'T' + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':00';
}

function fmtTime(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').substring(0, 16);
}
