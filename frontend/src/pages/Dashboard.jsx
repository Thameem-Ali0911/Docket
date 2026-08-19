import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch, clearToken, downloadExport } from '../lib/api';

/**
 * Dashboard page — provides document intelligence overview,
 * rich filtering (by type, anomaly status, date), and CSV/JSON export.
 */
export default function Dashboard() {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [documents, setDocuments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState(null);
    const [exporting, setExporting] = useState(false);

    // Filter states
    const [searchQuery, setSearchQuery] = useState('');
    const [typeFilter, setTypeFilter] = useState('ALL');
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [dateFilter, setDateFilter] = useState('ALL');

    useEffect(() => {
        loadDashboard();
    }, [navigate]);

    function loadDashboard() {
        setLoading(true);
        setLoadError(null);
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
                    clearToken();
                    navigate('/login', { replace: true });
                    return;
                }
                setLoadError(err.message || 'Failed to load dashboard.');
            })
            .finally(() => setLoading(false));
    }

    function handleLogout() {
        clearToken();
        navigate('/login', { replace: true });
    }

    async function handleBulkExport(format) {
        setExporting(true);
        try {
            await downloadExport(`/api/documents/export?format=${format}`, `docket-workspace-export.${format}`);
        } catch (err) {
            alert(`Bulk export failed: ${err.message}`);
        } finally {
            setExporting(false);
        }
    }

    async function handleSingleExport(docId, format, e) {
        e.stopPropagation();
        try {
            await downloadExport(`/api/documents/${docId}/export?format=${format}`, `document-${docId}-export.${format}`);
        } catch (err) {
            alert(`Document export failed: ${err.message}`);
        }
    }

    // Filtered documents calculation
    const filteredDocuments = useMemo(() => {
        return documents.filter(doc => {
            // Search query filter (matches filename, id, or failed reason)
            if (searchQuery.trim()) {
                const query = searchQuery.toLowerCase();
                const filename = doc.fileUrl ? doc.fileUrl.split('/').pop().toLowerCase() : '';
                const matchesSearch = filename.includes(query) ||
                    String(doc.id).includes(query) ||
                    doc.type.toLowerCase().includes(query) ||
                    (doc.failedReason && doc.failedReason.toLowerCase().includes(query));
                if (!matchesSearch) return false;
            }

            // Type filter
            if (typeFilter !== 'ALL' && doc.type !== typeFilter) {
                return false;
            }

            // Status & Flag filter
            if (statusFilter === 'FLAGGED') {
                if (doc.status !== 'PROCESSED' || doc.anomalyCount <= 0) return false;
            } else if (statusFilter === 'CLEAN') {
                if (doc.status !== 'PROCESSED' || doc.anomalyCount > 0) return false;
            } else if (statusFilter === 'PENDING') {
                if (doc.status !== 'PENDING') return false;
            } else if (statusFilter === 'FAILED') {
                if (doc.status !== 'FAILED') return false;
            }

            // Date filter
            if (dateFilter !== 'ALL' && doc.uploadedAt) {
                const docDate = new Date(doc.uploadedAt);
                const now = new Date();
                if (dateFilter === 'TODAY') {
                    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
                    if (docDate < startOfToday) return false;
                } else if (dateFilter === '7DAYS') {
                    const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
                    if (docDate < sevenDaysAgo) return false;
                } else if (dateFilter === '30DAYS') {
                    const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
                    if (docDate < thirtyDaysAgo) return false;
                }
            }

            return true;
        });
    }, [documents, searchQuery, typeFilter, statusFilter, dateFilter]);

    // Statistics counts
    const stats = useMemo(() => {
        const total = documents.length;
        const processed = documents.filter(d => d.status === 'PROCESSED').length;
        const flagged = documents.filter(d => d.status === 'PROCESSED' && d.anomalyCount > 0).length;
        const pending = documents.filter(d => d.status === 'PENDING').length;
        const failed = documents.filter(d => d.status === 'FAILED').length;
        return { total, processed, flagged, pending, failed };
    }, [documents]);

    const isFiltered = searchQuery !== '' || typeFilter !== 'ALL' || statusFilter !== 'ALL' || dateFilter !== 'ALL';

    function clearAllFilters() {
        setSearchQuery('');
        setTypeFilter('ALL');
        setStatusFilter('ALL');
        setDateFilter('ALL');
    }

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center" style={{ background: 'var(--color-bg)' }}>
                <p style={{ color: 'var(--color-text-secondary)' }}>Loading dashboard…</p>
            </div>
        );
    }

    if (loadError) {
        return (
            <div className="min-h-screen flex items-center justify-center px-6" style={{ background: 'var(--color-bg)' }}>
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
                    <h2 className="text-xl font-bold" style={{ fontFamily: 'var(--font-heading)', color: 'var(--color-primary)' }}>
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
            <main className="max-w-6xl mx-auto px-6 py-10">
                {/* Header */}
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
                    <div>
                        <h1>Dashboard</h1>
                        <p style={{ color: 'var(--color-text-secondary)', marginTop: '4px' }}>
                            Monitor document processing, anomaly flags, and export workspace intelligence.
                        </p>
                    </div>
                    <div className="flex flex-wrap gap-3">
                        <button className="btn-secondary" onClick={() => navigate('/templates')}>
                            Manage Templates
                        </button>
                        <button className="btn-primary" onClick={() => navigate('/upload')}>
                            Upload Document
                        </button>
                    </div>
                </div>

                {/* Overview Metrics Cards */}
                <div className="grid grid-cols-2 sm:grid-cols-5 gap-4 mb-8">
                    <div className="card p-4">
                        <p className="text-xs font-semibold uppercase tracking-wider text-gray-500 mb-1">Total Uploads</p>
                        <p className="text-2xl font-bold" style={{ color: 'var(--color-primary)' }}>{stats.total}</p>
                    </div>
                    <div className="card p-4">
                        <p className="text-xs font-semibold uppercase tracking-wider text-gray-500 mb-1">Processed</p>
                        <p className="text-2xl font-bold text-green-700">{stats.processed}</p>
                    </div>
                    <div className="card p-4">
                        <p className="text-xs font-semibold uppercase tracking-wider text-gray-500 mb-1">Flagged</p>
                        <p className="text-2xl font-bold" style={{ color: 'var(--color-warning)' }}>{stats.flagged}</p>
                    </div>
                    <div className="card p-4">
                        <p className="text-xs font-semibold uppercase tracking-wider text-gray-500 mb-1">Processing</p>
                        <p className="text-2xl font-bold text-blue-600">{stats.pending}</p>
                    </div>
                    <div className="card p-4">
                        <p className="text-xs font-semibold uppercase tracking-wider text-gray-500 mb-1">Failed</p>
                        <p className="text-2xl font-bold text-red-600">{stats.failed}</p>
                    </div>
                </div>

                {/* Filter and Action Bar */}
                <div className="card p-4 mb-6">
                    <div className="flex flex-col md:flex-row gap-4 justify-between items-stretch md:items-center">
                        {/* Search & Selectors */}
                        <div className="flex flex-wrap flex-1 gap-3 items-center">
                            {/* Search */}
                            <div className="relative min-w-[200px] flex-1">
                                <input
                                    type="text"
                                    placeholder="Search filename, ID, or keywords…"
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                    className="input"
                                    style={{ paddingLeft: '32px' }}
                                />
                                <svg className="absolute left-2.5 top-3 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <circle cx="11" cy="11" r="8" strokeWidth="2" />
                                    <line x1="21" y1="21" x2="16.65" y2="16.65" strokeWidth="2" strokeLinecap="round" />
                                </svg>
                            </div>

                            {/* Type Filter */}
                            <select
                                value={typeFilter}
                                onChange={(e) => setTypeFilter(e.target.value)}
                                className="input w-auto text-sm"
                            >
                                <option value="ALL">All Types</option>
                                <option value="INVOICE">Invoices</option>
                                <option value="CONTRACT">Contracts</option>
                                <option value="RESUME">Resumes</option>
                            </select>

                            {/* Status / Flag Filter */}
                            <select
                                value={statusFilter}
                                onChange={(e) => setStatusFilter(e.target.value)}
                                className="input w-auto text-sm"
                            >
                                <option value="ALL">All Statuses</option>
                                <option value="FLAGGED">⚠️ Flagged (Deviations)</option>
                                <option value="CLEAN">✅ Standard / Clean</option>
                                <option value="PENDING">Processing (Pending)</option>
                                <option value="FAILED">Failed</option>
                            </select>

                            {/* Date Filter */}
                            <select
                                value={dateFilter}
                                onChange={(e) => setDateFilter(e.target.value)}
                                className="input w-auto text-sm"
                            >
                                <option value="ALL">All Time</option>
                                <option value="TODAY">Today</option>
                                <option value="7DAYS">Last 7 Days</option>
                                <option value="30DAYS">Last 30 Days</option>
                            </select>

                            {isFiltered && (
                                <button
                                    onClick={clearAllFilters}
                                    className="btn-secondary"
                                    style={{ padding: '8px 12px', fontSize: '13px' }}
                                    title="Reset all filters"
                                >
                                    ✕ Clear
                                </button>
                            )}
                        </div>

                        {/* Bulk Export Actions */}
                        {documents.length > 0 && (
                            <div className="flex items-center gap-2 border-t md:border-t-0 pt-3 md:pt-0 shrink-0">
                                <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider mr-1">Bulk Export:</span>
                                <button
                                    className="btn-secondary"
                                    onClick={() => handleBulkExport('csv')}
                                    disabled={exporting}
                                    style={{ padding: '6px 12px', fontSize: '12px' }}
                                    title="Export all documents as CSV"
                                >
                                    📥 CSV
                                </button>
                                <button
                                    className="btn-secondary"
                                    onClick={() => handleBulkExport('json')}
                                    disabled={exporting}
                                    style={{ padding: '6px 12px', fontSize: '12px' }}
                                    title="Export all documents as JSON"
                                >
                                    📥 JSON
                                </button>
                            </div>
                        )}
                    </div>
                </div>

                {/* Documents Table */}
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
                ) : filteredDocuments.length === 0 ? (
                    <div className="card p-12 text-center">
                        <h3 style={{ marginBottom: '8px' }}>No matching documents</h3>
                        <p style={{ color: 'var(--color-text-secondary)', marginBottom: '20px', fontSize: '14px' }}>
                            No documents matched your search and filter criteria.
                        </p>
                        <button className="btn-secondary" onClick={clearAllFilters}>
                            Clear filters
                        </button>
                    </div>
                ) : (
                    <div className="card overflow-hidden">
                        <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr style={{ borderBottom: '1px solid var(--color-border)', backgroundColor: '#F9FAFB' }}>
                                        <th className="py-3 px-4 font-semibold text-xs text-gray-500 uppercase tracking-wider">Type</th>
                                        <th className="py-3 px-4 font-semibold text-xs text-gray-500 uppercase tracking-wider">Document / File</th>
                                        <th className="py-3 px-4 font-semibold text-xs text-gray-500 uppercase tracking-wider">Status</th>
                                        <th className="py-3 px-4 font-semibold text-xs text-gray-500 uppercase tracking-wider">Anomaly Flags</th>
                                        <th className="py-3 px-4 font-semibold text-xs text-gray-500 uppercase tracking-wider">Uploaded At</th>
                                        <th className="py-3 px-4 text-right font-semibold text-xs text-gray-500 uppercase tracking-wider">Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredDocuments.map(doc => {
                                        const filename = doc.fileUrl ? doc.fileUrl.split('/').pop() : 'Document';
                                        return (
                                            <tr key={doc.id}
                                                className="hover:bg-gray-50/50 cursor-pointer transition-colors"
                                                onClick={() => navigate(`/documents/${doc.id}`)}
                                                style={{ borderBottom: '1px solid var(--color-border)' }}>
                                                <td className="py-3 px-4">
                                                    <span className="font-semibold text-xs px-2.5 py-1 rounded bg-gray-100 text-gray-800">
                                                        {doc.type}
                                                    </span>
                                                </td>
                                                <td className="py-3 px-4 text-sm font-medium" style={{ color: 'var(--color-text-primary)' }}>
                                                    <div>
                                                        <span className="hover:underline">{filename}</span>
                                                        <p className="text-xs text-gray-400 font-normal">ID: {doc.id}</p>
                                                    </div>
                                                </td>
                                                <td className="py-3 px-4">
                                                    <span className={`badge ${doc.status === 'PENDING' ? 'badge-warning' : doc.status === 'PROCESSED' ? 'badge-success' : 'badge-danger'}`}>
                                                        {doc.status}
                                                    </span>
                                                </td>
                                                <td className="py-3 px-4">
                                                    {doc.status === 'PROCESSED' ? (
                                                        doc.anomalyCount > 0 ? (
                                                            <span className="badge badge-warning" title={`${doc.anomalyCount} anomalies detected`}>
                                                                ⚠️ {doc.anomalyCount} Deviation{doc.anomalyCount > 1 ? 's' : ''}
                                                            </span>
                                                        ) : (
                                                            <span className="badge badge-success" title="Matches standard template">
                                                                ✅ Standard
                                                            </span>
                                                        )
                                                    ) : (
                                                        <span className="text-xs text-gray-400">—</span>
                                                    )}
                                                </td>
                                                <td className="py-3 px-4 text-sm" style={{ color: 'var(--color-text-secondary)' }}>
                                                    {new Date(doc.uploadedAt).toLocaleString()}
                                                </td>
                                                <td className="py-3 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                                                    <div className="flex items-center justify-end gap-2">
                                                        <button
                                                            className="text-xs font-semibold hover:underline"
                                                            style={{ color: 'var(--color-primary)' }}
                                                            onClick={() => navigate(`/documents/${doc.id}`)}
                                                        >
                                                            View
                                                        </button>
                                                        <span className="text-gray-300">|</span>
                                                        <button
                                                            className="text-xs text-gray-500 hover:text-gray-900"
                                                            onClick={(e) => handleSingleExport(doc.id, 'csv', e)}
                                                            title="Export CSV"
                                                        >
                                                            CSV
                                                        </button>
                                                        <span className="text-gray-300">|</span>
                                                        <button
                                                            className="text-xs text-gray-500 hover:text-gray-900"
                                                            onClick={(e) => handleSingleExport(doc.id, 'json', e)}
                                                            title="Export JSON"
                                                        >
                                                            JSON
                                                        </button>
                                                    </div>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}
