import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'motion/react';
import {
    ArrowLeft, Upload, FileText, CheckCircle2, Loader2,
    X, Plus, AlertCircle, Files
} from 'lucide-react';
import AmbientAurora from '../components/ui/AmbientAurora';

/**
 * Upload Document page — supports single & multi-file batch upload (up to 10 files),
 * neomorphic styling, queue preview, individual file removal, and type tagging.
 */
export default function UploadDocument() {
    const navigate = useNavigate();
    const [files, setFiles] = useState([]);
    const [type, setType] = useState('INVOICE');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [dragging, setDragging] = useState(false);

    const MAX_FILES = 10;
    const MAX_FILE_SIZE_MB = 10;

    function addFiles(newFiles) {
        setError(null);
        const validFiles = [];
        const fileArray = Array.from(newFiles);

        for (const file of fileArray) {
            if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
                setError(`File "${file.name}" exceeds the 10 MB limit.`);
                continue;
            }
            const ext = file.name.split('.').pop().toLowerCase();
            if (!['pdf', 'png', 'jpg', 'jpeg'].includes(ext)) {
                setError(`File "${file.name}" is not a supported format (PDF, PNG, JPG).`);
                continue;
            }
            validFiles.push(file);
        }

        setFiles(prev => {
            const combined = [...prev, ...validFiles];
            if (combined.length > MAX_FILES) {
                setError(`Maximum ${MAX_FILES} files allowed per batch upload.`);
                return combined.slice(0, MAX_FILES);
            }
            return combined;
        });
    }

    function handleFileChange(e) {
        if (e.target.files && e.target.files.length > 0) {
            addFiles(e.target.files);
            e.target.value = ''; // Reset input so same file can be re-selected if removed
        }
    }

    function handleDrop(e) {
        e.preventDefault();
        setDragging(false);
        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
            addFiles(e.dataTransfer.files);
        }
    }

    function removeFile(indexToRemove) {
        setFiles(prev => prev.filter((_, idx) => idx !== indexToRemove));
    }

    function clearAllFiles() {
        setFiles([]);
        setError(null);
    }

    async function handleSubmit(e) {
        e.preventDefault();
        if (files.length === 0) return;

        setLoading(true);
        setError(null);

        const formData = new FormData();
        formData.append('type', type);

        const token = localStorage.getItem('docket_token');
        const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

        try {
            let endpoint = `${baseUrl}/api/documents/upload`;
            if (files.length === 1) {
                formData.append('file', files[0]);
            } else {
                endpoint = `${baseUrl}/api/documents/batch`;
                files.forEach(f => formData.append('files', f));
            }

            const response = await fetch(endpoint, {
                method: 'POST',
                headers: { Authorization: `Bearer ${token}` },
                body: formData,
            });

            const data = await response.json();
            if (!response.ok) {
                throw new Error(data?.error?.message || 'Failed to upload document(s)');
            }

            navigate('/dashboard', { replace: true });
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    const totalSizeBytes = files.reduce((acc, f) => acc + f.size, 0);
    const totalSizeMb = (totalSizeBytes / (1024 * 1024)).toFixed(2);

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
                    <span className="text-aurora cursor-pointer" style={{ fontSize: 17, fontWeight: 800 }} onClick={() => navigate('/dashboard')}>
                        Docket
                    </span>
                    <button onClick={() => navigate('/dashboard')} className="btn-secondary"
                        style={{ padding: '6px 14px', fontSize: 13 }}
                        aria-label="Return to Dashboard">
                        <ArrowLeft size={13} /> Dashboard
                    </button>
                </div>
            </nav>

            <main className="w-full max-w-[680px] mx-auto px-4 py-8 relative z-10">
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
                            Upload Documents
                        </h2>
                        <p style={{ color: 'var(--color-text-secondary)', fontSize: 13.5 }}>
                            Upload single files or multi-document batches for automated OCR, structured extraction, and anomaly checking.
                        </p>
                    </div>

                    {error && (
                        <div className="alert-error mb-5 flex items-center gap-2" role="alert">
                            <AlertCircle size={15} className="flex-shrink-0" />
                            <span>{error}</span>
                        </div>
                    )}

                    <form onSubmit={handleSubmit}>
                        {/* Document type — card selector */}
                        <div style={{ marginBottom: 22 }}>
                            <label className="label" style={{ marginBottom: 10 }}>Target Document Type</label>
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
                                        aria-pressed={type === opt.value}
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
                        <div style={{ marginBottom: 20 }}>
                            <div className="flex items-center justify-between mb-2">
                                <label className="label" style={{ margin: 0 }}>
                                    {files.length > 0 ? `Selected Files (${files.length}/${MAX_FILES})` : 'Document Files'}
                                </label>
                                {files.length > 0 && (
                                    <button type="button" onClick={clearAllFiles}
                                        className="text-xs text-rose-400 hover:text-rose-300 transition-colors"
                                        aria-label="Clear all selected files">
                                        Clear all
                                    </button>
                                )}
                            </div>

                            <input id="fileUpload" type="file" accept=".pdf,.png,.jpg,.jpeg" multiple
                                className="hidden" onChange={handleFileChange} />
                            
                            <label
                                htmlFor="fileUpload"
                                onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
                                onDragLeave={() => setDragging(false)}
                                onDrop={handleDrop}
                                style={{
                                    display: 'flex', flexDirection: 'column', alignItems: 'center',
                                    justifyContent: 'center', gap: 10, padding: files.length > 0 ? '20px' : '32px 20px',
                                    borderRadius: 14,
                                    border: `2px dashed ${files.length > 0 ? 'rgba(52,211,153,0.35)' : dragging ? 'rgba(124,92,252,0.60)' : 'rgba(196,181,253,0.18)'}`,
                                    background: files.length > 0 ? 'rgba(52,211,153,0.03)' : dragging ? 'rgba(124,92,252,0.07)' : 'var(--color-bg)',
                                    boxShadow: 'var(--neo-shadow-inset)',
                                    cursor: 'pointer',
                                    transition: 'all 0.2s ease',
                                }}
                            >
                                <div style={{
                                    width: 40, height: 40, borderRadius: 10,
                                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                                    background: files.length > 0 ? 'rgba(52,211,153,0.12)' : 'rgba(124,92,252,0.12)',
                                    color: files.length > 0 ? '#34D399' : 'var(--color-aurora-start)',
                                    boxShadow: 'var(--neo-shadow-sm)',
                                }}>
                                    {files.length > 0 ? <Plus size={18} /> : <Files size={18} />}
                                </div>
                                <div style={{ textAlign: 'center' }}>
                                    <p style={{ fontSize: 13.5, fontWeight: 600, color: 'var(--color-text-primary)', marginBottom: 2 }}>
                                        {files.length > 0 ? 'Click or drop more files to add to batch' : 'Click to browse or drag & drop files'}
                                    </p>
                                    <p style={{ fontSize: 12, color: 'var(--color-text-disabled)' }}>
                                        PDF, PNG, or JPG · up to 10 MB each (max {MAX_FILES} files)
                                    </p>
                                </div>
                            </label>
                        </div>

                        {/* File Queue List */}
                        <AnimatePresence>
                            {files.length > 0 && (
                                <motion.div
                                    initial={{ opacity: 0, height: 0 }}
                                    animate={{ opacity: 1, height: 'auto' }}
                                    exit={{ opacity: 0, height: 0 }}
                                    className="mb-6 overflow-hidden"
                                >
                                    <div className="rounded-xl border p-3"
                                        style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
                                        <div className="flex items-center justify-between text-xs text-gray-400 mb-2 px-1">
                                            <span>Batch Queue ({files.length} {files.length === 1 ? 'file' : 'files'})</span>
                                            <span>Total: {totalSizeMb} MB</span>
                                        </div>
                                        <div className="space-y-1.5 max-h-[180px] overflow-y-auto pr-1">
                                            {files.map((file, index) => (
                                                <motion.div
                                                    key={`${file.name}-${index}`}
                                                    initial={{ opacity: 0, x: -6 }}
                                                    animate={{ opacity: 1, x: 0 }}
                                                    exit={{ opacity: 0, x: 6 }}
                                                    className="flex items-center justify-between p-2 rounded-lg text-xs"
                                                    style={{ background: 'var(--color-surface-raised)', border: '1px solid rgba(255,255,255,0.04)' }}
                                                >
                                                    <div className="flex items-center gap-2 truncate pr-2">
                                                        <FileText size={13} className="text-cyan-400 flex-shrink-0" />
                                                        <span className="text-gray-200 font-medium truncate">{file.name}</span>
                                                        <span className="text-gray-500 text-[11px] flex-shrink-0">
                                                            ({(file.size / (1024 * 1024)).toFixed(2)} MB)
                                                        </span>
                                                    </div>
                                                    <button
                                                        type="button"
                                                        onClick={() => removeFile(index)}
                                                        className="text-gray-400 hover:text-rose-400 transition-colors p-1"
                                                        aria-label={`Remove ${file.name}`}
                                                    >
                                                        <X size={13} />
                                                    </button>
                                                </motion.div>
                                            ))}
                                        </div>
                                    </div>
                                </motion.div>
                            )}
                        </AnimatePresence>

                        <button type="submit" className="btn-primary w-full" disabled={files.length === 0 || loading}
                            style={{ padding: '12px 22px', fontSize: 14, justifyContent: 'center' }}
                            aria-label="Upload and process documents">
                            {loading ? (
                                <><Loader2 size={15} className="animate-spin" /> Uploading & queueing {files.length} {files.length === 1 ? 'document' : 'documents'}…</>
                            ) : (
                                <><Upload size={14} /> Upload {files.length > 1 ? `${files.length} Documents (Batch)` : 'Document'}</>
                            )}
                        </button>
                    </form>
                </motion.div>
            </main>
        </div>
    );
}
