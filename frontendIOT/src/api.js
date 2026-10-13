// Gọi REST API của backend Spring (xem vite.config.js để biết proxy)
const BASE = '/api/v1'

async function request(path, options) {
  const res = await fetch(BASE + path, options)
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.message || 'Lỗi ' + res.status)
  }
  return res.status === 204 ? null : res.json()
}

const query = (params) => {
  const p = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => v !== '' && v != null && p.set(k, v))
  return '?' + p
}

export const getLatest = () => request('/sensor-data')
export const getChart = (limit = 20) => request('/sensor-data/chart' + query({ limit }))
export const getSensorList = (params) => request('/sensor-data-list' + query(params))
export const getLedStatus = () => request('/led-status')
export const controlLed = (led, state) =>
  request('/led-control', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ led, state }) })
export const getHistory = (params) => request('/action-history' + query(params))
export const getProfile = () => request('/profile')
