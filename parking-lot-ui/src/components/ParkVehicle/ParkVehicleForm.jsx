import { useState } from 'react';
import { api } from '../../api/parkingApi';

const VEHICLE_TYPES = [
  { value: 'MOTORCYCLE', label: '🏍️ Motorcycle', rate: '$1.00/hr', spots: 'SMALL/MEDIUM/LARGE' },
  { value: 'CAR',        label: '🚗 Car',         rate: '$2.00/hr', spots: 'MEDIUM/LARGE' },
  { value: 'TRUCK',      label: '🚛 Truck',       rate: '$3.50/hr', spots: 'LARGE only' },
];

export function ParkVehicleForm({ onSuccess }) {
  const [plate, setPlate] = useState('');
  const [type, setType]   = useState('CAR');
  const [loading, setLoading] = useState(false);
  const [result, setResult]   = useState(null);
  const [error, setError]     = useState('');

  async function submit(e) {
    e.preventDefault();
    if (!plate.trim()) { setError('License plate is required.'); return; }
    setLoading(true);
    setError('');
    setResult(null);
    try {
      const ticket = await api.parkVehicle({ licensePlate: plate.trim(), vehicleType: type });
      setResult(ticket);
      setPlate('');
      onSuccess?.('Vehicle parked — ticket ' + ticket.id);
    } catch (err) {
      setError(err.message);
    }
    setLoading(false);
  }

  return (
    <div className="max-w-xl space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-slate-800">Park a Vehicle</h2>
        <p className="text-sm text-slate-400 mt-1">Enter the license plate and select vehicle type to get a ticket.</p>
      </div>

      <form onSubmit={submit} className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 space-y-5">
        {/* license plate */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">License Plate</label>
          <input
            value={plate}
            onChange={e => setPlate(e.target.value.toUpperCase())}
            placeholder="e.g. MH12AB1234"
            className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-slate-800 font-mono uppercase tracking-widest focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* vehicle type */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Vehicle Type</label>
          <div className="grid grid-cols-3 gap-3">
            {VEHICLE_TYPES.map(vt => (
              <button
                key={vt.value}
                type="button"
                onClick={() => setType(vt.value)}
                className={`border rounded-xl p-3 text-center transition-all ${
                  type === vt.value
                    ? 'border-blue-600 bg-blue-50 ring-2 ring-blue-300'
                    : 'border-slate-200 hover:border-slate-300 bg-white'
                }`}
              >
                <div className="text-2xl">{vt.label.split(' ')[0]}</div>
                <div className="text-xs font-semibold text-slate-700 mt-1">{vt.label.split(' ').slice(1).join(' ')}</div>
                <div className="text-[10px] text-slate-400">{vt.rate}</div>
              </button>
            ))}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">
            Fits in: {VEHICLE_TYPES.find(v => v.value === type)?.spots}
          </p>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 rounded-xl">{error}</div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-semibold py-3 rounded-xl transition-colors"
        >
          {loading ? 'Parking...' : 'Park Vehicle'}
        </button>
      </form>

      {/* ticket result */}
      {result && (
        <div className="bg-gradient-to-br from-blue-600 to-blue-800 text-white rounded-2xl p-6 shadow-lg space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-lg">🎫 Parking Ticket</h3>
            <span className="text-xs bg-white/20 rounded-full px-3 py-1 font-mono">{result.id}</span>
          </div>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div><div className="opacity-60 text-xs uppercase tracking-wide">Vehicle</div><div className="font-semibold">{result.vehicle?.licensePlate}</div></div>
            <div><div className="opacity-60 text-xs uppercase tracking-wide">Type</div><div className="font-semibold">{result.vehicle?.type}</div></div>
            <div><div className="opacity-60 text-xs uppercase tracking-wide">Floor</div><div className="font-semibold">{result.floorNumber}</div></div>
            <div><div className="opacity-60 text-xs uppercase tracking-wide">Spot</div><div className="font-semibold">{result.spotNumber} ({result.spotType})</div></div>
            <div className="col-span-2"><div className="opacity-60 text-xs uppercase tracking-wide">Entry Time</div><div className="font-semibold">{new Date(result.entryTime).toLocaleString()}</div></div>
          </div>
          <p className="text-xs opacity-70 pt-1 border-t border-white/20">Save this ticket ID to exit the lot.</p>
        </div>
      )}
    </div>
  );
}
