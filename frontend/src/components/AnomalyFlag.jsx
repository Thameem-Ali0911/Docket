import React from 'react';
import { AlertCircle, AlertTriangle } from 'lucide-react';

export default function AnomalyFlag({ flag }) {
    const isCritical = flag.severity === 'CRITICAL' || flag.severity === 'HIGH';

    return (
        <div
            className="p-4 rounded-xl border flex items-start gap-3 mb-3"
            style={{
                background: isCritical ? 'rgba(246, 90, 90, 0.12)' : 'rgba(245, 165, 36, 0.12)',
                borderColor: isCritical ? 'rgba(246, 90, 90, 0.3)' : 'rgba(245, 165, 36, 0.3)',
            }}
        >
            <div className="mt-0.5 shrink-0" style={{ color: isCritical ? '#F65A5A' : '#F5A524' }}>
                {isCritical ? <AlertCircle size={18} /> : <AlertTriangle size={18} />}
            </div>
            <div>
                <div className="flex items-center gap-2">
                    <h4 className="text-sm font-semibold text-white">
                        Deviation in <span style={{ color: 'var(--color-aurora-end)' }}>{flag.fieldName}</span>
                    </h4>
                    <span
                        className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded"
                        style={{
                            background: isCritical ? 'rgba(246, 90, 90, 0.2)' : 'rgba(245, 165, 36, 0.2)',
                            color: isCritical ? '#F65A5A' : '#F5A524',
                        }}
                    >
                        {flag.severity || 'WARNING'}
                    </span>
                </div>
                <p className="text-sm mt-1" style={{ color: 'var(--color-text-secondary)', lineHeight: '1.5' }}>
                    {flag.description}
                </p>
            </div>
        </div>
    );
}
