import { useEffect, useState } from 'react';
import { api } from '../../api/parkingApi';
import { Spinner } from '../shared/Spinner';

function Section({ title, children }) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 space-y-4">
      <h3 className="font-semibold text-slate-700 border-b border-slate-100 pb-3">{title}</h3>
      {children}
    </div>
  );
}

function Field({ label, children }) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>
      {children}
    </div>
  );
}

export function AdminPanel({ onSuccess }) {
  const [floors, setFloors]     = useState([]);
  const [loading, setLoading]   = useState(true);

  // add floor
  const [floorNum, setFloorNum] = useState('');
  const [floorErr, setFloorErr] = useState('');
  const [floorBusy, setFloorBusy] = useState(false);

  // add spot
  const [selFloor, setSelFloor] = useState('');
  const [spotNum, setSpotNum]   = useState('');
  const [spotType, setSpotType] = useState('MEDIUM');
  const [spotErr, setSpotErr]   = useState('');
  const [spotBusy, setSpotBusy] = useState(false);

  // ticket lookup
  const [ticketId, setTicketId] = useState('');
  const [ticketResult, setTicketResult] = useState(null);
  const [ticketErr, setTicketErr] = useState('');
  const [ticketBusy, setTicketBusy] = useState(false);

  async function loadFloors() {
    setLoading(true);
    try {
      const lot = await api.getParkingLot();
      setFloors(lot.floors ?? []);
      if (!selFloor && lot.floors?.length) setSelFloor(lot.floors[0].id);
    } catch { /* ignore */ }
    setLoading(false);
  }

  useEffect(() => { loadFloors(); }, []);

  async function submitFloor(e) {
    e.preventDefault();
    if (!floorNum) { setFloorErr('Floor number required.'); return; }
    setFloorBusy(true); setFloorErr('');
    try {
      await api.addFloor({ floorNumber: Number(floorNum) });
      setFloorNum('');
      await loadFloors();
      onSuccess?.('Floor ' + floorNum + ' added.');
    } catch (err) { setFloorErr(err.message); }
    setFloorBusy(false);
  }

  async function submitSpot(e) {
    e.preventDefault();
    if (!selFloor) { setSpotErr('Select a floor.'); return; }
    if (!spotNum)  { setSpotErr('Spot number required.'); return; }
    setSpotBusy(true); setSpotErr('');
    try {
      await api.addSpot(selFloor, { spotNumber: spotNum, spotType });
      setSpotNum('');
      await loadFloors();
      const floorNo = floors.find(f => f.id === selFloor)?.floorNumber;
      onSuccess?.(`Spot ${spotNum} added to Floor ${floorNo}.`);
    } catch (err) { setSpotErr(err.message); }
    setSpotBusy(false);
  }

  async function lookupTicket(e) {
    e.preventDefault();
    if (!ticketId.trim()) { setTicketErr('Ticket ID required.'); return; }
    setTicketBusy(true); setTicketErr(''); setTicketResult(null);
    try {
      setTicketResult(await api.getTicket(ticketId.trim()));
    } catch (err) { setTicketErr(err.message); }
    setTicketBusy(false);
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-slate-800">Admin Panel</h2>
        <p className="text-sm text-slate-400 mt-1">Manage floors, spots, and look up tickets.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Add Floor */}
        <Section title="➕ Add Floor">
          <form onSubmit={submitFloor} className="space-y-3">
            <Field label="Floor Number">
              <input
                type="number" min="1" value={floorNum}
                onChange={e => setFloorNum(e.target.value)}
                placeholder="e.g. 4"
                className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </Field>
            {floorErr && <p className="text-red-600 text-sm">{floorErr}</p>}
            <button disabled={floorBusy} className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-semibold py-2.5 rounded-xl">
              {floorBusy ? 'Adding...' : 'Add Floor'}
            </button>
          </form>
        </Section>

        {/* Add Spot */}
        <Section title="➕ Add Spot to Floor">
          <form onSubmit={submitSpot} className="space-y-3">
            <Field label="Select Floor">
              {loading ? <Spinner size="sm" /> : (
                <select
                  value={selFloor}
                  onChange={e => setSelFloor(e.target.value)}
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {floors.map(f => <option key={f.id} value={f.id}>Floor {f.floorNumber}</option>)}
                </select>
              )}
            </Field>
            <Field label="Spot Number">
              <input
                value={spotNum}
                onChange={e => setSpotNum(e.target.value.toUpperCase())}
                placeholder="e.g. F4-L1"
                className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-slate-800 font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </Field>
            <Field label="Spot Type">
              <div className="grid grid-cols-3 gap-2">
                {['SMALL','MEDIUM','LARGE'].map(t => (
                  <button
                    key={t} type="button"
                    onClick={() => setSpotType(t)}
                    className={`py-2 rounded-xl text-sm font-medium border transition-all ${
                      spotType === t ? 'bg-blue-600 text-white border-blue-600' : 'border-slate-200 text-slate-600 hover:border-slate-300'
                    }`}
                  >{t}</button>
                ))}
              </div>
            </Field>
            {spotErr && <p className="text-red-600 text-sm">{spotErr}</p>}
            <button disabled={spotBusy} className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-semibold py-2.5 rounded-xl">
              {spotBusy ? 'Adding...' : 'Add Spot'}
            </button>
          </form>
        </Section>
      </div>

      {/* Ticket Lookup */}
      <Section title="🔍 Ticket Lookup">
        <form onSubmit={lookupTicket} className="flex gap-3">
          <input
            value={ticketId}
            onChange={e => setTicketId(e.target.value.toUpperCase())}
            placeholder="TKT-XXXXXXXX"
            className="flex-1 border border-slate-300 rounded-xl px-4 py-2.5 text-slate-800 font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button disabled={ticketBusy} className="bg-slate-800 hover:bg-slate-900 disabled:opacity-50 text-white font-semibold px-5 py-2.5 rounded-xl">
            {ticketBusy ? '...' : 'Search'}
          </button>
        </form>
        {ticketErr && <p className="text-red-600 text-sm">{ticketErr}</p>}
        {ticketResult && (
          <div className="bg-slate-50 rounded-xl p-4 text-sm space-y-2 border border-slate-200">
            {[
              ['Ticket ID', ticketResult.id],
              ['License', ticketResult.vehicle?.licensePlate],
              ['Type', ticketResult.vehicle?.type],
              ['Floor', ticketResult.floorNumber],
              ['Spot', `${ticketResult.spotNumber} (${ticketResult.spotType})`],
              ['Entry', new Date(ticketResult.entryTime).toLocaleString()],
              ['Exit', ticketResult.exitTime ? new Date(ticketResult.exitTime).toLocaleString() : '—'],
              ['Amount', ticketResult.amount ? `$${ticketResult.amount.toFixed(2)}` : 'Pending'],
              ['Status', ticketResult.status],
            ].map(([k, v]) => (
              <div key={k} className="flex justify-between">
                <span className="text-slate-400">{k}</span>
                <span className={`font-medium ${k === 'Status' ? (v === 'PAID' ? 'text-green-600' : 'text-orange-500') : 'text-slate-700'}`}>{v}</span>
              </div>
            ))}
          </div>
        )}
      </Section>

      {/* Current Floors */}
      <Section title="🏢 Floor Overview">
        {loading ? <Spinner /> : (
          <div className="space-y-2">
            {floors.map(f => (
              <div key={f.id} className="flex items-center justify-between bg-slate-50 rounded-xl px-4 py-3">
                <span className="font-medium text-slate-700">Floor {f.floorNumber}</span>
                <div className="flex gap-3 text-xs text-slate-500">
                  {['SMALL','MEDIUM','LARGE'].map(t => {
                    const count = f.spots?.filter(s => s.type === t).length ?? 0;
                    if (!count) return null;
                    return <span key={t}>{count}× {t}</span>;
                  })}
                  <span className="text-slate-300">|</span>
                  <span className="font-semibold text-slate-600">{f.spots?.length ?? 0} total</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </Section>
    </div>
  );
}
