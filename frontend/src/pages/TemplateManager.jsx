import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch, clearToken } from '../lib/api';

export default function TemplateManager() {
    const navigate = useNavigate();
    const [documents, setDocuments] = useState([]);
    const [currentTemplate, setCurrentTemplate] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [successMsg, setSuccessMsg] = useState(null);

    useEffect(() => {
        loadData();
    }, []);

    function loadData() {
        setLoading(true);
        setError(null);
        
        Promise.all([
            apiFetch('/api/documents'),
            apiFetch('/api/templates/INVOICE').catch(err => {
                // Return null if no template exists or 404
                return null;
            })
        ])
            .then(([docsData, templateData]) => {
                setDocuments(docsData.filter(d => d.status === 'PROCESSED' && d.type === 'INVOICE'));
                setCurrentTemplate(templateData);
            })
            .catch((err) => {
                if (err.status === 401) {
                    clearToken();
                    navigate('/login', { replace: true });
                    return;
                }
                setError(err.message || 'Failed to load templates.');
            })
            .finally(() => setLoading(false));
    }

    function handleSetTemplate(documentId) {
        setSuccessMsg(null);
        setError(null);
        
        apiFetch('/api/templates', {
            method: 'POST',
            body: JSON.stringify({ documentId: parseInt(documentId, 10) })
        })
            .then((newTemplate) => {
                setSuccessMsg('Template updated successfully!');
                // Reload to get the latest template document object
                loadData();
            })
            .catch(err => setError(err.message || 'Failed to set template.'));
    }

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center" style={{ background: 'var(--color-bg)' }}>
                <p style={{ color: 'var(--color-text-secondary)' }}>Loading…</p>
            </div>
        );
    }

    return (
        <div className="min-h-screen" style={{ background: 'var(--color-bg)' }}>
            <nav className="card flex items-center justify-between px-6 py-3"
                 style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none' }}>
                <div className="flex items-center gap-3">
                    <h2 className="text-xl font-bold" style={{ fontFamily: 'var(--font-heading)', color: 'var(--color-primary)' }}>
                        Docket
                    </h2>
                </div>
                <button onClick={() => navigate('/dashboard')} className="btn-secondary" style={{ padding: '6px 14px', fontSize: '13px' }}>
                    ← Back to dashboard
                </button>
            </nav>

            <main className="max-w-3xl mx-auto px-6 py-12">
                <div className="mb-8">
                    <h1>Template Manager</h1>
                    <p style={{ color: 'var(--color-text-secondary)', marginTop: '8px' }}>
                        Set a standard document format to compare new uploads against for anomaly detection.
                    </p>
                </div>

                {error && <div className="alert-error mb-6">{error}</div>}
                {successMsg && <div className="p-4 rounded-lg bg-green-50 text-green-900 border border-green-200 mb-6">{successMsg}</div>}

                <div className="card p-6 mb-8">
                    <h3 className="mb-4">Invoice Template</h3>
                    
                    {currentTemplate ? (
                        <div className="mb-6 p-4 rounded-lg border bg-gray-50 flex items-center justify-between">
                            <div>
                                <p className="font-semibold text-sm text-gray-900">Current Template</p>
                                <p className="text-sm text-gray-600 mt-1">
                                    {currentTemplate.fileUrl.split('/').pop()} (ID: {currentTemplate.id})
                                </p>
                            </div>
                            <span className="badge badge-success">Active</span>
                        </div>
                    ) : (
                        <div className="mb-6 p-4 rounded-lg border bg-yellow-50 text-yellow-800 text-sm">
                            No template is currently set for Invoices. New invoices will not be checked for anomalies.
                        </div>
                    )}

                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">
                            Select a new template
                        </label>
                        {documents.length === 0 ? (
                            <p className="text-sm text-gray-500">You must upload and process at least one invoice before you can set a template.</p>
                        ) : (
                            <div className="flex gap-3">
                                <select 
                                    className="flex-1 rounded-lg border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm p-2 border"
                                    id="template-select"
                                >
                                    <option value="">-- Select an invoice --</option>
                                    {documents.map(doc => (
                                        <option key={doc.id} value={doc.id}>
                                            ID: {doc.id} - {doc.fileUrl.split('/').pop()}
                                        </option>
                                    ))}
                                </select>
                                <button 
                                    className="btn-primary"
                                    onClick={() => {
                                        const select = document.getElementById('template-select');
                                        if (select.value) handleSetTemplate(select.value);
                                    }}
                                >
                                    Save
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}
