import React from 'react';
import { AlertCircle, AlertTriangle } from 'lucide-react';

export default function AnomalyFlag({ flag }) {
    const isDuplicate = flag.fieldName?.includes('(Duplicate)');
    const isTrend = flag.fieldName?.includes('(Price Surge)') || flag.fieldName?.includes('(Unusual Drop)') ||
                    flag.fieldName?.includes('(Term Contraction)') || flag.fieldName?.includes('(Invalid Window)');
    const categoryLabel = isDuplicate ? 'Duplicate Alert' : isTrend ? 'Trend Anomaly' : 'Template Deviation';
    const categoryBadgeColor = isDuplicate ? 'rgba(246, 90, 90, 0.25)' : isTrend ? 'rgba(56, 189, 248, 0.2)' : 'rgba(168, 85, 247, 0.2)';
    const categoryTextColor = isDuplicate ? '#F87171' : isTrend ? '#38BDF8' : '#C084FC';

    // Clean up display field name if it has parenthetical suffix
    const cleanFieldName = flag.fieldName?.replace(/\s*\([^)]*\)/, '') || flag.fieldName;

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
            <div className="flex-1 min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                    <h4 className="text-sm font-semibold text-white">
                        {isDuplicate ? 'Duplicate Found in' : isTrend ? 'Trend Discrepancy in' : 'Deviation in'}{' '}
                        <span style={{ color: 'var(--color-aurora-end)' }}>{cleanFieldName}</span>
                    </h4>
                    <span
                        className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded border"
                        style={{
                            background: categoryBadgeColor,
                            borderColor: categoryTextColor,
                            color: categoryTextColor,
                        }}
                    >
                        {categoryLabel}
                    </span>
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
                <p className="text-sm mt-1.5" style={{ color: 'var(--color-text-secondary)', lineHeight: '1.5' }}>
                    {flag.description}
                </p>
            </div>
        </div>
    );
}
