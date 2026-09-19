import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'motion/react';
import {
    ArrowLeft, Download, ExternalLink, FileText, Sparkles,
    AlertTriangle, RotateCcw, Edit3, Check, X, ChevronDown, ChevronUp
} from 'lucide-react';
import { apiFetch, apiPatch, clearToken, downloadExport, fetchBlobUrl } from '../lib/api';
import AnomalyFlag from '../components/AnomalyFlag';
import AmbientAurora from '../components/ui/AmbientAurora';

/**
 * Document detail page — Phase 10 edition.
 * New: auto-polling for PENDING docs, human-in-the-loop field correction editor,
 * aria-labels for accessibility, mobile-responsive layout tweaks.
 */
export default function DocumentDetail() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [document, setDocument]                     = useState(null);
    const [blobUrl, setBlobUrl]                       = useState(null);
    const [extraction, setExtraction]                 = useState(null); // full extraction object
    const [fields, setFields]                         = useState(null);
    const [extractionFailedReason, setExtractionFailedReason] = useState(null);
    const [summary, setSummary]                       = useState(null);
    const [summaryFailedReason, setSummaryFailedReason]       = useState(null);
    const [anomalies, setAnomalies]                   = useState([]);
    const [loading, setLoading]                       = useState(true);
    const [error, setError]                           = useState(null);
    const [reprocessing, setReprocessing]             = useState(false);

    // Human-correction editor state
    const [editingFields, setEditingFields]           = useState(false);
    const [correctionJson, setCorrectionJson]         = useState('');
    const [correctionNote, setCorrectionNote]         = useState('');
    const [savingCorrection, setSavingCorrection]     = useState(false);
    const [correctionError, setCorrectionError]       = useState(null);
    const [correctionSaved, setCorrectionSaved]       = useState(false);

    // Polling ref
    const pollIntervalRef = useRef(null);
    const blobUrlRef      = useRef(null);

    const loadSubResources = useCallback((docId) => {
        return Promise.allSettled([
            apiFetch(`/api/documents/${docId}/extraction`),
            apiFetch(`/api/documents/${docId}/summary`),
            apiFetch(`/api/documents/${docId}/anomalies`),
        ]).then(([extractionRes, summaryRes, anomaliesRes]) => {
            if (extractionRes.status === 'fulfilled' && extractionRes.value) {
                const extr = extractionRes.value;
                setExtraction(extr);
                setExtractionFailedReason(extr.failedReason || null);
                // Prefer humanCorrectedJson if present
                const effective = extr.humanCorrectedJson || extr.fieldsJson;
                if (effective) {
                    try { setFields(JSON.parse(effective)); } catch { /* ignore */ }
                }
                setCorrectionJson(JSON.stringify(
                    JSON.parse(extr.humanCorrectedJson || extr.fieldsJson || '{}'), null, 2
                ));
            }
            if (summaryRes.status === 'fulfilled' && summaryRes.value) {
                const s = summaryRes.value;
                setSummaryFailedReason(s.failedReason || null);
                setSummary(s.summaryText || null);
            }
            if (anomaliesRes.status === 'fulfilled' && anomaliesRes.value) {
                setAnomalies(anomaliesRes.value);
            }
        });
    }, []);

    useEffect(() => {
        let active = true;

        async function load() {
            setLoading(true);
            setError(null);
            try {
                const doc = await apiFetch(`/api/documents/${id}`);
                if (!active) return;
                setDocument(doc);

                // Fetch preview blob
                fetchBlobUrl(`/api/documents/${id}/file`)
                    .then(url => {
                        if (!active) { window.URL.revokeObjectURL(url); return; }
                        blobUrlRef.current = url;
                        setBlobUrl(url);
                    })
                    .catch(err => console.warn('Preview load failed:', err));

                await loadSubResources(id);
            } catch (err) {
                if (!active) return;
                if (err.status === 401) {
                    clearToken();
                    navigate('/login', { replace: true });
                    return;
                }
                setError(err.message || 'Failed to load document.');
            } finally {
                if (active) setLoading(false);
            }
        }

        load();

        return () => {
            active = false;
            if (blobUrlRef.current) window.URL.revokeObjectURL(blobUrlRef.current);
            if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
        };
    }, [id, navigate, loadSubResources]);

    // Auto-poll every 4s while document is PENDING, stop when PROCESSED/FAILED
    useEffect(() => {
        if (!document) return;

        if (document.status === 'PENDING') {
            if (pollIntervalRef.current) return; // already polling
            pollIntervalRef.current = setInterval(async () => {
                try {
                    const refreshed = await apiFetch(`/api/documents/${id}`);
                    setDocument(refreshed);
                    if (refreshed.status !== 'PENDING') {
                        clearInterval(pollIntervalRef.current);
                        pollIntervalRef.current = null;
                        await loadSubResources(id);
                    }
                } catch { /* network blip — will retry next tick */ }
            }, 4000);
        } else {
            if (pollIntervalRef.current) {
                clearInterval(pollIntervalRef.current);
                pollIntervalRef.current = null;
            }
        }

        return () => {
            if (pollIntervalRef.current) {
                clearInterval(pollIntervalRef.current);
                pollIntervalRef.current = null;
            }
        };
    }, [document?.status, id, loadSubResources]);

    async function handleExport(format) {
        try {
            await downloadExport(`/api/documents/${id}/export?format=${format}`, `document-${id}-export.${format}`);
        } catch (err) {
            alert(`Export failed: ${err.message}`);
        }
    }

    async function handleReprocess() {
        setReprocessing(true);
        try {
            await apiFetch(`/api/documents/${id}/reprocess`, { method: 'POST' });
            const refreshed = await apiFetch(`/api/documents/${id}`);
            setDocument(refreshed);
        } catch (err) {
            alert(`Reprocess failed: ${err.message}`);
        } finally {
            setReprocessing(false);
        }
    }

    function openEditor() {
        setCorrectionJson(JSON.stringify(fields || {}, null, 2));
        setCorrectionNote(extraction?.correctionNote || '');
        setCorrectionError(null);
        setCorrectionSaved(false);
        setEditingFields(true);
    }

    async function handleSaveCorrection() {
        setSavingCorrection(true);
        setCorrectionError(null);
        try {
            JSON.parse(correctionJson); // validate JSON first
        } catch {
            setCorrectionError('Invalid JSON — please fix the syntax before saving.');
            setSavingCorrection(false);
            return;
        }
        try {
            const updated = await apiPatch(`/api/documents/${id}/extraction`, {
                correctedFieldsJson: correctionJson,
                correctionNote: correctionNote || null,
            });
            setExtraction(updated);
            setFields(JSON.parse(updated.humanCorrectedJson || updated.fieldsJson));
            setCorrectionSaved(true);
            setEditingFields(false);
        } catch (err) {
            setCorrectionError(err.message || 'Failed to save correction.');
        } finally {
            setSavingCorrection(false);
        }
    }

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center relative overflow-hidden"
                 style={{ background: 'var(--color-bg)' }}>
                <AmbientAurora opacity={0.35} />
                <div className="flex flex-col items-center gap-3 z-10">
                    <div className="w-8 h-8 rounded-full border-2 border-t-transparent animate-spin"
                         style={{ borderColor: 'var(--color-aurora-start)', borderTopColor: 'transparent' }} />
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: '14px' }}>Loading document intelligence…</p>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen relative overflow-hidden" style={{ background: 'var(--color-bg)' }}>
            <AmbientAurora opacity={0.4} />

            {/* Top Navigation */}
            <nav className="card relative z-20 flex items-center justify-between px-4 sm:px-6 py-3.5"
                 style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none', background: 'rgba(18, 16, 27, 0.85)' }}
                 aria-label="Document detail navigation">
                <div className="flex items-center gap-3">
                    <h2 className="text-xl font-bold cursor-pointer tracking-tight"
                        onClick={() => navigate('/dashboard')}>
                        <span className="text-aurora">Docket</span>
                    </h2>
                </div>
                <button
                    onClick={() => navigate('/dashboard')}
                    className="btn-secondary"
                    style={{ padding: '5px 14px', fontSize: '13px' }}
                    aria-label="Back to Dashboard"
                >
                    <ArrowLeft size={14} />
                    <span className="hidden sm:inline">Back to Dashboard</span>
                    <span className="sm:hidden">Back</span>
                </button>
            </nav>

            <main className="max-w-6xl mx-auto px-4 sm:px-6 py-8 sm:py-10 relative z-10">
                {error ? (
                    <div className="alert-error" role="alert">{error}</div>
                ) : (
                    <>
                        {/* Header */}
                        <motion.div
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ duration: 0.3 }}
                            className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8"
                        >
                            <div>
                                <div className="flex items-center gap-3 flex-wrap">
                                    <span className="font-semibold text-xs px-2.5 py-1 rounded"
                                          style={{ background: 'rgba(124, 92, 252, 0.15)', color: 'var(--color-aurora-start)', border: '1px solid rgba(124, 92, 252, 0.25)' }}>
                                        {document?.type}
                                    </span>
                                    <h1 className="text-xl sm:text-2xl font-bold break-all">{document?.fileUrl?.split('/').pop()}</h1>
                                </div>
                                <p style={{ color: 'var(--color-text-secondary)', fontSize: '13px', marginTop: '4px' }}>
                                    Document #{document?.id} • {document?.uploadedAt ? new Date(document.uploadedAt).toLocaleString() : '—'}
                                    {extraction?.correctedAt && (
                                        <span className="ml-2 text-emerald-400">✓ Human-corrected {new Date(extraction.correctedAt).toLocaleDateString()}</span>
                                    )}
                                </p>
                            </div>
                            <div className="flex items-center gap-2 flex-wrap">
                                {(document?.status === 'FAILED' || document?.status === 'PENDING') && (
                                    <button
                                        onClick={handleReprocess}
                                        disabled={reprocessing}
                                        className="btn-secondary flex items-center gap-1.5 text-amber-300 hover:text-amber-200"
                                        style={{ padding: '5px 12px', fontSize: '12px' }}
                                        aria-label="Trigger document reprocessing"
                                    >
                                        <RotateCcw size={13} className={reprocessing ? 'animate-spin' : ''} />
                                        {reprocessing ? 'Reprocessing…' : 'Reprocess'}
                                    </button>
                                )}
                                <button
                                    onClick={() => handleExport('csv')}
                                    className="btn-secondary"
                                    style={{ padding: '5px 12px', fontSize: '12px' }}
                                    aria-label="Export document as CSV"
                                >
                                    <Download size={13} />
                                    CSV
                                </button>
                                <button
                                    onClick={() => handleExport('json')}
                                    className="btn-secondary"
                                    style={{ padding: '5px 12px', fontSize: '12px' }}
                                    aria-label="Export document as JSON"
                                >
                                    <Download size={13} />
                                    JSON
                                </button>
                                <span className={`badge ${
                                    document?.status === 'PENDING'   ? 'badge-warning badge-pulse'
                                    : document?.status === 'PROCESSED' ? 'badge-success'
                                    : 'badge-danger'
                                }`} aria-live="polite">
                                    {document?.status}
                                    {document?.status === 'PENDING' && ' · auto-refreshing'}
                                </span>
                            </div>
                        </motion.div>

                        <div className="grid gap-6 lg:grid-cols-2">
                            {/* File preview */}
                            <motion.div
                                initial={{ opacity: 0, x: -10 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ duration: 0.3, delay: 0.1 }}
                                className="card overflow-hidden"
                                style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                            >
                                <div className="px-4 py-3 flex items-center justify-between"
                                     style={{ borderBottom: '1px solid var(--color-border)', background: 'var(--color-surface-raised)' }}>
                                    <h3 className="text-sm font-semibold text-gray-300">Original File Preview</h3>
                                    {blobUrl && (
                                        <a href={blobUrl} target="_blank" rel="noreferrer"
                                           className="text-xs font-semibold flex items-center gap-1 hover:text-cyan-300"
                                           aria-label="Open file in new tab">
                                            <span>Open in new tab</span>
                                            <ExternalLink size={12} />
                                        </a>
                                    )}
                                </div>
                                <div style={{ height: '520px', background: '#0D0B14' }}>
                                    {blobUrl ? (
                                        <iframe
                                            title="Document preview"
                                            src={blobUrl}
                                            style={{ width: '100%', height: '100%', border: 'none' }}
                                        />
                                    ) : (
                                        <div className="h-full flex items-center justify-center text-sm text-gray-500">
                                            {document?.status === 'PENDING' ? 'Processing…' : 'Preview unavailable'}
                                        </div>
                                    )}
                                </div>
                            </motion.div>

                            {/* Extracted fields + human correction */}
                            <motion.div
                                initial={{ opacity: 0, x: 10 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ duration: 0.3, delay: 0.1 }}
                                className="card overflow-hidden"
                                style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                            >
                                <div className="px-4 py-3 flex items-center justify-between"
                                     style={{ borderBottom: '1px solid var(--color-border)', background: 'var(--color-surface-raised)' }}>
                                    <div className="flex items-center gap-2">
                                        <Sparkles size={15} className="text-purple-400" />
                                        <h3 className="text-sm font-semibold text-gray-300">Extracted Intelligence</h3>
                                        {extraction?.humanCorrectedJson ? (
                                            <span className="text-[10px] px-1.5 py-0.5 rounded font-semibold"
                                                  style={{ background: 'rgba(52,211,153,0.15)', color: '#34d399' }}>
                                                HUMAN CORRECTED
                                            </span>
                                        ) : fields?.fieldConfidences && Object.keys(fields.fieldConfidences).length > 0 ? (
                                            (() => {
                                                const vals = Object.values(fields.fieldConfidences).filter(v => typeof v === 'number');
                                                if (vals.length === 0) return null;
                                                const avg = Math.round((vals.reduce((a, b) => a + b, 0) / vals.length) * 100);
                                                const color = avg >= 85 ? '#34d399' : avg >= 70 ? '#fbbf24' : '#f87171';
                                                const bg = avg >= 85 ? 'rgba(52,211,153,0.12)' : avg >= 70 ? 'rgba(251,191,36,0.12)' : 'rgba(248,113,113,0.12)';
                                                return (
                                                    <span className="text-[10px] px-2 py-0.5 rounded-full font-semibold flex items-center gap-1"
                                                          style={{ background: bg, color: color, border: `1px solid ${color}33` }}
                                                          title="Average AI field extraction confidence score">
                                                        <span className="w-1.5 h-1.5 rounded-full" style={{ background: color }} />
                                                        {avg}% avg conf
                                                    </span>
                                                );
                                            })()
                                        ) : null}
                                    </div>
                                    {fields && !editingFields && (
                                        <button
                                            onClick={openEditor}
                                            className="btn-ghost flex items-center gap-1 text-xs"
                                            style={{ padding: '3px 8px' }}
                                            aria-label="Edit extracted fields"
                                        >
                                            <Edit3 size={12} />
                                            Edit
                                        </button>
                                    )}
                                </div>

                                {document?.status === 'PENDING' ? (
                                    <div className="p-8 text-center" style={{ color: 'var(--color-text-secondary)' }}>
                                        <div className="w-6 h-6 border-2 border-cyan-400 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
                                        <p className="text-sm">Processing — auto-refreshing every 4 s…</p>
                                    </div>
                                ) : extractionFailedReason ? (
                                    <div className="p-6">
                                        <span className="badge badge-danger">Extraction failed</span>
                                        <p style={{ color: 'var(--color-text-secondary)', marginTop: '10px', fontSize: '14px' }}>
                                            {extractionFailedReason}
                                        </p>
                                    </div>
                                ) : !fields ? (
                                    <div className="p-8 text-center" style={{ color: 'var(--color-text-secondary)' }}>
                                        <p className="text-sm">No structured data extracted for this document.</p>
                                    </div>
                                ) : (
                                    <div className="p-5 max-h-[520px] overflow-y-auto">
                                        {/* Anomaly flags */}
                                        {anomalies && anomalies.length > 0 && (
                                            <div className="mb-6">
                                                <h4 className="text-xs font-bold uppercase tracking-wider text-amber-400 mb-2 flex items-center gap-1.5">
                                                    <AlertTriangle size={13} aria-hidden="true" />
                                                    Detected Anomalies & Deviations ({anomalies.length})
                                                </h4>
                                                {anomalies.map(flag => (
                                                    <AnomalyFlag key={flag.id} flag={flag} />
                                                ))}
                                            </div>
                                        )}

                                        {/* Correction saved banner */}
                                        <AnimatePresence>
                                            {correctionSaved && (
                                                <motion.div
                                                    initial={{ opacity: 0, y: -6 }}
                                                    animate={{ opacity: 1, y: 0 }}
                                                    exit={{ opacity: 0 }}
                                                    className="mb-4 px-3 py-2 rounded-lg text-xs font-semibold flex items-center gap-2"
                                                    style={{ background: 'rgba(52,211,153,0.12)', color: '#34d399', border: '1px solid rgba(52,211,153,0.25)' }}
                                                    role="status"
                                                >
                                                    <Check size={13} />
                                                    Correction saved. AI extraction preserved in audit trail.
                                                </motion.div>
                                            )}
                                        </AnimatePresence>

                                        {/* Inline correction editor */}
                                        <AnimatePresence>
                                            {editingFields && (
                                                <motion.div
                                                    initial={{ opacity: 0, height: 0 }}
                                                    animate={{ opacity: 1, height: 'auto' }}
                                                    exit={{ opacity: 0, height: 0 }}
                                                    className="mb-5 overflow-hidden"
                                                >
                                                    <div className="rounded-lg border p-4"
                                                         style={{ background: 'rgba(0,0,0,0.3)', borderColor: 'rgba(124,92,252,0.35)' }}>
                                                        <p className="text-xs text-gray-400 mb-2">
                                                            Edit the JSON below to correct any AI extraction errors.
                                                            The original AI output is preserved for auditing.
                                                        </p>
                                                        <textarea
                                                            value={correctionJson}
                                                            onChange={e => setCorrectionJson(e.target.value)}
                                                            className="w-full font-mono text-xs rounded p-2 resize-y"
                                                            style={{
                                                                minHeight: '160px',
                                                                background: 'rgba(0,0,0,0.5)',
                                                                color: '#e2e8f0',
                                                                border: '1px solid var(--color-border)',
                                                                outline: 'none',
                                                            }}
                                                            aria-label="Corrected extraction JSON"
                                                            spellCheck="false"
                                                        />
                                                        <input
                                                            type="text"
                                                            value={correctionNote}
                                                            onChange={e => setCorrectionNote(e.target.value)}
                                                            placeholder="Optional: reason for correction (e.g. 'Total was truncated by OCR')"
                                                            className="w-full text-xs rounded px-2 py-1.5 mt-2"
                                                            style={{
                                                                background: 'rgba(0,0,0,0.4)',
                                                                color: '#e2e8f0',
                                                                border: '1px solid var(--color-border)',
                                                                outline: 'none',
                                                            }}
                                                            aria-label="Correction note"
                                                            maxLength={1000}
                                                        />
                                                        {correctionError && (
                                                            <p className="text-red-400 text-xs mt-2" role="alert">{correctionError}</p>
                                                        )}
                                                        <div className="flex gap-2 mt-3">
                                                            <button
                                                                onClick={handleSaveCorrection}
                                                                disabled={savingCorrection}
                                                                className="btn-primary flex items-center gap-1.5 text-xs"
                                                                style={{ padding: '5px 14px' }}
                                                                aria-label="Save field correction"
                                                            >
                                                                <Check size={13} />
                                                                {savingCorrection ? 'Saving…' : 'Save Correction'}
                                                            </button>
                                                            <button
                                                                onClick={() => setEditingFields(false)}
                                                                className="btn-secondary flex items-center gap-1.5 text-xs"
                                                                style={{ padding: '5px 12px' }}
                                                                aria-label="Cancel field editing"
                                                            >
                                                                <X size={13} />
                                                                Cancel
                                                            </button>
                                                        </div>
                                                    </div>
                                                </motion.div>
                                            )}
                                        </AnimatePresence>

                                        <ExtractionFields type={document?.type} fields={fields} />
                                    </div>
                                )}
                            </motion.div>

                            {/* Summary */}
                            <motion.div
                                initial={{ opacity: 0, y: 10 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ duration: 0.3, delay: 0.2 }}
                                className="card overflow-hidden lg:col-span-2"
                                style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                            >
                                <div className="px-4 py-3 flex items-center gap-2"
                                     style={{ borderBottom: '1px solid var(--color-border)', background: 'var(--color-surface-raised)' }}>
                                    <FileText size={15} className="text-cyan-400" aria-hidden="true" />
                                    <h3 className="text-sm font-semibold text-gray-300">Plain-English Summary</h3>
                                </div>
                                {document?.status === 'PENDING' ? (
                                    <div className="p-6" style={{ color: 'var(--color-text-secondary)' }}>
                                        Summarization running — will appear automatically when ready.
                                    </div>
                                ) : summaryFailedReason ? (
                                    <div className="p-6">
                                        <span className="badge badge-danger">Summarization failed</span>
                                        <p style={{ color: 'var(--color-text-secondary)', marginTop: '8px', fontSize: '14px' }}>
                                            {summaryFailedReason}
                                        </p>
                                    </div>
                                ) : !summary ? (
                                    <div className="p-6" style={{ color: 'var(--color-text-secondary)', fontSize: '14px' }}>
                                        No summary generated for this document.
                                    </div>
                                ) : (
                                    <div className="p-6 text-sm" style={{ lineHeight: '1.65', color: 'var(--color-text-primary)' }}>
                                        {summary}
                                    </div>
                                )}
                            </motion.div>
                        </div>
                    </>
                )}
            </main>
        </div>
    );
}

/** Dispatches to the correct field renderer based on document type. */
function ExtractionFields({ type, fields }) {
    if (type === 'INVOICE')  return <InvoiceFields fields={fields} />;
    if (type === 'CONTRACT') return <ContractFields fields={fields} />;
    if (type === 'RESUME')   return <ResumeFields fields={fields} />;
    return <pre className="text-xs p-3 rounded bg-black/40 text-gray-300" style={{ overflowX: 'auto' }}>{JSON.stringify(fields, null, 2)}</pre>;
}

function InvoiceFields({ fields }) {
    const conf = fields.fieldConfidences || {};
    return (
        <>
            <dl className="grid gap-3 mb-6 grid-cols-2">
                <Field label="Vendor"        value={fields.vendorName}   confidence={conf.vendorName} />
                <Field label="Invoice #"     value={fields.invoiceNumber} confidence={conf.invoiceNumber} />
                <Field label="Invoice Date"  value={fields.invoiceDate}   confidence={conf.invoiceDate} />
                <Field label="Due Date"      value={fields.dueDate}       confidence={conf.dueDate} />
                <div className="col-span-2">
                    <Field label="Total Amount" value={fields.totalAmount} confidence={conf.totalAmount} bold />
                </div>
            </dl>

            {Array.isArray(fields.lineItems) && fields.lineItems.length > 0 && (
                <div className="mt-4">
                    <div className="flex items-center justify-between mb-2">
                        <SectionHeader>Line Items</SectionHeader>
                        {typeof conf.lineItems === 'number' && (
                            <ConfidenceBadge score={conf.lineItems} />
                        )}
                    </div>
                    <div className="rounded-lg overflow-hidden border" style={{ borderColor: 'var(--color-border)' }}>
                        <table className="w-full text-left border-collapse text-xs" aria-label="Invoice line items">
                            <thead>
                                <tr style={{ borderBottom: '1px solid var(--color-border)', backgroundColor: 'var(--color-surface-raised)' }}>
                                    <th className="py-2.5 px-3 font-semibold text-gray-400">Description</th>
                                    <th className="py-2.5 px-3 font-semibold text-gray-400">Qty</th>
                                    <th className="py-2.5 px-3 font-semibold text-gray-400">Unit Price</th>
                                    <th className="py-2.5 px-3 font-semibold text-right text-gray-400">Amount</th>
                                </tr>
                            </thead>
                            <tbody>
                                {fields.lineItems.map((item, idx) => (
                                    <tr key={idx} style={{ borderBottom: '1px solid var(--color-border)' }} className="hover:bg-purple-950/20">
                                        <td className="py-2 px-3 text-white font-medium">{item.description}</td>
                                        <td className="py-2 px-3 text-gray-300">{item.quantity  || '—'}</td>
                                        <td className="py-2 px-3 text-gray-300">{item.unitPrice || '—'}</td>
                                        <td className="py-2 px-3 text-right font-semibold text-emerald-400">{item.amount || '—'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </>
    );
}

function ContractFields({ fields }) {
    const conf = fields.fieldConfidences || {};
    return (
        <>
            <dl className="grid gap-3 mb-6 grid-cols-2">
                <div className="col-span-2">
                    <Field label="Contract Title" value={fields.contractTitle} confidence={conf.contractTitle} bold />
                </div>
                <Field label="Effective Date"  value={fields.effectiveDate}  confidence={conf.effectiveDate} />
                <Field label="Term / Duration" value={fields.termOrDuration} confidence={conf.termOrDuration} />
                <Field label="Governing Law"   value={fields.governingLaw}   confidence={conf.governingLaw} />
                <Field label="Total Value"     value={fields.totalValue}     confidence={conf.totalValue} />
            </dl>

            {Array.isArray(fields.parties) && fields.parties.length > 0 && (
                <div className="mb-4">
                    <div className="flex items-center justify-between mb-2">
                        <SectionHeader>Involved Parties</SectionHeader>
                        {typeof conf.parties === 'number' && (
                            <ConfidenceBadge score={conf.parties} />
                        )}
                    </div>
                    <ul className="text-sm space-y-1 pl-4 list-disc text-gray-300">
                        {fields.parties.map((p, i) => <li key={i}>{p}</li>)}
                    </ul>
                </div>
            )}
        </>
    );
}

function ResumeFields({ fields }) {
    const conf = fields.fieldConfidences || {};
    return (
        <>
            <dl className="grid gap-3 mb-6 grid-cols-2">
                <div className="col-span-2">
                    <Field label="Candidate Name" value={fields.candidateName} confidence={conf.candidateName} bold />
                </div>
                <Field label="Email"     value={fields.email} confidence={conf.email} />
                <Field label="Phone"     value={fields.phone} confidence={conf.phone} />
                <div className="col-span-2">
                    <Field label="Education" value={fields.education} confidence={conf.education} />
                </div>
            </dl>

            {Array.isArray(fields.skills) && fields.skills.length > 0 && (
                <div className="mb-5">
                    <div className="flex items-center justify-between mb-2">
                        <SectionHeader>Extracted Skills</SectionHeader>
                        {typeof conf.skills === 'number' && (
                            <ConfidenceBadge score={conf.skills} />
                        )}
                    </div>
                    <div className="flex flex-wrap gap-1.5 mt-2">
                        {fields.skills.map((s, i) => (
                            <span key={i} className="badge badge-info" style={{ fontSize: '11px' }}>{s}</span>
                        ))}
                    </div>
                </div>
            )}

            {Array.isArray(fields.experience) && fields.experience.length > 0 && (
                <div>
                    <div className="flex items-center justify-between mb-2">
                        <SectionHeader>Experience History</SectionHeader>
                        {typeof conf.experience === 'number' && (
                            <ConfidenceBadge score={conf.experience} />
                        )}
                    </div>
                    <div className="rounded-lg overflow-hidden border mt-2" style={{ borderColor: 'var(--color-border)' }}>
                        <table className="w-full text-left border-collapse text-xs" aria-label="Work experience history">
                            <thead>
                                <tr style={{ borderBottom: '1px solid var(--color-border)', backgroundColor: 'var(--color-surface-raised)' }}>
                                    <th className="py-2.5 px-3 font-semibold text-gray-400">Company</th>
                                    <th className="py-2.5 px-3 font-semibold text-gray-400">Role</th>
                                    <th className="py-2.5 px-3 font-semibold text-gray-400">Duration</th>
                                </tr>
                            </thead>
                            <tbody>
                                {fields.experience.map((exp, idx) => (
                                    <tr key={idx} style={{ borderBottom: '1px solid var(--color-border)' }} className="hover:bg-purple-950/20">
                                        <td className="py-2 px-3 text-white font-medium">{exp.company  || '—'}</td>
                                        <td className="py-2 px-3 text-cyan-300">{exp.role     || '—'}</td>
                                        <td className="py-2 px-3 text-gray-400">{exp.duration || '—'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </>
    );
}

function ConfidenceBadge({ score }) {
    if (typeof score !== 'number') return null;
    const pct = Math.round(score * 100);
    const color = pct >= 85 ? '#34d399' : pct >= 70 ? '#fbbf24' : '#f87171';
    const bg = pct >= 85 ? 'rgba(52,211,153,0.12)' : pct >= 70 ? 'rgba(251,191,36,0.12)' : 'rgba(248,113,113,0.12)';
    return (
        <span
            className="text-[10px] px-1.5 py-0.5 rounded font-mono font-medium flex items-center gap-1"
            style={{ background: bg, color: color, border: `1px solid ${color}33` }}
            title={`Extraction confidence: ${pct}%`}
        >
            <span className="w-1 h-1 rounded-full" style={{ background: color }} />
            {pct}%
        </span>
    );
}

function SectionHeader({ children }) {
    return (
        <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider">
            {children}
        </h4>
    );
}

function Field({ label, value, confidence, bold }) {
    return (
        <div className="p-3 rounded-lg border" style={{ background: 'var(--color-surface-raised)', borderColor: 'var(--color-border)' }}>
            <div className="flex items-center justify-between mb-1">
                <dt className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider">
                    {label}
                </dt>
                {typeof confidence === 'number' && (
                    <ConfidenceBadge score={confidence} />
                )}
            </div>
            <dd className={`text-white ${bold ? 'text-lg font-bold text-cyan-300' : 'text-sm font-normal'}`}>
                {value || '—'}
            </dd>
        </div>
    );
}
