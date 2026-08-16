import React from 'react';

export default function AnomalyFlag({ flag }) {
    const isCritical = flag.severity === 'CRITICAL';
    
    return (
        <div className={`p-4 rounded-lg border flex items-start gap-3 mb-3 ${
            isCritical ? 'bg-red-50 border-red-200' : 'bg-amber-50 border-amber-200'
        }`}>
            <div className="mt-0.5">
                {isCritical ? '🚨' : '⚠️'}
            </div>
            <div>
                <h4 className={`text-sm font-semibold ${isCritical ? 'text-red-900' : 'text-amber-900'}`}>
                    Anomaly detected in {flag.fieldName}
                </h4>
                <p className={`text-sm mt-1 ${isCritical ? 'text-red-700' : 'text-amber-800'}`}>
                    {flag.description}
                </p>
            </div>
        </div>
    );
}
