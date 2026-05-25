const BASE = '/api';

async function request(path, options = {}) {
  const res = await fetch(BASE + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const json = await res.json();
  if (!json.success) throw new Error(json.message || 'Request failed');
  return json.data;
}

export const api = {
  getStatus:        ()           => request('/parking/status'),
  getPricing:       ()           => request('/parking/pricing'),
  getActiveTickets: ()           => request('/parking/active-tickets'),
  getTicket:        (id)         => request(`/parking/ticket/${id}`),
  parkVehicle:      (body)       => request('/parking/entry', { method: 'POST', body: JSON.stringify(body) }),
  exitVehicle:      (ticketId)   => request(`/parking/exit/${ticketId}`, { method: 'POST' }),
  getParkingLot:    ()           => request('/admin/parking-lot'),
  addFloor:         (body)       => request('/admin/floors', { method: 'POST', body: JSON.stringify(body) }),
  addSpot:          (floorId, b) => request(`/admin/floors/${floorId}/spots`, { method: 'POST', body: JSON.stringify(b) }),
};
