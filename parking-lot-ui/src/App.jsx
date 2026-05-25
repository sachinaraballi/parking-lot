import { useState, useCallback } from 'react';
import { Dashboard }       from './components/Dashboard/Dashboard';
import { ParkVehicleForm } from './components/ParkVehicle/ParkVehicleForm';
import { ActiveTicketsList }from './components/ActiveTickets/ActiveTicketsList';
import { PricingTable }    from './components/Pricing/PricingTable';
import { AdminPanel }      from './components/Admin/AdminPanel';
import { Toast }           from './components/shared/Toast';

const NAV = [
  { id: 'dashboard',  label: 'Dashboard',       icon: '🏠' },
  { id: 'park',       label: 'Park Vehicle',     icon: '🚘' },
  { id: 'tickets',    label: 'Active Tickets',   icon: '🎫' },
  { id: 'pricing',    label: 'Pricing',          icon: '💰' },
  { id: 'admin',      label: 'Admin',            icon: '⚙️'  },
];

export default function App() {
  const [page, setPage]   = useState('dashboard');
  const [toast, setToast] = useState(null);

  const notify = useCallback((msg, type = 'success') => {
    setToast({ msg, type, key: Date.now() });
  }, []);

  const pages = {
    dashboard: <Dashboard />,
    park:      <ParkVehicleForm   onSuccess={msg => notify(msg)} />,
    tickets:   <ActiveTicketsList onSuccess={msg => notify(msg)} />,
    pricing:   <PricingTable />,
    admin:     <AdminPanel        onSuccess={msg => notify(msg)} />,
  };

  return (
    <div className="min-h-screen bg-slate-50 flex">
      {/* sidebar */}
      <aside className="w-56 shrink-0 bg-white border-r border-slate-200 flex flex-col shadow-sm sticky top-0 h-screen">
        <div className="px-5 py-5 border-b border-slate-100">
          <div className="text-xl font-black text-slate-800 tracking-tight">🅿️ ParkOS</div>
          <div className="text-xs text-slate-400 mt-0.5">Management System</div>
        </div>
        <nav className="flex-1 px-3 py-4 space-y-1">
          {NAV.map(n => (
            <button
              key={n.id}
              onClick={() => setPage(n.id)}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all text-left ${
                page === n.id
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              <span className="text-base">{n.icon}</span>
              {n.label}
            </button>
          ))}
        </nav>
        <div className="px-5 py-4 border-t border-slate-100">
          <div className="text-[10px] text-slate-400">Backend: localhost:8080</div>
        </div>
      </aside>

      {/* main content */}
      <main className="flex-1 p-6 overflow-auto">
        <div className="max-w-5xl mx-auto">
          {pages[page]}
        </div>
      </main>

      {/* toast */}
      {toast && (
        <Toast
          key={toast.key}
          message={toast.msg}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  );
}
