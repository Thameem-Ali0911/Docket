import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { apiFetch, clearToken } from '../lib/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Document detail page — shows the Gemini-extracted invoice fields
 * (vendor, invoice #, dates, total, line items) next to a preview
 * of the original uploaded file.
 */
export default function DocumentDetail() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [document, setDocument] = useState(null);
    const [fields, setFields] = useState(null);
    const [extractionFailedReason, setExtractionFailedReason] = useState(null);
    const [summary, setSummary] = useState(null);
    const [summaryFailedReason, setSummaryFailedReason] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        setLoading(true);
        setError(null);

        Promise.all([
            apiFetch('/api/documents'),
            apiFetch(`/api/documents/${id}/extraction`),
            apiFetch(`/api/documents/${id}/summary`),
        ])
            .then(([documents, extraction, summaryResp]) => {
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
            })
            .catch((err) => {
                if (err.status === 401) {
                    clearToken();
                    navigate('/login', { replace: true });
                    return;
                }
                // Any other failure (transient 500, backend momentarily busy, network
                // blip) shouldn't wipe a valid login — surface it instead.
                setError(err.message || 'Failed to load document.');
            })
            .finally(() => setLoading(false));
    }, [id, navigate]);

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center"
                 style={{ background: 'var(--color-bg)' }}>
                <p style={{ color: 'var(--color-text-secondary)' }}>Loading…</p>
            </div>
        );
    }

    return (
        <div className="min-h-screen" style={{ background: 'var(--color-bg)' }}>
            <nav className="card flex items-center justify-between px-6 py-3"
                 style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none' }}>
                <div className="flex items-center gap-3">
                    <h2 className="text-xl font-bold"
                        style={{ fontFamily: 'var(--font-heading)', color: 'var(--color-primary)' }}>
                        Docket
                    </h2>
                </div>
                <button onClick={() => navigate('/dashboard')} className="btn-secondary"
                        style={{ padding: '6px 14px', fontSize: '13px' }}>
                    ← Back to dashboard
                </button>
            </nav>

            <main className="max-w-6xl mx-auto px-6 py-12">
                {error ? (
                    <div className="alert-error">{error}</div>
                ) : (
                    <>
                        <div className="flex items-center justify-between mb-8">
                            <div>
                                <h1>{document?.type} — {document?.fileUrl?.split('/').pop()}</h1>
                                <p style={{ color: 'var(--color-text-secondary)', fontSize: '14px', marginTop: '4px' }}>
                                    Uploaded {document?.uploadedAt ? new Date(document.uploadedAt).toLocaleString() : '—'}
                                </p>
                            </div>
                            <span className={`badge ${
                                document?.status === 'PENDING' ? 'badge-warning'
                                    : document?.status === 'PROCESSED' ? 'badge-success'
                                    : 'badge-danger'
                            }`}>
                                {document?.status}
                            </span>
                        </div>

                        <div className="grid gap-6" style={{ gridTemplateColumns: '1fr 1fr' }}>
                            {/* File preview */}
                            <div className="card overflow-hidden">
                                <div className="px-4 py-3" style={{ borderBottom: '1px solid var(--color-border)' }}>
                                    <h3>Original file</h3>
                                </div>
                                <div style={{ height: '600px', background: '#F9FAFB' }}>
                                    {document?.fileUrl && (
                                        <iframe
                                            title="Document preview"
                                            src={API_BASE_URL + document.fileUrl}
                                            style={{ width: '100%', height: '100%', border: 'none' }}
                                        />
                                    )}
                                </div>
                                <div className="px-4 py-3" style={{ borderTop: '1px solid var(--color-border)' }}>
                                    <a href={API_BASE_URL + document?.fileUrl} target="_blank" rel="noreferrer"
                                       className="text-sm font-medium hover:underline"
                                       style={{ color: 'var(--color-primary)' }}>
                                        Open original in new tab
                                    </a>
                                </div>
                            </div>

                            {/* Extracted fields */}
                            <div className="card overflow-hidden">
                                <div className="px-4 py-3" style={{ borderBottom: '1px solid var(--color-border)' }}>
                                    <h3>Extracted fields</h3>
                                </div>

                                {document?.status === 'PENDING' ? (
                                    <div className="p-6" style={{ color: 'var(--color-text-secondary)' }}>
                                        Still processing this document — extraction hasn't run yet.
                                    </div>
                                ) : extractionFailedReason ? (
                                    <div className="p-6">
                                        <span className="badge badge-danger">Extraction failed</span>
                                        <p style={{ color: 'var(--color-text-secondary)', marginTop: '10px', fontSize: '14px' }}>
                                            {extractionFailedReason}
                                        </p>
                                    </div>
                                ) : !fields ? (
                                    <div className="p-6" style={{ color: 'var(--color-text-secondary)' }}>
                                        No extraction available for this document yet.
                                    </div>
                                ) : (
                                    <div className="p-4">
                                        <dl className="grid gap-3 mb-6" style={{ gridTemplateColumns: '1fr 1fr' }}>
                                            <Field label="Vendor" value={fields.vendorName} />
                                            <Field label="Invoice #" value={fields.invoiceNumber} />
                                            <Field label="Invoice date" value={fields.invoiceDate} />
                                            <Field label="Due date" value={fields.dueDate} />
                                            <Field label="Total" value={fields.totalAmount} bold />
                                        </dl>

                                        {Array.isArray(fields.lineItems) && fields.lineItems.length > 0 && (
                                            <div>
                                                <h4 className="mb-2" style={{
                                                    fontSize: '13px', fontWeight: 600,
                                                    color: 'var(--color-text-secondary)',
                                                    textTransform: 'uppercase', letterSpacing: '0.04em',
                                                }}>
                                                    Line items
                                                </h4>
                                                <table className="w-full text-left border-collapse text-sm">
                                                    <thead>
                                                        <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
                                                            <th className="py-2 pr-2 font-semibold" style={{ color: 'var(--color-text-secondary)' }}>Description</th>
                                                            <th className="py-2 pr-2 font-semibold" style={{ color: 'var(--color-text-secondary)' }}>Qty</th>
                                                            <th className="py-2 pr-2 font-semibold" style={{ color: 'var(--color-text-secondary)' }}>Unit price</th>
                                                            <th className="py-2 font-semibold text-right" style={{ color: 'var(--color-text-secondary)' }}>Amount</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        {fields.lineItems.map((item, idx) => (
                                                            <tr key={idx} style={{ borderBottom: '1px solid var(--color-border)' }}>
                                                                <td className="py-2 pr-2">{item.description}</td>
                                                                <td className="py-2 pr-2">{item.quantity || '—'}</td>
                                                                <td className="py-2 pr-2">{item.unitPrice || '—'}</td>
                                                                <td className="py-2 text-right">{item.amount || '—'}</td>
                                                            </tr>
                                                        ))}
                                                    </tbody>
                                                </table>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                            
                            {/* Summary */}
                            <div className="card overflow-hidden" style={{ gridColumn: '1 / -1' }}>
                                <div className="px-4 py-3" style={{ borderBottom: '1px solid var(--color-border)' }}>
                                    <h3>Summary</h3>
                                </div>
                                {document?.status === 'PENDING' ? (
                                    <div className="p-6" style={{ color: 'var(--color-text-secondary)' }}>
                                        Still processing this document — summarization hasn't run yet.
                                    </div>
                                ) : summaryFailedReason ? (
                                    <div className="p-6">
                                        <span className="badge badge-danger">Summarization failed</span>
                                        <p style={{ color: 'var(--color-text-secondary)', marginTop: '10px', fontSize: '14px' }}>
                                            {summaryFailedReason}
                                        </p>
                                    </div>
                                ) : !summary ? (
                                    <div className="p-6" style={{ color: 'var(--color-text-secondary)' }}>
                                        No summary available for this document yet.
                                    </div>
                                ) : (
                                    <div className="p-6 text-sm" style={{ lineHeight: '1.6', color: 'var(--color-text-primary)' }}>
                                        {summary}
                                    </div>
                                )}
                            </div>
                        </div>
                    </>
                )}
            </main>
        </div>
    );
}

function Field({ label, value, bold }) {
    return (
        <div>
            <dt style={{
                fontSize: '12px', fontWeight: 600, color: 'var(--color-text-secondary)',
                textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '2px',
            }}>
                {label}
            </dt>
            <dd style={{ fontSize: bold ? '18px' : '15px', fontWeight: bold ? 700 : 400 }}>
                {value || '—'}
            </dd>
        </div>
    );
}
