import { useEffect, useState } from 'react';
import { api } from '../../api/parkingApi';

const INFO = {
  MOTORCYCLE: { emoji: '🏍️', fits: 'SMALL · MEDIUM · LARGE', color: 'from-purple-500 to-violet-600' },
  CAR:        { emoji: '🚗', fits: 'MEDIUM · LARGE',          color: 'from-blue-500 to-cyan-600' },
  TRUCK:      { emoji: '🚛', fits: 'LARGE only',              color: 'from-orange-500 to-amber-600' },
};

function calc(hourlyRate, hours) {
  return (Math.max(1, hours) * hourlyRate).toFixed(2);
}

export function PricingTable() {
  const [pricing, setPricing] = useState(null);

  useEffect(() => { api.getPricing().then(setPricing).catch(() => {}); }, []);

  if (!pricing) return null;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-slate-800">Pricing</h2>
        <p className="text-sm text-slate-400 mt-1">Billing is per hour, ceiling to next full hour. Minimum 1 hour.</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {Object.entries(pricing).map(([type, info]) => {
          const meta = INFO[type];
          return (
            <div key={type} className={`bg-gradient-to-br ${meta.color} text-white rounded-2xl p-5 shadow-md`}>
              <div className="text-4xl mb-2">{meta.emoji}</div>
              <div className="font-bold text-lg">{type}</div>
              <div className="text-3xl font-black mt-1">${info.hourlyRate}<span className="text-sm font-normal opacity-80">/hr</span></div>
              <div className="text-xs opacity-75 mt-2">Fits in: {meta.fits}</div>
            </div>
          );
        })}
      </div>

      {/* estimation table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-100">
          <h3 className="font-semibold text-slate-700">Estimated Charges</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-5 py-3 text-left text-slate-500 font-medium">Duration</th>
                {Object.keys(pricing).map(type => (
                  <th key={type} className="px-5 py-3 text-center text-slate-500 font-medium">{type}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {[1, 2, 3, 6, 12, 24].map(h => (
                <tr key={h} className="hover:bg-slate-50">
                  <td className="px-5 py-3 font-medium text-slate-700">{h}h</td>
                  {Object.entries(pricing).map(([type, info]) => (
                    <td key={type} className="px-5 py-3 text-center text-slate-600 font-mono">
                      ${calc(info.hourlyRate, h)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
