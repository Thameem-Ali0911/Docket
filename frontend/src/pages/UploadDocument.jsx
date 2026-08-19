import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import { ArrowLeft, Upload, FileText, CheckCircle2 } from 'lucide-react';
import AmbientAurora from '../components/ui/AmbientAurora';

export default function UploadDocument() {
    const navigate = useNavigate();
    const [file, setFile] = useState(null);
    const [type, setType] = useState('INVOICE');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    function handleFileChange(e) {
        if (e.target.files && e.target.files[0]) {
            setFile(e.target.files[0]);
        }
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
            const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/documents/upload`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                },
                body: formData,
            });

            const data = await response.json();
            if (!response.ok) {
                throw new Error(data?.error?.message || 'Failed to upload document');
            }

            navigate('/dashboard', { replace: true });
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="min-h-screen relative overflow-hidden" style={{ background: 'var(--color-bg)' }}>
            <AmbientAurora opacity={0.4} />

            {/* Top Navigation */}
            <nav className="card relative z-20 flex items-center justify-between px-6 py-3.5 mb-8"
                 style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none', background: 'rgba(18, 16, 27, 0.85)' }}>
                <h2 className="text-xl font-bold tracking-tight">
                    <span className="text-aurora">Docket</span>
                </h2>
                <button onClick={() => navigate('/dashboard')} className="btn-secondary" style={{ padding: '5px 14px', fontSize: '13px' }}>
                    <ArrowLeft size={14} />
                    Back to Dashboard
                </button>
            </nav>

            <main className="max-w-xl mx-auto px-6 relative z-10 py-4">
                <motion.div
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3 }}
                    className="card p-8"
                    style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                >
                    <h2 className="text-2xl font-bold mb-2">Upload Document</h2>
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: '14px', marginBottom: '24px' }}>
                        Upload your file to run OCR, extract structured fields, and verify template compliance.
                    </p>

                    {error && <div className="alert-error mb-6">{error}</div>}

                    <form onSubmit={handleSubmit}>
                        <div className="mb-6">
                            <label className="label" htmlFor="docType">Document Type</label>
                            <select
                                id="docType"
                                className="input"
                                value={type}
                                onChange={(e) => setType(e.target.value)}
                            >
                                <option value="INVOICE">Invoice</option>
                                <option value="CONTRACT">Contract</option>
                                <option value="RESUME">Resume</option>
                            </select>
                        </div>

                        <div className="mb-8">
                            <label className="label" htmlFor="fileUpload">Document File</label>
                            <div className="border-2 border-dashed rounded-xl p-6 text-center transition-colors hover:border-purple-500/50"
                                 style={{ borderColor: 'var(--color-border)', background: 'var(--color-surface-raised)' }}>
                                <input
                                    id="fileUpload"
                                    type="file"
                                    accept=".pdf,.png,.jpg,.jpeg"
                                    className="hidden"
                                    onChange={handleFileChange}
                                />
                                <label htmlFor="fileUpload" className="cursor-pointer flex flex-col items-center gap-2">
                                    {file ? (
                                        <>
                                            <CheckCircle2 size={32} className="text-emerald-400" />
                                            <p className="font-semibold text-sm text-white">{file.name}</p>
                                            <p className="text-xs text-gray-400">{(file.size / (1024 * 1024)).toFixed(2)} MB • Click to change</p>
                                        </>
                                    ) : (
                                        <>
                                            <div className="w-10 h-10 rounded-full flex items-center justify-center"
                                                 style={{ background: 'rgba(124, 92, 252, 0.15)', color: 'var(--color-aurora-start)' }}>
                                                <Upload size={20} />
                                            </div>
                                            <p className="font-medium text-sm text-white mt-1">Click or drag file to upload</p>
                                            <p className="text-xs text-gray-400">PDF, PNG, or JPG up to 10MB</p>
                                        </>
                                    )}
                                </label>
                            </div>
                        </div>

                        <button type="submit" className="btn-primary w-full" disabled={!file || loading}>
                            {loading ? (
                                <>
                                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                    Uploading & Processing…
                                </>
                            ) : (
                                <>
                                    <Upload size={15} />
                                    Upload Document
                                </>
                            )}
                        </button>
                    </form>
                </motion.div>
            </main>
        </div>
    );
}
