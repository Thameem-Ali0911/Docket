import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch, clearToken } from '../lib/api';

const DOCUMENT_TYPES = [
    { type: 'INVOICE', label: 'Invoices', singular: 'Invoice', description: 'Standard billing layout, typical vendor terms, tax structures, and line item formats.' },
    { type: 'CONTRACT', label: 'Contracts', singular: 'Contract', description: 'Standard legal terms, termination notice clauses, liability limitations, and governing law.' },
    { type: 'RESUME', label: 'Resumes', singular: 'Resume', description: 'Expected qualification standards, required skill profiles, and section structures.' },
];

export default function TemplateManager() {
    const navigate = useNavigate();
    const [activeType, setActiveType] = useState('INVOICE');
    const [documents, setDocuments] = useState([]);
    const [templates, setTemplates] = useState({}); // type -> document
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);
    const [error, setError] = useState(null);
    const [successMsg, setSuccessMsg] = useState(null);
    const [selectedDocId, setSelectedDocId] = useState('');

    useEffect(() => {
        loadData();
    }, []);

    // Reset selected document dropdown when switching types
    useEffect(() => {
        setSelectedDocId('');
        setError(null);
        setSuccessMsg(null);
    }, [activeType]);

    async function loadData() {
        setLoading(true);
        setError(null);

        try {
            const [docsData, templatesData] = await Promise.all([
                apiFetch('/api/documents'),
                apiFetch('/api/templates').catch(() => [])
            ]);

            setDocuments(docsData);

            // Map list of templates by document type
            const templateMap = {};
            if (Array.isArray(templatesData)) {
                templatesData.forEach(t => {
                    if (t.documentType && t.document) {
                        templateMap[t.documentType] = t.document;
                    }
                });
            }
            setTemplates(templateMap);
        } catch (err) {
            if (err.status === 401) {
                clearToken();
                navigate('/login', { replace: true });
                return;
            }
            setError(err.message || 'Failed to load templates.');
        } finally {
            setLoading(false);
        }
    }

    async function handleSetTemplate(documentId) {
        if (!documentId) return;
        setActionLoading(true);
        setSuccessMsg(null);
        setError(null);

        try {
            await apiFetch('/api/templates', {
                method: 'POST',
                body: JSON.stringify({ documentId: parseInt(documentId, 10) })
            });
            setSuccessMsg(`Template for ${DOCUMENT_TYPES.find(d => d.type === activeType)?.label || activeType} updated successfully!`);
            setSelectedDocId('');
            await loadData();
        } catch (err) {
            setError(err.message || 'Failed to set template.');
        } finally {
            setActionLoading(false);
        }
    }

    async function handleDeleteTemplate(type) {
        const typeConfig = DOCUMENT_TYPES.find(d => d.type === type);
        const typeName = typeConfig ? typeConfig.singular : type;
        if (!window.confirm(`Are you sure you want to remove the standard template for ${typeName}s? New uploads will not be checked for anomalies until a new template is set.`)) {
            return;
        }

        setActionLoading(true);
        setSuccessMsg(null);
        setError(null);

        try {
            await apiFetch(`/api/templates/${type}`, {
                method: 'DELETE'
            });
            setSuccessMsg(`Template for ${typeConfig?.label || type} removed successfully.`);
            setTemplates(prev => {
                const next = { ...prev };
                delete next[type];
                return next;
            });
        } catch (err) {
            setError(err.message || 'Failed to remove template.');
        } finally {
            setActionLoading(false);
        }
    }

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center" style={{ background: 'var(--color-bg)' }}>
                <p style={{ color: 'var(--color-text-secondary)' }}>Loading templates…</p>
            </div>
        );
    }

    const currentTypeConfig = DOCUMENT_TYPES.find(d => d.type === activeType);
    const currentTemplate = templates[activeType];
    const matchingDocs = documents.filter(d => d.status === 'PROCESSED' && d.type === activeType);

    return (
        <div className="min-h-screen" style={{ background: 'var(--color-bg)' }}>
            {/* Top navigation bar */}
            <nav className="card flex items-center justify-between px-6 py-3"
                 style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none' }}>
                <div className="flex items-center gap-3">
                    <h2 className="text-xl font-bold cursor-pointer"
                        onClick={() => navigate('/dashboard')}
                        style={{ fontFamily: 'var(--font-heading)', color: 'var(--color-primary)' }}>
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
                        Set a standard document format per type to compare new uploads against for automatic anomaly and deviation detection.
                    </p>
                </div>

                {error && <div className="alert-error mb-6">{error}</div>}
                {successMsg && (
                    <div className="p-4 rounded-lg bg-green-50 text-green-900 border border-green-200 mb-6 flex items-center justify-between">
                        <span>{successMsg}</span>
                        <button onClick={() => setSuccessMsg(null)} className="text-green-700 font-bold ml-4">✕</button>
                    </div>
                )}

                {/* Document Type Tabs */}
                <div className="flex gap-2 mb-6 border-b" style={{ borderColor: 'var(--color-border)' }}>
                    {DOCUMENT_TYPES.map(({ type, label }) => {
                        const isActive = activeType === type;
                        const hasTemplate = !!templates[type];
                        return (
                            <button
                                key={type}
                                onClick={() => setActiveType(type)}
                                className={`py-3 px-5 text-sm font-semibold flex items-center gap-2 border-b-2 transition-all cursor-pointer ${
                                    isActive
                                        ? 'border-[var(--color-primary)] text-[var(--color-primary)] bg-white rounded-t-lg'
                                        : 'border-transparent text-gray-500 hover:text-gray-800'
                                }`}
                                style={{
                                    borderBottomColor: isActive ? 'var(--color-primary)' : 'transparent',
                                    color: isActive ? 'var(--color-primary)' : 'var(--color-text-secondary)',
                                    marginBottom: '-1px'
                                }}
                            >
                                <span>{label}</span>
                                {hasTemplate ? (
                                    <span className="w-2 h-2 rounded-full" style={{ backgroundColor: 'var(--color-accent)' }} title="Template active" />
                                ) : (
                                    <span className="w-2 h-2 rounded-full bg-gray-300" title="No template set" />
                                )}
                            </button>
                        );
                    })}
                </div>

                {/* Active Tab Panel */}
                <div className="card p-6 mb-8">
                    <div className="flex items-center justify-between mb-4">
                        <div>
                            <h3>{currentTypeConfig?.singular} Template</h3>
                            <p className="text-xs text-gray-500 mt-1">{currentTypeConfig?.description}</p>
                        </div>
                    </div>

                    {currentTemplate ? (
                        <div className="mb-6 p-4 rounded-lg border flex items-center justify-between"
                             style={{ background: 'var(--color-bg)', borderColor: 'var(--color-border)' }}>
                            <div className="flex items-center gap-3">
                                <div className="p-2 rounded bg-white border" style={{ borderColor: 'var(--color-border)' }}>
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none"
                                         stroke="var(--color-primary)" strokeWidth="2"
                                         strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                        <polyline points="14 2 14 8 20 8" />
                                        <line x1="16" y1="13" x2="8" y2="13" />
                                        <line x1="16" y1="17" x2="8" y2="17" />
                                    </svg>
                                </div>
                                <div>
                                    <div className="flex items-center gap-2">
                                        <p className="font-semibold text-sm" style={{ color: 'var(--color-text-primary)' }}>
                                            {currentTemplate.fileUrl.split('/').pop()}
                                        </p>
                                        <span className="badge badge-success">Active</span>
                                    </div>
                                    <p className="text-xs mt-1" style={{ color: 'var(--color-text-secondary)' }}>
                                        Document ID: {currentTemplate.id} • Uploaded: {new Date(currentTemplate.uploadedAt).toLocaleDateString()}
                                    </p>
                                </div>
                            </div>
                            <div className="flex items-center gap-2">
                                <button
                                    onClick={() => navigate(`/documents/${currentTemplate.id}`)}
                                    className="btn-secondary"
                                    style={{ padding: '6px 12px', fontSize: '13px' }}
                                >
                                    View Details
                                </button>
                                <button
                                    onClick={() => handleDeleteTemplate(activeType)}
                                    disabled={actionLoading}
                                    className="btn-secondary"
                                    style={{
                                        padding: '6px 12px',
                                        fontSize: '13px',
                                        color: 'var(--color-danger)',
                                        borderColor: 'var(--color-danger-light)'
                                    }}
                                    title="Remove this standard template"
                                >
                                    Remove Template
                                </button>
                            </div>
                        </div>
                    ) : (
                        <div className="mb-6 p-4 rounded-lg border text-sm"
                             style={{ background: 'var(--color-warning-light)', color: 'var(--color-warning)', borderColor: '#F2DFB8' }}>
                            ⚠️ No standard template is currently set for {currentTypeConfig?.label}. New {currentTypeConfig?.label.toLowerCase()} will not be checked for deviations.
                        </div>
                    )}

                    <div className="pt-2 border-t" style={{ borderColor: 'var(--color-border)' }}>
                        <label className="block text-sm font-semibold mb-2" style={{ color: 'var(--color-text-primary)' }}>
                            {currentTemplate ? `Change ${currentTypeConfig?.singular} Template` : `Designate a ${currentTypeConfig?.singular} as Standard Template`}
                        </label>

                        {matchingDocs.length === 0 ? (
                            <div className="p-4 rounded-lg border text-sm bg-gray-50 flex items-center justify-between" style={{ borderColor: 'var(--color-border)' }}>
                                <p style={{ color: 'var(--color-text-secondary)' }}>
                                    You must upload and process at least one {currentTypeConfig?.singular.toLowerCase()} before you can set it as a standard template.
                                </p>
                                <button
                                    className="btn-primary ml-4 shrink-0"
                                    style={{ padding: '6px 14px', fontSize: '13px' }}
                                    onClick={() => navigate('/upload')}
                                >
                                    Upload {currentTypeConfig?.singular}
                                </button>
                            </div>
                        ) : (
                            <div className="flex gap-3">
                                <select
                                    className="flex-1 input"
                                    value={selectedDocId}
                                    onChange={(e) => setSelectedDocId(e.target.value)}
                                    disabled={actionLoading}
                                >
                                    <option value="">-- Choose a processed {currentTypeConfig?.singular.toLowerCase()} --</option>
                                    {matchingDocs.map(doc => (
                                        <option key={doc.id} value={doc.id}>
                                            ID: {doc.id} — {doc.fileUrl.split('/').pop()} ({new Date(doc.uploadedAt).toLocaleDateString()})
                                        </option>
                                    ))}
                                </select>
                                <button
                                    className="btn-primary"
                                    disabled={!selectedDocId || actionLoading}
                                    onClick={() => handleSetTemplate(selectedDocId)}
                                >
                                    {actionLoading ? 'Saving…' : 'Set as Template'}
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}

