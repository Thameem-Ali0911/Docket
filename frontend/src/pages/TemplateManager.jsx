import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import { ArrowLeft, SlidersHorizontal, CheckCircle2, AlertTriangle, Trash2, Eye, Upload } from 'lucide-react';
import { apiFetch, clearToken } from '../lib/api';
import AmbientAurora from '../components/ui/AmbientAurora';

const DOCUMENT_TYPES = [
    { type: 'INVOICE', label: 'Invoices', singular: 'Invoice', description: 'Standard billing layout, vendor terms, tax structures, and line item formats.' },
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
            <div className="min-h-screen flex items-center justify-center relative overflow-hidden" style={{ background: 'var(--color-bg)' }}>
                <AmbientAurora opacity={0.35} />
                <div className="flex flex-col items-center gap-3 z-10">
                    <div className="w-8 h-8 rounded-full border-2 border-t-transparent animate-spin" style={{ borderColor: 'var(--color-aurora-start)', borderTopColor: 'transparent' }} />
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: '14px' }}>Loading templates…</p>
                </div>
            </div>
        );
    }

    const currentTypeConfig = DOCUMENT_TYPES.find(d => d.type === activeType);
    const currentTemplate = templates[activeType];
    const matchingDocs = documents.filter(d => d.status === 'PROCESSED' && d.type === activeType);

    return (
        <div className="min-h-screen relative overflow-hidden" style={{ background: 'var(--color-bg)' }}>
            <AmbientAurora opacity={0.4} />

            {/* Top navigation bar */}
            <nav className="card relative z-20 flex items-center justify-between px-6 py-3.5"
                 style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none', background: 'rgba(18, 16, 27, 0.85)' }}>
                <div className="flex items-center gap-3">
                    <h2 className="text-xl font-bold cursor-pointer tracking-tight"
                        onClick={() => navigate('/dashboard')}>
                        <span className="text-aurora">Docket</span>
                    </h2>
                </div>
                <button onClick={() => navigate('/dashboard')} className="btn-secondary" style={{ padding: '5px 14px', fontSize: '13px' }}>
                    <ArrowLeft size={14} />
                    Back to Dashboard
                </button>
            </nav>

            <main className="max-w-3xl mx-auto px-6 py-10 relative z-10">
                <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3 }}
                    className="mb-8"
                >
                    <h1 className="text-3xl font-bold">Template Manager</h1>
                    <p style={{ color: 'var(--color-text-secondary)', marginTop: '6px', fontSize: '14px' }}>
                        Establish standard benchmark documents per category. New uploads are automatically evaluated against these templates for non-standard terms, unexpected clauses, and deviations.
                    </p>
                </motion.div>

                {error && <div className="alert-error mb-6">{error}</div>}
                {successMsg && (
                    <motion.div
                        initial={{ opacity: 0, y: -8 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="p-4 rounded-xl mb-6 flex items-center justify-between"
                        style={{ background: 'rgba(52, 211, 153, 0.14)', border: '1px solid rgba(52, 211, 153, 0.3)', color: '#34D399' }}
                    >
                        <span className="text-sm font-medium">{successMsg}</span>
                        <button onClick={() => setSuccessMsg(null)} className="text-emerald-300 font-bold ml-4 hover:text-white">✕</button>
                    </motion.div>
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
                                        ? 'border-purple-400 text-white bg-purple-950/30 rounded-t-xl'
                                        : 'border-transparent text-gray-400 hover:text-white'
                                }`}
                                style={{
                                    borderBottomColor: isActive ? 'var(--color-aurora-start)' : 'transparent',
                                    marginBottom: '-1px'
                                }}
                            >
                                <span>{label}</span>
                                {hasTemplate ? (
                                    <span className="w-2 h-2 rounded-full bg-emerald-400" title="Template active" />
                                ) : (
                                    <span className="w-2 h-2 rounded-full bg-gray-600" title="No template set" />
                                )}
                            </button>
                        );
                    })}
                </div>

                {/* Active Tab Panel */}
                <motion.div
                    key={activeType}
                    initial={{ opacity: 0, y: 6 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.2 }}
                    className="card p-6 mb-8"
                    style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                >
                    <div className="mb-5">
                        <h3 className="text-lg font-bold">{currentTypeConfig?.singular} Template</h3>
                        <p className="text-xs text-gray-400 mt-1">{currentTypeConfig?.description}</p>
                    </div>

                    {currentTemplate ? (
                        <div className="mb-6 p-4 rounded-xl border flex items-center justify-between"
                             style={{ background: 'var(--color-surface-raised)', borderColor: 'var(--color-border)' }}>
                            <div className="flex items-center gap-3">
                                <div className="p-2.5 rounded-lg" style={{ background: 'rgba(124, 92, 252, 0.15)', color: 'var(--color-aurora-start)' }}>
                                    <CheckCircle2 size={22} className="text-emerald-400" />
                                </div>
                                <div>
                                    <div className="flex items-center gap-2">
                                        <p className="font-semibold text-sm text-white">
                                            {currentTemplate.fileUrl.split('/').pop()}
                                        </p>
                                        <span className="badge badge-success">Active Standard</span>
                                    </div>
                                    <p className="text-xs mt-1 text-gray-400">
                                        ID: #{currentTemplate.id} • Uploaded: {new Date(currentTemplate.uploadedAt).toLocaleDateString()}
                                    </p>
                                </div>
                            </div>
                            <div className="flex items-center gap-2">
                                <button
                                    onClick={() => navigate(`/documents/${currentTemplate.id}`)}
                                    className="btn-secondary"
                                    style={{ padding: '5px 12px', fontSize: '12px' }}
                                >
                                    <Eye size={13} />
                                    View
                                </button>
                                <button
                                    onClick={() => handleDeleteTemplate(activeType)}
                                    disabled={actionLoading}
                                    className="btn-secondary"
                                    style={{
                                        padding: '5px 12px',
                                        fontSize: '12px',
                                        color: '#F65A5A',
                                        borderColor: 'rgba(246, 90, 90, 0.3)'
                                    }}
                                    title="Remove this standard template"
                                >
                                    <Trash2 size={13} />
                                    Remove
                                </button>
                            </div>
                        </div>
                    ) : (
                        <div className="mb-6 p-4 rounded-xl border text-sm flex items-center gap-2.5"
                             style={{ background: 'var(--color-warning-light)', color: '#F5A524', borderColor: 'rgba(245, 165, 36, 0.3)' }}>
                            <AlertTriangle size={18} className="shrink-0" />
                            <span>No standard template is currently set for {currentTypeConfig?.label}. New {currentTypeConfig?.label.toLowerCase()} will not be evaluated for anomalies until a benchmark is chosen.</span>
                        </div>
                    )}

                    <div className="pt-4 border-t" style={{ borderColor: 'var(--color-border)' }}>
                        <label className="block text-xs font-semibold uppercase tracking-wider mb-2 text-gray-400">
                            {currentTemplate ? `Change ${currentTypeConfig?.singular} Standard` : `Select a processed ${currentTypeConfig?.singular} as standard`}
                        </label>

                        {matchingDocs.length === 0 ? (
                            <div className="p-4 rounded-xl border text-sm flex items-center justify-between"
                                 style={{ background: 'var(--color-surface-raised)', borderColor: 'var(--color-border)' }}>
                                <p style={{ color: 'var(--color-text-secondary)' }}>
                                    Upload and process at least one {currentTypeConfig?.singular.toLowerCase()} before setting it as a standard.
                                </p>
                                <button
                                    className="btn-primary ml-4 shrink-0"
                                    style={{ padding: '6px 14px', fontSize: '13px' }}
                                    onClick={() => navigate('/upload')}
                                >
                                    <Upload size={14} />
                                    Upload {currentTypeConfig?.singular}
                                </button>
                            </div>
                        ) : (
                            <div className="flex gap-3">
                                <select
                                    className="flex-1 input text-sm"
                                    value={selectedDocId}
                                    onChange={(e) => setSelectedDocId(e.target.value)}
                                    disabled={actionLoading}
                                >
                                    <option value="">-- Choose a processed {currentTypeConfig?.singular.toLowerCase()} --</option>
                                    {matchingDocs.map(doc => (
                                        <option key={doc.id} value={doc.id}>
                                            ID: #{doc.id} — {doc.fileUrl.split('/').pop()} ({new Date(doc.uploadedAt).toLocaleDateString()})
                                        </option>
                                    ))}
                                </select>
                                <button
                                    className="btn-primary shrink-0"
                                    disabled={!selectedDocId || actionLoading}
                                    onClick={() => handleSetTemplate(selectedDocId)}
                                >
                                    {actionLoading ? 'Saving…' : 'Set as Standard'}
                                </button>
                            </div>
                        )}
                    </div>
                </motion.div>
            </main>
        </div>
    );
}
