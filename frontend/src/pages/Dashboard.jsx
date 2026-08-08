import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch, clearToken } from '../lib/api';

/**
 * Dashboard page — shows the user's workspace and document list.
 * Phase 1: empty state only ("No documents yet").
 * Calls GET /api/auth/me on mount to verify the session and get workspace info.
 */
export default function Dashboard() {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        apiFetch('/api/auth/me')
            .then(setUser)
            .catch(() => {
                // Token is invalid/expired — send back to login
                clearToken();
                navigate('/login', { replace: true });
            })
            .finally(() => setLoading(false));
    }, [navigate]);

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
                </div>

                {/* Empty state — Phase 1 placeholder */}
                <div className="card p-12 text-center">
                    <div style={{ marginBottom: '16px' }}>
                        {/* Simple document icon */}
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
                    <button className="btn-primary" disabled title="Upload available in Phase 2">
                        Upload document
                    </button>
                </div>
            </main>
        </div>
    );
}
