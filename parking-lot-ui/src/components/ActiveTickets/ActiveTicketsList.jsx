import { useEffect, useState } from 'react';
import { api } from '../../api/parkingApi';
import { Spinner } from '../shared/Spinner';

const TYPE_BADGE = {
  MOTORCYCLE: 'bg-purple-100 text-purple-700',
  CAR:        'bg-blue-100 text-blue-700',
  TRUCK:      'bg-orange-100 text-orange-700',
};

function duration(entryTime) {
  const mins = Math.floor((Date.now() - new Date(entryTime)) / 60000);
  if (mins < 60) return `${mins}m`;
  return `${Math.floor(mins / 60)}h ${mins % 60}m`;
}

function ExitModal({ ticket, onConfirm, onCancel, loading }) {
  return (
    <div className="fixed inset-0 bg-black/40 z-40 flex items-center justify-center">
      <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-sm mx-4 space-y-4">
        <h3 className="font-bold text-slate-800 text-lg">Exit Parking Lot</h3>
        <p className="text-sm text-slate-500">
          Check out <span className="font-mono font-semibold text-slate-700">{ticket.vehicle?.licensePlate}</span>?
          They've been parked for <span className="font-semibold">{duration(ticket.entryTime)}</span>.
        </p>
        <div className="flex gap-3 pt-2">
          <button onClick={onCancel} className="flex-1 border border-slate-200 rounded-xl py-2.5 text-sm font-medium hover:bg-slate-50">Cancel</button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className="flex-1 bg-red-600 text-white rounded-xl py-2.5 text-sm font-semibold hover:bg-red-700 disabled:opacity-50"
          >
            {loading ? 'Processing...' : 'Confirm Exit'}
          </button>
        </div>
      </div>
    </div>
  );
}

function PaymentReceipt({ receipt, onClose }) {
  return (
    <div className="fixed inset-0 bg-black/40 z-40 flex items-center justify-center">
      <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-sm mx-4 space-y-4">
        <div className="text-center">
          <div className="w-14 h-14 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-3">
            <span className="text-2xl">✓</span>
          </div>
          <h3 className="font-bold text-slate-800 text-lg">Payment Successful</h3>
        </div>
        <div className="bg-slate-50 rounded-xl p-4 space-y-2 text-sm">
          {[
            ['Ticket',    receipt.ticketId],
            ['Vehicle',   receipt.licensePlate],
            ['Duration',  `${receipt.durationMinutes} min`],
            ['Rate',      receipt.pricingRate],
            ['Floor/Spot',`${receipt.floor} / ${receipt.spot}`],
          ].map(([k,v]) => (
            <div key={k} className="flex justify-between">
              <span className="text-slate-400">{k}</span>
              <span className="font-medium text-slate-700 text-right max-w-[60%] break-all">{v}</span>
            </div>
          ))}
          <div className="flex justify-between border-t border-slate-200 pt-2 mt-2">
            <span className="font-bold text-slate-700">Amount Due</span>
            <span className="font-bold text-green-600 text-lg">{receipt.amountDue}</span>
          </div>
        </div>
        <button onClick={onClose} className="w-full bg-slate-800 text-white rounded-xl py-3 font-semibold hover:bg-slate-900">
          Done
        </button>
      </div>
    </div>
  );
}

export function ActiveTicketsList({ onSuccess }) {
  const [tickets, setTickets]     = useState([]);
  const [loading, setLoading]     = useState(true);
  const [exiting, setExiting]     = useState(null);
  const [receipt, setReceipt]     = useState(null);
  const [exitLoading, setExitLoading] = useState(false);

  async function refresh() {
    setLoading(true);
    try { setTickets(await api.getActiveTickets()); } catch { /* ignore */ }
    setLoading(false);
  }

  useEffect(() => { refresh(); }, []);

  async function confirmExit() {
    setExitLoading(true);
    try {
      const result = await api.exitVehicle(exiting.id);
      setReceipt(result);
      setExiting(null);
      await refresh();
      onSuccess?.(`${result.licensePlate} checked out — ${result.amountDue}`);
    } catch (err) {
      onSuccess?.('Error: ' + err.message);
    }
    setExitLoading(false);
  }

  return (
    <div className="space-y-5">
      {exiting && <ExitModal ticket={exiting} onConfirm={confirmExit} onCancel={() => setExiting(null)} loading={exitLoading} />}
      {receipt  && <PaymentReceipt receipt={receipt} onClose={() => setReceipt(null)} />}

      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">Active Tickets</h2>
          <p className="text-sm text-slate-400 mt-0.5">Vehicles currently parked</p>
        </div>
        <button onClick={refresh} className="text-sm text-blue-600 hover:text-blue-800 font-medium">↻ Refresh</button>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : tickets.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-16 text-center">
          <div className="text-4xl mb-3">🅿️</div>
          <p className="text-slate-400 font-medium">No vehicles currently parked.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {tickets.map(t => (
            <div key={t.id} className="bg-white rounded-2xl border border-slate-200 shadow-sm p-4 flex items-center gap-4">
              <div className="text-3xl">{t.vehicle?.type === 'MOTORCYCLE' ? '🏍️' : t.vehicle?.type === 'TRUCK' ? '🚛' : '🚗'}</div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-mono font-bold text-slate-800">{t.vehicle?.licensePlate}</span>
                  <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${TYPE_BADGE[t.vehicle?.type]}`}>{t.vehicle?.type}</span>
                </div>
                <div className="text-sm text-slate-400 mt-0.5">
                  Floor {t.floorNumber} · {t.spotNumber} ({t.spotType}) · In since {new Date(t.entryTime).toLocaleTimeString()}
                </div>
              </div>
              <div className="text-right hidden sm:block">
                <div className="text-sm font-semibold text-slate-700">{duration(t.entryTime)}</div>
                <div className="text-xs text-slate-400 font-mono">{t.id}</div>
              </div>
              <button
                onClick={() => setExiting(t)}
                className="bg-red-50 hover:bg-red-100 text-red-600 font-semibold text-sm px-4 py-2 rounded-xl transition-colors whitespace-nowrap"
              >
                Exit
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
