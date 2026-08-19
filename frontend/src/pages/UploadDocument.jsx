import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import { ArrowLeft, Upload, FileText, CheckCircle2, Loader2 } from 'lucide-react';
import AmbientAurora from '../components/ui/AmbientAurora';

/**
 * Upload Document page — neomorphic card, inset dropzone well, spring animations.
 */
export default function UploadDocument() {
    const navigate = useNavigate();
    const [file, setFile] = useState(null);
    const [type, setType] = useState('INVOICE');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [dragging, setDragging] = useState(false);

    function handleFileChange(e) {
        if (e.target.files && e.target.files[0]) setFile(e.target.files[0]);
    }

    function handleDrop(e) {
        e.preventDefault();
        setDragging(false);
        const f = e.dataTransfer.files[0];
        if (f) setFile(f);
    }

    async function handleSubmit(e) {
        e.preventDefault();
        if (!file) return;
        setLoading(true);
        setError(null);
        const formData = new FormData();
        formData.append('file', file);
        formData.append('type', type);
        try {
            const token = localStorage.getItem('docket_token');
            const response = await fetch(
                `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/documents/upload`,
                { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: formData }
            );
            const data = await response.json();
            if (!response.ok) throw new Error(data?.error?.message || 'Failed to upload document');
            navigate('/dashboard', { replace: true });
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    /* Type selector options */
    const typeOptions = [
        { value: 'INVOICE', label: '🧾 Invoice', desc: 'Bills, receipts, purchase orders' },
        { value: 'CONTRACT', label: '📄 Contract', desc: 'Agreements, SOWs, NDAs' },
        { value: 'RESUME', label: '👤 Resume', desc: 'CVs and professional profiles' },
    ];

    return (
        <div className="min-h-screen relative overflow-hidden" style={{ background: 'var(--color-bg)' }}>
            <AmbientAurora opacity={0.38} />

            {/* Frosted nav strip */}
            <nav className="nav-frosted sticky top-0 z-30 px-4 sm:px-8 py-0">
                <div className="w-full max-w-[1600px] mx-auto flex items-center justify-between h-[52px]">
                    <span className="text-aurora" style={{ fontSize: 17, fontWeight: 800 }}>Docket</span>
                    <button onClick={() => navigate('/dashboard')} className="btn-secondary"
                        style={{ padding: '6px 14px', fontSize: 13 }}>
                        <ArrowLeft size={13} /> Dashboard
                    </button>
                </div>
            </nav>

            <main className="w-full max-w-[620px] mx-auto px-4 py-8 relative z-10">
                <motion.div
                    initial={{ opacity: 0, y: 16, scale: 0.98 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
                    className="card-lg"
                    style={{ padding: '40px 36px' }}
                >
                    {/* Aurora top edge line */}
                    <div style={{
                        position: 'absolute', top: 0, left: '12%', right: '12%', height: '1px',
                        background: 'linear-gradient(90deg, transparent, rgba(124,92,252,0.55), rgba(34,211,238,0.45), transparent)',
                    }} />

                    {/* Header */}
                    <div style={{ marginBottom: 28 }}>
                        <div style={{
                            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                            width: 44, height: 44, borderRadius: 12, marginBottom: 14,
                            background: 'rgba(124,92,252,0.13)',
                            border: '1px solid rgba(124,92,252,0.28)',
                            boxShadow: 'var(--neo-shadow-sm)',
                            color: 'var(--color-aurora-start)',
                        }}>
                            <Upload size={20} />
                        </div>
                        <h2 style={{ fontSize: 22, fontWeight: 800, letterSpacing: '-0.022em', marginBottom: 4 }}>
                            Upload Document
                        </h2>
                        <p style={{ color: 'var(--color-text-secondary)', fontSize: 13.5 }}>
                            OCR extraction, structured field parsing, and template compliance checking.
                        </p>
                    </div>

                    {error && <div className="alert-error mb-5">{error}</div>}

                    <form onSubmit={handleSubmit}>
                        {/* Document type — card selector */}
                        <div style={{ marginBottom: 22 }}>
                            <label className="label" style={{ marginBottom: 10 }}>Document Type</label>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                                {typeOptions.map(opt => (
                                    <button
                                        key={opt.value}
                                        type="button"
                                        onClick={() => setType(opt.value)}
                                        style={{
                                            display: 'flex', alignItems: 'center', gap: 12,
                                            padding: '11px 14px', borderRadius: 11,
                                            border: `1px solid ${type === opt.value ? 'rgba(124,92,252,0.50)' : 'var(--color-border)'}`,
                                            background: type === opt.value ? 'rgba(124,92,252,0.10)' : 'var(--color-bg)',
                                            boxShadow: type === opt.value ? 'var(--neo-shadow-sm), 0 0 12px rgba(124,92,252,0.12)' : 'var(--neo-shadow-inset)',
                                            cursor: 'pointer',
                                            transition: 'all 0.18s ease',
                                            textAlign: 'left',
                                        }}
                                    >
                                        <span style={{ fontSize: 18, lineHeight: 1 }}>{opt.label.split(' ')[0]}</span>
                                        <div>
                                            <p style={{
                                                fontSize: 13.5, fontWeight: 600,
                                                color: type === opt.value ? 'var(--color-aurora-start)' : 'var(--color-text-primary)',
                                            }}>
                                                {opt.label.split(' ').slice(1).join(' ')}
                                            </p>
                                            <p style={{ fontSize: 11.5, color: 'var(--color-text-disabled)' }}>{opt.desc}</p>
                                        </div>
                                        {type === opt.value && (
                                            <CheckCircle2 size={15} style={{ marginLeft: 'auto', color: 'var(--color-aurora-start)', flexShrink: 0 }} />
                                        )}
                                    </button>
                                ))}
                            </div>
                        </div>

                        {/* Dropzone */}
                        <div style={{ marginBottom: 24 }}>
                            <label className="label" style={{ marginBottom: 10 }}>Document File</label>
                            <input id="fileUpload" type="file" accept=".pdf,.png,.jpg,.jpeg"
                                className="hidden" onChange={handleFileChange} />
                            <label
                                htmlFor="fileUpload"
                                onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
                                onDragLeave={() => setDragging(false)}
                                onDrop={handleDrop}
                                style={{
                                    display: 'flex', flexDirection: 'column', alignItems: 'center',
                                    justifyContent: 'center', gap: 10, padding: '32px 20px',
                                    borderRadius: 14,
                                    border: `2px dashed ${file ? 'rgba(52,211,153,0.45)' : dragging ? 'rgba(124,92,252,0.60)' : 'rgba(196,181,253,0.18)'}`,
                                    background: file ? 'rgba(52,211,153,0.05)' : dragging ? 'rgba(124,92,252,0.07)' : 'var(--color-bg)',
                                    boxShadow: 'var(--neo-shadow-inset)',
                                    cursor: 'pointer',
                                    transition: 'all 0.2s ease',
                                }}
                            >
                                {file ? (
                                    <>
                                        <div style={{
                                            width: 44, height: 44, borderRadius: 12,
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                            background: 'rgba(52,211,153,0.12)', color: '#34D399',
                                            boxShadow: 'var(--neo-shadow-sm)',
                                        }}>
                                            <CheckCircle2 size={22} />
                                        </div>
                                        <div style={{ textAlign: 'center' }}>
                                            <p style={{ fontSize: 13.5, fontWeight: 600, color: 'var(--color-text-primary)', marginBottom: 3 }}>
                                                {file.name}
                                            </p>
                                            <p style={{ fontSize: 12, color: 'var(--color-text-disabled)' }}>
                                                {(file.size / (1024 * 1024)).toFixed(2)} MB · Click to change
                                            </p>
                                        </div>
                                    </>
                                ) : (
                                    <>
                                        <div style={{
                                            width: 44, height: 44, borderRadius: 12,
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                            background: 'rgba(124,92,252,0.12)', color: 'var(--color-aurora-start)',
                                            boxShadow: 'var(--neo-shadow-sm)',
                                        }}>
                                            <FileText size={20} />
                                        </div>
                                        <div style={{ textAlign: 'center' }}>
                                            <p style={{ fontSize: 13.5, fontWeight: 600, color: 'var(--color-text-primary)', marginBottom: 3 }}>
                                                Click to browse or drag & drop
                                            </p>
                                            <p style={{ fontSize: 12, color: 'var(--color-text-disabled)' }}>
                                                PDF, PNG, or JPG · up to 10 MB
                                            </p>
                                        </div>
                                    </>
                                )}
                            </label>
                        </div>

                        <button type="submit" className="btn-primary w-full" disabled={!file || loading}
                            style={{ padding: '12px 22px', fontSize: 14, justifyContent: 'center' }}>
                            {loading ? (
                                <><Loader2 size={15} className="animate-spin" /> Uploading & processing…</>
                            ) : (
                                <><Upload size={14} /> Upload Document</>
                            )}
                        </button>
                    </form>
                </motion.div>
            </main>
        </div>
    );
}
