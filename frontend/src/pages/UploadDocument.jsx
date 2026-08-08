import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../lib/api';

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
            // We use standard fetch here because apiFetch currently stringifies JSON by default
            // and we need to send multipart/form-data.
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

            // Redirect back to dashboard on success
            navigate('/dashboard', { replace: true });
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="min-h-screen" style={{ background: 'var(--color-bg)' }}>
            <nav className="card flex items-center justify-between px-6 py-3 mb-8"
                 style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none' }}>
                <h2 className="text-xl font-bold"
                    style={{ fontFamily: 'var(--font-heading)', color: 'var(--color-primary)' }}>
                    Docket
                </h2>
                <button onClick={() => navigate('/dashboard')} className="btn-secondary" style={{ padding: '6px 14px', fontSize: '13px' }}>
                    Back to Dashboard
                </button>
            </nav>

            <main className="max-w-xl mx-auto px-6">
                <div className="card p-8">
                    <h2 className="mb-6">Upload Document</h2>

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
                                <option value="CONTRACT" disabled>Contract (Coming Soon)</option>
                                <option value="RESUME" disabled>Resume (Coming Soon)</option>
                            </select>
                        </div>

                        <div className="mb-8">
                            <label className="label" htmlFor="fileUpload">File</label>
                            <input
                                id="fileUpload"
                                type="file"
                                accept=".pdf,.png,.jpg,.jpeg"
                                className="input"
                                onChange={handleFileChange}
                                required
                            />
                            <p className="mt-2 text-xs" style={{ color: 'var(--color-text-secondary)' }}>
                                PDF, PNG, or JPG up to 10MB
                            </p>
                        </div>

                        <button type="submit" className="btn-primary w-full" disabled={!file || loading}>
                            {loading ? 'Uploading...' : 'Upload Document'}
                        </button>
                    </form>
                </div>
            </main>
        </div>
    );
}
