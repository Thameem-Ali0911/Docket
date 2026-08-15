import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch, clearToken } from '../lib/api';

/**
 * Dashboard page — shows the user's workspace and document list.
 */
export default function Dashboard() {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [documents, setDocuments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState(null);

    useEffect(() => {
        loadDashboard();
    }, [navigate]);

    function loadDashboard() {
        setLoading(true);
        setLoadError(null);
        // Fetch user and workspace documents in parallel
        Promise.all([
            apiFetch('/api/auth/me'),
            apiFetch('/api/documents')
        ])
            .then(([userData, docsData]) => {
                setUser(userData);
                setDocuments(docsData);
            })
            .catch((err) => {
                if (err.status === 401) {
                    // Token is genuinely invalid/expired — send back to login
                    clearToken();
                    navigate('/login', { replace: true });
                    return;
                }
                // Any other failure (backend momentarily busy with OCR/Gemini
                // work, transient 500, network blip) shouldn't wipe a valid login.
                setLoadError(err.message || 'Failed to load dashboard.');
            })
            .finally(() => setLoading(false));
    }

    function handleLogout() {
        clearToken();
        navigate('/login', { replace: true });
    }

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center"
                 style={{ background: 'var(--color-bg)' }}>
                <p style={{ color: 'var(--color-text-secondary)' }}>Loading…</p>
            </div>
        );
    }

    if (loadError) {
        return (
            <div className="min-h-screen flex items-center justify-center px-6"
                 style={{ background: 'var(--color-bg)' }}>
                <div className="card p-8 text-center" style={{ maxWidth: '420px' }}>
                    <p className="alert-error" style={{ marginBottom: '16px' }}>{loadError}</p>
                    <button className="btn-primary" onClick={loadDashboard}>Retry</button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen" style={{ background: 'var(--color-bg)' }}>
            {/* Top navigation bar */}
            <nav className="card flex items-center justify-between px-6 py-3"
                 style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none' }}>
                <div className="flex items-center gap-3">
                    <h2 className="text-xl font-bold"
                        style={{ fontFamily: 'var(--font-heading)', color: 'var(--color-primary)' }}>
                        Docket
                    </h2>
                    {user && (
                        <span className="badge badge-info">{user.workspaceName}</span>
                    )}
                </div>
                <div className="flex items-center gap-4">
                    {user && (
                        <span style={{ color: 'var(--color-text-secondary)', fontSize: '14px' }}>
                            {user.email}
                        </span>
                    )}
                    <button onClick={handleLogout} className="btn-secondary" style={{ padding: '6px 14px', fontSize: '13px' }}>
                        Sign out
                    </button>
                </div>
            </nav>

            {/* Main content */}
            <main className="max-w-5xl mx-auto px-6 py-12">
                <div className="flex items-center justify-between mb-8">
                    <h1>Dashboard</h1>
                    <button className="btn-primary" onClick={() => navigate('/upload')}>
                        Upload Document
                    </button>
                </div>

                {documents.length === 0 ? (
                    <div className="card p-12 text-center">
                        <div style={{ marginBottom: '16px' }}>
                            <svg width="48" height="48" viewBox="0 0 24 24" fill="none"
                                 stroke="var(--color-text-disabled)" strokeWidth="1.5"
                                 strokeLinecap="round" strokeLinejoin="round"
                                 style={{ margin: '0 auto' }}>
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                <polyline points="14 2 14 8 20 8" />
                                <line x1="16" y1="13" x2="8" y2="13" />
                                <line x1="16" y1="17" x2="8" y2="17" />
                                <polyline points="10 9 9 9 8 9" />
                            </svg>
                        </div>
                        <h3 style={{ marginBottom: '8px' }}>No documents yet</h3>
                        <p style={{ color: 'var(--color-text-secondary)', marginBottom: '24px', fontSize: '15px' }}>
                            Upload your first document to get started with extraction, summarization, and anomaly detection.
                        </p>
                        <button className="btn-primary" onClick={() => navigate('/upload')}>
                            Upload document
                        </button>
                    </div>
                ) : (
                    <div className="card overflow-hidden">
                        <table className="w-full text-left border-collapse">
                            <thead>
                                <tr style={{ borderBottom: '1px solid var(--color-border)', backgroundColor: '#F9FAFB' }}>
                                    <th className="py-3 px-4 font-semibold text-sm" style={{ color: 'var(--color-text-secondary)' }}>Type</th>
                                    <th className="py-3 px-4 font-semibold text-sm" style={{ color: 'var(--color-text-secondary)' }}>File</th>
                                    <th className="py-3 px-4 font-semibold text-sm" style={{ color: 'var(--color-text-secondary)' }}>Status</th>
                                    <th className="py-3 px-4 font-semibold text-sm" style={{ color: 'var(--color-text-secondary)' }}>Uploaded At</th>
                                    <th className="py-3 px-4"></th>
                                </tr>
                            </thead>
                            <tbody>
                                {documents.map(doc => (
                                    <tr key={doc.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                                        <td className="py-3 px-4 font-medium">{doc.type}</td>
                                        <td className="py-3 px-4 text-sm" style={{ color: 'var(--color-text-secondary)' }}>
                                            <a href={import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + doc.fileUrl : doc.fileUrl} target="_blank" rel="noreferrer" className="hover:underline">
                                                {doc.fileUrl.split('/').pop()}
                                            </a>
                                        </td>
                                        <td className="py-3 px-4">
                                            <span className={`badge ${doc.status === 'PENDING' ? 'badge-warning' : doc.status === 'PROCESSED' ? 'badge-success' : 'badge-danger'}`}>
                                                {doc.status}
                                            </span>
                                        </td>
                                        <td className="py-3 px-4 text-sm" style={{ color: 'var(--color-text-secondary)' }}>
                                            {new Date(doc.uploadedAt).toLocaleString()}
                                        </td>
                                        <td className="py-3 px-4 text-right">
                                            <button
                                                className="text-sm font-medium hover:underline"
                                                style={{ color: 'var(--color-primary)' }}
                                                onClick={() => navigate(`/documents/${doc.id}`)}
                                            >
                                                View
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </main>
        </div>
    );
}
