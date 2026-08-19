import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'motion/react';
import { ArrowLeft, CheckCircle2, AlertTriangle, Trash2, Eye, Upload, SlidersHorizontal } from 'lucide-react';
import { apiFetch, clearToken } from '../lib/api';
import AmbientAurora from '../components/ui/AmbientAurora';

const DOCUMENT_TYPES = [
    { type: 'INVOICE', label: 'Invoices', singular: 'Invoice', emoji: '🧾', description: 'Standard billing layout, vendor terms, tax structures, and line item formats.' },
    { type: 'CONTRACT', label: 'Contracts', singular: 'Contract', emoji: '📄', description: 'Standard legal terms, termination clauses, liability limitations, and governing law.' },
    { type: 'RESUME', label: 'Resumes', singular: 'Resume', emoji: '👤', description: 'Expected qualification standards, required skill profiles, and section structures.' },
];

/**
 * Template Manager — neomorphic tab switcher, card-inset active template display.
 */
export default function TemplateManager() {
    const navigate = useNavigate();
    const [activeType, setActiveType] = useState('INVOICE');
    const [documents, setDocuments] = useState([]);
    const [templates, setTemplates] = useState({});
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);
    const [error, setError] = useState(null);
    const [successMsg, setSuccessMsg] = useState(null);
    const [selectedDocId, setSelectedDocId] = useState('');

    useEffect(() => { loadData(); }, []);
    useEffect(() => { setSelectedDocId(''); setError(null); setSuccessMsg(null); }, [activeType]);

    async function loadData() {
        setLoading(true);
        setError(null);
        try {
            const [docsData, templatesData] = await Promise.all([
                apiFetch('/api/documents'),
                apiFetch('/api/templates').catch(() => [])
            ]);
            setDocuments(docsData);
            const map = {};
            if (Array.isArray(templatesData)) {
                templatesData.forEach(t => { if (t.documentType && t.document) map[t.documentType] = t.document; });
            }
            setTemplates(map);
        } catch (err) {
            if (err.status === 401) { clearToken(); navigate('/login', { replace: true }); return; }
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
            const label = DOCUMENT_TYPES.find(d => d.type === activeType)?.label || activeType;
            setSuccessMsg(`${label} template updated successfully.`);
            setSelectedDocId('');
            await loadData();
        } catch (err) {
            setError(err.message || 'Failed to set template.');
        } finally {
            setActionLoading(false);
        }
    }

    async function handleDeleteTemplate(type) {
        const cfg = DOCUMENT_TYPES.find(d => d.type === type);
        if (!window.confirm(`Remove the ${cfg?.singular} standard template? New uploads will not be anomaly-checked until a new one is set.`)) return;
        setActionLoading(true);
        setSuccessMsg(null);
        setError(null);
        try {
            await apiFetch(`/api/templates/${type}`, { method: 'DELETE' });
            setSuccessMsg(`${cfg?.label || type} template removed.`);
            setTemplates(prev => { const next = { ...prev }; delete next[type]; return next; });
        } catch (err) {
            setError(err.message || 'Failed to remove template.');
        } finally {
            setActionLoading(false);
        }
    }

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center relative overflow-hidden"
                 style={{ background: 'var(--color-bg)' }}>
                <AmbientAurora opacity={0.35} />
                <div className="flex flex-col items-center gap-3 z-10">
                    <div className="w-8 h-8 rounded-full border-2 animate-spin"
                         style={{ borderColor: 'var(--color-aurora-start)', borderTopColor: 'transparent' }} />
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: 14 }}>Loading templates…</p>
                </div>
            </div>
        );
    }

    const cfg = DOCUMENT_TYPES.find(d => d.type === activeType);
    const currentTemplate = templates[activeType];
    const matchingDocs = documents.filter(d => d.status === 'PROCESSED' && d.type === activeType);

    return (
        <div className="min-h-screen relative overflow-hidden" style={{ background: 'var(--color-bg)' }}>
            <AmbientAurora opacity={0.35} />

            {/* Frosted nav */}
            <nav className="nav-frosted sticky top-0 z-30 px-4 sm:px-8 py-0">
                <div className="w-full max-w-[1600px] mx-auto flex items-center justify-between h-[52px]">
                    <div className="flex items-center gap-2.5">
                        <span className="text-aurora" style={{ fontSize: 17, fontWeight: 800 }}>Docket</span>
                        <span style={{ color: 'var(--color-text-disabled)', fontSize: 13 }}>/</span>
                        <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--color-text-secondary)' }}>Templates</span>
                    </div>
                    <button onClick={() => navigate('/dashboard')} className="btn-secondary"
                        style={{ padding: '6px 14px', fontSize: 13 }}>
                        <ArrowLeft size={13} /> Dashboard
                    </button>
                </div>
            </nav>

            <main className="max-w-[780px] mx-auto px-4 sm:px-6 py-8 relative z-10">

                {/* Page header */}
                <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
                    style={{ marginBottom: 24 }}
                >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
                        <div style={{
                            width: 38, height: 38, borderRadius: 10,
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            background: 'rgba(124,92,252,0.13)', color: 'var(--color-aurora-start)',
                            boxShadow: 'var(--neo-shadow-sm)',
                        }}>
                            <SlidersHorizontal size={17} />
                        </div>
                        <h1 style={{ fontSize: 22, fontWeight: 800, letterSpacing: '-0.022em' }}>Template Manager</h1>
                    </div>
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: 13.5, marginLeft: 48 }}>
                        Set a benchmark document per category. Uploads are automatically compared for deviations.
                    </p>
                </motion.div>

                {/* Alerts */}
                {error && <div className="alert-error mb-5">{error}</div>}
                <AnimatePresence>
                    {successMsg && (
                        <motion.div
                            initial={{ opacity: 0, y: -8 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -8 }}
                            style={{
                                padding: '11px 15px', borderRadius: 10, marginBottom: 16,
                                background: 'rgba(52,211,153,0.10)', border: '1px solid rgba(52,211,153,0.25)',
                                color: '#34D399', fontSize: 13.5, fontWeight: 500,
                                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                                boxShadow: 'var(--neo-shadow-sm)',
                            }}
                        >
                            <span><CheckCircle2 size={14} style={{ display: 'inline', marginRight: 7, verticalAlign: 'middle' }} />{successMsg}</span>
                            <button onClick={() => setSuccessMsg(null)}
                                style={{ background: 'none', border: 'none', color: '#34D399', cursor: 'pointer', fontWeight: 700, fontSize: 16, lineHeight: 1, marginLeft: 12 }}>
                                ✕
                            </button>
                        </motion.div>
                    )}
                </AnimatePresence>

                {/* Neomorphic tab bar */}
                <div className="tab-bar mb-5">
                    {DOCUMENT_TYPES.map(({ type, label, emoji }) => {
                        const hasTemplate = !!templates[type];
                        return (
                            <button
                                key={type}
                                onClick={() => setActiveType(type)}
                                className={`tab-item ${activeType === type ? 'active' : ''}`}
                                style={{ display: 'flex', alignItems: 'center', gap: 6, justifyContent: 'center' }}
                            >
                                <span style={{ fontSize: 14 }}>{emoji}</span>
                                <span>{label}</span>
                                <span style={{
                                    width: 7, height: 7, borderRadius: '50%',
                                    background: hasTemplate ? '#34D399' : 'var(--color-text-disabled)',
                                    flexShrink: 0,
                                }} />
                            </button>
                        );
                    })}
                </div>

                {/* Panel */}
                <AnimatePresence mode="wait">
                    <motion.div
                        key={activeType}
                        initial={{ opacity: 0, y: 8 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -8 }}
                        transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] }}
                        className="card"
                        style={{ padding: '28px 28px', background: 'var(--color-surface)' }}
                    >
                        {/* Panel header */}
                        <div style={{ marginBottom: 20 }}>
                            <h3 style={{ fontSize: 16, fontWeight: 700, marginBottom: 4 }}>
                                {cfg?.emoji} {cfg?.singular} Standard
                            </h3>
                            <p style={{ color: 'var(--color-text-secondary)', fontSize: 13 }}>{cfg?.description}</p>
                        </div>

                        {/* Current template */}
                        {currentTemplate ? (
                            <div className="card-inset" style={{ padding: '14px 16px', marginBottom: 20 }}>
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                                        <div style={{
                                            width: 38, height: 38, borderRadius: 10, flexShrink: 0,
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                            background: 'rgba(52,211,153,0.12)', color: '#34D399',
                                            boxShadow: 'var(--neo-shadow-sm)',
                                        }}>
                                            <CheckCircle2 size={18} />
                                        </div>
                                        <div>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2 }}>
                                                <span style={{ fontSize: 13.5, fontWeight: 600, color: 'var(--color-text-primary)' }}>
                                                    {currentTemplate.fileUrl.split('/').pop()}
                                                </span>
                                                <span className="badge badge-success">Active</span>
                                            </div>
                                            <span style={{ fontSize: 11.5, color: 'var(--color-text-disabled)' }}>
                                                ID #{currentTemplate.id} · {new Date(currentTemplate.uploadedAt).toLocaleDateString()}
                                            </span>
                                        </div>
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: 7, flexShrink: 0 }}>
                                        <button
                                            onClick={() => navigate(`/documents/${currentTemplate.id}`)}
                                            className="btn-secondary"
                                            style={{ padding: '5px 12px', fontSize: 12 }}
                                        >
                                            <Eye size={12} /> View
                                        </button>
                                        <button
                                            onClick={() => handleDeleteTemplate(activeType)}
                                            disabled={actionLoading}
                                            className="btn-secondary"
                                            style={{ padding: '5px 12px', fontSize: 12, color: '#F65A5A', borderColor: 'rgba(246,90,90,0.3)' }}
                                        >
                                            <Trash2 size={12} /> Remove
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div style={{
                                padding: '12px 14px', borderRadius: 10, marginBottom: 20,
                                background: 'var(--color-warning-light)', border: '1px solid rgba(245,165,36,0.28)',
                                color: '#F5A524', fontSize: 13, display: 'flex', alignItems: 'flex-start', gap: 10,
                                boxShadow: 'var(--neo-shadow-sm)',
                            }}>
                                <AlertTriangle size={16} style={{ flexShrink: 0, marginTop: 1 }} />
                                <span>No standard set for {cfg?.label}. New uploads won't be anomaly-checked until a benchmark is chosen.</span>
                            </div>
                        )}

                        {/* Divider */}
                        <div className="divider" />

                        {/* Set / change template */}
                        <div>
                            <label className="label" style={{ marginBottom: 10 }}>
                                {currentTemplate ? `Change ${cfg?.singular} Standard` : `Choose a ${cfg?.singular} to set as standard`}
                            </label>

                            {matchingDocs.length === 0 ? (
                                <div className="card-inset" style={{ padding: '16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
                                    <p style={{ color: 'var(--color-text-secondary)', fontSize: 13 }}>
                                        Upload and process at least one {cfg?.singular.toLowerCase()} first.
                                    </p>
                                    <button className="btn-primary" style={{ padding: '7px 14px', fontSize: 13, flexShrink: 0 }}
                                        onClick={() => navigate('/upload')}>
                                        <Upload size={13} /> Upload
                                    </button>
                                </div>
                            ) : (
                                <div style={{ display: 'flex', gap: 10 }}>
                                    <select
                                        className="input flex-1"
                                        style={{ fontSize: 13 }}
                                        value={selectedDocId}
                                        onChange={(e) => setSelectedDocId(e.target.value)}
                                        disabled={actionLoading}
                                    >
                                        <option value="">— Select a processed {cfg?.singular.toLowerCase()} —</option>
                                        {matchingDocs.map(doc => (
                                            <option key={doc.id} value={doc.id}>
                                                #{doc.id} — {doc.fileUrl.split('/').pop()} ({new Date(doc.uploadedAt).toLocaleDateString()})
                                            </option>
                                        ))}
                                    </select>
                                    <button
                                        className="btn-primary"
                                        style={{ fontSize: 13, padding: '8px 18px', flexShrink: 0 }}
                                        disabled={!selectedDocId || actionLoading}
                                        onClick={() => handleSetTemplate(selectedDocId)}
                                    >
                                        {actionLoading ? 'Saving…' : 'Set Standard'}
                                    </button>
                                </div>
                            )}
                        </div>
                    </motion.div>
                </AnimatePresence>

            </main>
        </div>
    );
}
