import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion } from 'motion/react';
import { ArrowLeft, Download, ExternalLink, FileText, Sparkles, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { apiFetch, clearToken, downloadExport } from '../lib/api';
import AnomalyFlag from '../components/AnomalyFlag';
import AmbientAurora from '../components/ui/AmbientAurora';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Document detail page — "Aurora Obsidian" edition.
 * Displays Gemini structured extraction, summaries, and deviation alerts
 * next to original document preview with full CSV/JSON export actions.
 */
export default function DocumentDetail() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [document, setDocument] = useState(null);
    const [fields, setFields] = useState(null);
    const [extractionFailedReason, setExtractionFailedReason] = useState(null);
    const [summary, setSummary] = useState(null);
    const [summaryFailedReason, setSummaryFailedReason] = useState(null);
    const [anomalies, setAnomalies] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        setLoading(true);
        setError(null);

        Promise.all([
            apiFetch('/api/documents'),
            apiFetch(`/api/documents/${id}/extraction`),
            apiFetch(`/api/documents/${id}/summary`),
            apiFetch(`/api/documents/${id}/anomalies`),
        ])
            .then(([documents, extraction, summaryResp, anomaliesResp]) => {
                const doc = documents.find(d => String(d.id) === String(id));
                if (!doc) {
                    setError('Document not found.');
                    return;
                }
                setDocument(doc);

                if (extraction) {
                    setExtractionFailedReason(extraction.failedReason || null);
                    if (extraction.fieldsJson) {
                        try {
                            setFields(JSON.parse(extraction.fieldsJson));
                        } catch {
                            setError('Extracted data could not be read.');
                        }
                    }
                }

                if (summaryResp) {
                    setSummaryFailedReason(summaryResp.failedReason || null);
                    setSummary(summaryResp.summaryText || null);
                }

                if (anomaliesResp) {
                    setAnomalies(anomaliesResp);
                }
            })
            .catch((err) => {
                if (err.status === 401) {
                    clearToken();
                    navigate('/login', { replace: true });
                    return;
                }
                setError(err.message || 'Failed to load document.');
            })
            .finally(() => setLoading(false));
    }, [id, navigate]);

    async function handleExport(format) {
        try {
            await downloadExport(`/api/documents/${id}/export?format=${format}`, `document-${id}-export.${format}`);
        } catch (err) {
            alert(`Export failed: ${err.message}`);
        }
    }

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center relative overflow-hidden"
                 style={{ background: 'var(--color-bg)' }}>
                <AmbientAurora opacity={0.35} />
                <div className="flex flex-col items-center gap-3 z-10">
                    <div className="w-8 h-8 rounded-full border-2 border-t-transparent animate-spin" style={{ borderColor: 'var(--color-aurora-start)', borderTopColor: 'transparent' }} />
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: '14px' }}>Loading document intelligence…</p>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen relative overflow-hidden" style={{ background: 'var(--color-bg)' }}>
            <AmbientAurora opacity={0.4} />

            {/* Top Navigation */}
            <nav className="card relative z-20 flex items-center justify-between px-6 py-3.5"
                 style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none', background: 'rgba(18, 16, 27, 0.85)' }}>
                <div className="flex items-center gap-3">
                    <h2 className="text-xl font-bold cursor-pointer tracking-tight"
                        onClick={() => navigate('/dashboard')}>
                        <span className="text-aurora">Docket</span>
                    </h2>
                </div>
                <button onClick={() => navigate('/dashboard')} className="btn-secondary"
                        style={{ padding: '5px 14px', fontSize: '13px' }}>
                    <ArrowLeft size={14} />
                    Back to Dashboard
                </button>
            </nav>

            <main className="max-w-6xl mx-auto px-6 py-10 relative z-10">
                {error ? (
                    <div className="alert-error">{error}</div>
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
                                <div className="flex items-center gap-3">
                                    <span className="font-semibold text-xs px-2.5 py-1 rounded"
                                          style={{ background: 'rgba(124, 92, 252, 0.15)', color: 'var(--color-aurora-start)', border: '1px solid rgba(124, 92, 252, 0.25)' }}>
                                        {document?.type}
                                    </span>
                                    <h1 className="text-2xl font-bold">{document?.fileUrl?.split('/').pop()}</h1>
                                </div>
                                <p style={{ color: 'var(--color-text-secondary)', fontSize: '13px', marginTop: '4px' }}>
                                    Document #{document?.id} • Uploaded {document?.uploadedAt ? new Date(document.uploadedAt).toLocaleString() : '—'}
                                </p>
                            </div>
                            <div className="flex items-center gap-2.5">
                                <button
                                    onClick={() => handleExport('csv')}
                                    className="btn-secondary"
                                    style={{ padding: '5px 12px', fontSize: '12px' }}
                                    title="Export this document as CSV"
                                >
                                    <Download size={13} />
                                    CSV
                                </button>
                                <button
                                    onClick={() => handleExport('json')}
                                    className="btn-secondary"
                                    style={{ padding: '5px 12px', fontSize: '12px' }}
                                    title="Export this document as JSON"
                                >
                                    <Download size={13} />
                                    JSON
                                </button>
                                <span className={`badge ${
                                    document?.status === 'PENDING' ? 'badge-warning badge-pulse'
                                        : document?.status === 'PROCESSED' ? 'badge-success'
                                        : 'badge-danger'
                                }`}>
                                    {document?.status}
                                </span>
                            </div>
                        </motion.div>

                        <div className="grid gap-6 md:grid-cols-2">
                            {/* File preview */}
                            <motion.div
                                initial={{ opacity: 0, x: -10 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ duration: 0.3, delay: 0.1 }}
                                className="card overflow-hidden"
                                style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                            >
                                <div className="px-4 py-3 flex items-center justify-between" style={{ borderBottom: '1px solid var(--color-border)', background: 'var(--color-surface-raised)' }}>
                                    <h3 className="text-sm font-semibold text-gray-300">Original File Preview</h3>
                                    {document?.fileUrl && (
                                        <a href={API_BASE_URL + document.fileUrl} target="_blank" rel="noreferrer"
                                           className="text-xs font-semibold flex items-center gap-1 hover:text-cyan-300">
                                            <span>Open tab</span>
                                            <ExternalLink size={12} />
                                        </a>
                                    )}
                                </div>
                                <div style={{ height: '580px', background: '#0D0B14' }}>
                                    {document?.fileUrl && (
                                        <iframe
                                            title="Document preview"
                                            src={API_BASE_URL + document.fileUrl}
                                            style={{ width: '100%', height: '100%', border: 'none' }}
                                        />
                                    )}
                                </div>
                            </motion.div>

                            {/* Extracted fields */}
                            <motion.div
                                initial={{ opacity: 0, x: 10 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ duration: 0.3, delay: 0.1 }}
                                className="card overflow-hidden"
                                style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                            >
                                <div className="px-4 py-3 flex items-center gap-2" style={{ borderBottom: '1px solid var(--color-border)', background: 'var(--color-surface-raised)' }}>
                                    <Sparkles size={15} className="text-purple-400" />
                                    <h3 className="text-sm font-semibold text-gray-300">Extracted Intelligence</h3>
                                </div>

                                {document?.status === 'PENDING' ? (
                                    <div className="p-8 text-center" style={{ color: 'var(--color-text-secondary)' }}>
                                        <div className="w-6 h-6 border-2 border-cyan-400 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
                                        <p className="text-sm">Still processing this document — extraction in progress…</p>
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
                                    <div className="p-5 max-h-[580px] overflow-y-auto">
                                        {anomalies && anomalies.length > 0 && (
                                            <div className="mb-6">
                                                <h4 className="text-xs font-bold uppercase tracking-wider text-amber-400 mb-2 flex items-center gap-1.5">
                                                    <AlertTriangle size={13} />
                                                    Detected Template Deviations ({anomalies.length})
                                                </h4>
                                                {anomalies.map(flag => (
                                                    <AnomalyFlag key={flag.id} flag={flag} />
                                                ))}
                                            </div>
                                        )}
                                        <ExtractionFields type={document?.type} fields={fields} />
                                    </div>
                                )}
                            </motion.div>

                            {/* Summary */}
                            <motion.div
                                initial={{ opacity: 0, y: 10 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ duration: 0.3, delay: 0.2 }}
                                className="card overflow-hidden md:col-span-2"
                                style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                            >
                                <div className="px-4 py-3 flex items-center gap-2" style={{ borderBottom: '1px solid var(--color-border)', background: 'var(--color-surface-raised)' }}>
                                    <FileText size={15} className="text-cyan-400" />
                                    <h3 className="text-sm font-semibold text-gray-300">Plain-English Summary</h3>
                                </div>
                                {document?.status === 'PENDING' ? (
                                    <div className="p-6" style={{ color: 'var(--color-text-secondary)' }}>
                                        Summarization running in background…
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
    if (type === 'INVOICE') return <InvoiceFields fields={fields} />;
    if (type === 'CONTRACT') return <ContractFields fields={fields} />;
    if (type === 'RESUME') return <ResumeFields fields={fields} />;
    return <pre className="text-xs p-3 rounded bg-black/40 text-gray-300" style={{ overflowX: 'auto' }}>{JSON.stringify(fields, null, 2)}</pre>;
}

function InvoiceFields({ fields }) {
    return (
        <>
            <dl className="grid gap-3 mb-6 grid-cols-2">
                <Field label="Vendor" value={fields.vendorName} />
                <Field label="Invoice #" value={fields.invoiceNumber} />
                <Field label="Invoice Date" value={fields.invoiceDate} />
                <Field label="Due Date" value={fields.dueDate} />
                <div className="col-span-2">
                    <Field label="Total Amount" value={fields.totalAmount} bold />
                </div>
            </dl>

            {Array.isArray(fields.lineItems) && fields.lineItems.length > 0 && (
                <div className="mt-4">
                    <SectionHeader>Line Items</SectionHeader>
                    <div className="rounded-lg overflow-hidden border" style={{ borderColor: 'var(--color-border)' }}>
                        <table className="w-full text-left border-collapse text-xs">
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
                                        <td className="py-2 px-3 text-gray-300">{item.quantity || '—'}</td>
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
    return (
        <>
            <dl className="grid gap-3 mb-6 grid-cols-2">
                <div className="col-span-2">
                    <Field label="Contract Title" value={fields.contractTitle} bold />
                </div>
                <Field label="Effective Date" value={fields.effectiveDate} />
                <Field label="Term / Duration" value={fields.termOrDuration} />
                <Field label="Governing Law" value={fields.governingLaw} />
                <Field label="Total Value" value={fields.totalValue} />
            </dl>

            {Array.isArray(fields.parties) && fields.parties.length > 0 && (
                <div className="mb-4">
                    <SectionHeader>Involved Parties</SectionHeader>
                    <ul className="text-sm space-y-1 pl-4 list-disc text-gray-300">
                        {fields.parties.map((p, i) => <li key={i}>{p}</li>)}
                    </ul>
                </div>
            )}
        </>
    );
}

function ResumeFields({ fields }) {
    return (
        <>
            <dl className="grid gap-3 mb-6 grid-cols-2">
                <div className="col-span-2">
                    <Field label="Candidate Name" value={fields.candidateName} bold />
                </div>
                <Field label="Email" value={fields.email} />
                <Field label="Phone" value={fields.phone} />
                <div className="col-span-2">
                    <Field label="Education" value={fields.education} />
                </div>
            </dl>

            {Array.isArray(fields.skills) && fields.skills.length > 0 && (
                <div className="mb-5">
                    <SectionHeader>Extracted Skills</SectionHeader>
                    <div className="flex flex-wrap gap-1.5 mt-2">
                        {fields.skills.map((s, i) => (
                            <span key={i} className="badge badge-info" style={{ fontSize: '11px' }}>{s}</span>
                        ))}
                    </div>
                </div>
            )}

            {Array.isArray(fields.experience) && fields.experience.length > 0 && (
                <div>
                    <SectionHeader>Experience History</SectionHeader>
                    <div className="rounded-lg overflow-hidden border mt-2" style={{ borderColor: 'var(--color-border)' }}>
                        <table className="w-full text-left border-collapse text-xs">
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
                                        <td className="py-2 px-3 text-white font-medium">{exp.company || '—'}</td>
                                        <td className="py-2 px-3 text-cyan-300">{exp.role || '—'}</td>
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

function SectionHeader({ children }) {
    return (
        <h4 className="mb-2 text-xs font-bold text-gray-400 uppercase tracking-wider">
            {children}
        </h4>
    );
}

function Field({ label, value, bold }) {
    return (
        <div className="p-3 rounded-lg border" style={{ background: 'var(--color-surface-raised)', borderColor: 'var(--color-border)' }}>
            <dt className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider mb-1">
                {label}
            </dt>
            <dd className={`text-white ${bold ? 'text-lg font-bold text-cyan-300' : 'text-sm font-normal'}`}>
                {value || '—'}
            </dd>
        </div>
    );
}
