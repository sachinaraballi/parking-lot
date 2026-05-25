import { useEffect, useState } from 'react';
import { api } from '../../api/parkingApi';
import { Spinner } from '../shared/Spinner';

function StatCard({ label, value, sub, color }) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-5 flex flex-col gap-1 shadow-sm">
      <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{label}</span>
      <span className={`text-4xl font-bold ${color}`}>{value}</span>
      {sub && <span className="text-xs text-slate-400">{sub}</span>}
    </div>
  );
}

function SpotDot({ occupied, type }) {
  const typeColor = {
    SMALL:  occupied ? 'bg-red-400'    : 'bg-emerald-400',
    MEDIUM: occupied ? 'bg-orange-400' : 'bg-blue-400',
    LARGE:  occupied ? 'bg-red-500'    : 'bg-violet-400',
  };
  const label = { SMALL: 'S', MEDIUM: 'M', LARGE: 'L' }[type];
  return (
    <div
      title={`${type} — ${occupied ? 'Occupied' : 'Free'}`}
      className={`w-8 h-8 rounded-lg ${typeColor[type]} flex items-center justify-center text-white text-[10px] font-bold cursor-default transition-transform hover:scale-110`}
    >
      {label}
    </div>
  );
}

function FloorCard({ floor }) {
  const pct = floor.totalSpots ? Math.round((floor.occupiedSpots / floor.totalSpots) * 100) : 0;
  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5">
      <div className="flex items-center justify-between mb-3">
        <h3 className="font-semibold text-slate-700">Floor {floor.floorNumber}</h3>
        <span className="text-xs bg-slate-100 text-slate-500 rounded-full px-3 py-1">
          {floor.availableSpots}/{floor.totalSpots} free
        </span>
      </div>

      {/* occupancy bar */}
      <div className="h-1.5 bg-slate-100 rounded-full mb-4 overflow-hidden">
        <div
          className={`h-full rounded-full transition-all ${pct > 80 ? 'bg-red-500' : pct > 50 ? 'bg-orange-400' : 'bg-emerald-500'}`}
          style={{ width: `${pct}%` }}
        />
      </div>

      {/* spot grid */}
      <div className="flex flex-wrap gap-1.5">
        {floor.spots.map(spot => (
          <SpotDot key={spot.id} occupied={spot.occupied} type={spot.type} />
        ))}
      </div>

      {/* legend */}
      <div className="mt-3 flex gap-3 text-[10px] text-slate-400 flex-wrap">
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-sm bg-emerald-400 inline-block"/>S free</span>
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-sm bg-blue-400 inline-block"/>M free</span>
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-sm bg-violet-400 inline-block"/>L free</span>
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-sm bg-red-400 inline-block"/>Occupied</span>
      </div>
    </div>
  );
}

export function Dashboard() {
  const [status, setStatus] = useState(null);
  const [lotData, setLotData] = useState(null);
  const [loading, setLoading] = useState(true);

  async function refresh() {
    setLoading(true);
    try {
      const [s, lot] = await Promise.all([api.getStatus(), api.getParkingLot()]);
      setStatus(s);
      // merge spot detail from admin endpoint into floors
      const floorsWithSpots = s.floors.map(sf => {
        const full = lot.floors.find(f => f.id === sf.floorId);
        return { ...sf, spots: full?.spots ?? [] };
      });
      setLotData(floorsWithSpots);
    } catch { /* ignore */ }
    setLoading(false);
  }

  useEffect(() => { refresh(); }, []);

  if (loading) return (
    <div className="flex justify-center items-center h-64"><Spinner size="lg" /></div>
  );
  if (!status) return <p className="text-slate-400 text-center py-20">Could not load status.</p>;

  return (
    <div className="space-y-6">
      {/* header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">{status.name}</h2>
          <p className="text-sm text-slate-400">{status.address}</p>
        </div>
        <button onClick={refresh} className="flex items-center gap-2 text-sm text-blue-600 hover:text-blue-800 font-medium">
          ↻ Refresh
        </button>
      </div>

      {/* stat cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <StatCard label="Total Spots"     value={status.totalSpots}    color="text-slate-700" />
        <StatCard label="Available"       value={status.availableSpots} color="text-emerald-600" />
        <StatCard label="Occupied"        value={status.occupiedSpots}  color="text-red-500" />
        <StatCard label="Occupancy"       value={status.occupancyRate}  color="text-orange-500"
          sub={`${status.activeTickets} active ticket${status.activeTickets !== 1 ? 's' : ''}`} />
      </div>

      {/* floor grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        {lotData?.map(floor => <FloorCard key={floor.floorId} floor={floor} />)}
      </div>
    </div>
  );
}
