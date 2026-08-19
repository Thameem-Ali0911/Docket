import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import {
    FileText,
    CheckCircle2,
    AlertTriangle,
    Clock,
    XCircle,
    Download,
    Upload,
    SlidersHorizontal,
    Search,
    X,
    ExternalLink
} from 'lucide-react';
import { apiFetch, clearToken, downloadExport } from '../lib/api';
import AmbientAurora from '../components/ui/AmbientAurora';

/**
 * Dashboard page — "Aurora Obsidian" edition.
 * Features ambient light, glassmorphism, Framer Motion staggered animations,
 * multi-criteria filtering, overview metrics, and single/bulk CSV/JSON export.
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
            if (searchQuery.trim()) {
                const query = searchQuery.toLowerCase();
                const filename = doc.fileUrl ? doc.fileUrl.split('/').pop().toLowerCase() : '';
                const matchesSearch = filename.includes(query) ||
                    String(doc.id).includes(query) ||
                    doc.type.toLowerCase().includes(query) ||
                    (doc.failedReason && doc.failedReason.toLowerCase().includes(query));
                if (!matchesSearch) return false;
            }

            if (typeFilter !== 'ALL' && doc.type !== typeFilter) {
                return false;
            }

            if (statusFilter === 'FLAGGED') {
                if (doc.status !== 'PROCESSED' || doc.anomalyCount <= 0) return false;
            } else if (statusFilter === 'CLEAN') {
                if (doc.status !== 'PROCESSED' || doc.anomalyCount > 0) return false;
            } else if (statusFilter === 'PENDING') {
                if (doc.status !== 'PENDING') return false;
            } else if (statusFilter === 'FAILED') {
                if (doc.status !== 'FAILED') return false;
            }

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

    // Statistics
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
            <div className="min-h-screen flex items-center justify-center relative overflow-hidden" style={{ background: 'var(--color-bg)' }}>
                <AmbientAurora opacity={0.35} />
                <div className="flex flex-col items-center gap-3 z-10">
                    <div className="w-8 h-8 rounded-full border-2 border-t-transparent animate-spin" style={{ borderColor: 'var(--color-aurora-start)', borderTopColor: 'transparent' }} />
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: '14px' }}>Loading workspace…</p>
                </div>
            </div>
        );
    }

    if (loadError) {
        return (
            <div className="min-h-screen flex items-center justify-center px-6 relative overflow-hidden" style={{ background: 'var(--color-bg)' }}>
                <AmbientAurora opacity={0.35} />
                <div className="card p-8 text-center max-w-md w-full z-10">
                    <p className="alert-error mb-5">{loadError}</p>
                    <button className="btn-primary w-full" onClick={loadDashboard}>Retry</button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen relative overflow-x-hidden" style={{ background: 'var(--color-bg)' }}>
            {/* Ambient Aurora Top Glow */}
            <AmbientAurora opacity={0.4} />

            {/* Top navigation bar */}
            <nav className="card relative z-20 flex items-center justify-between px-6 py-3.5"
                 style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none', background: 'rgba(18, 16, 27, 0.85)' }}>
                <div className="flex items-center gap-3">
                    <h2 className="text-xl font-bold tracking-tight">
                        <span className="text-aurora">Docket</span>
                    </h2>
                    {user && (
                        <span className="badge badge-info">{user.workspaceName}</span>
                    )}
                </div>
                <div className="flex items-center gap-4">
                    {user && (
                        <span style={{ color: 'var(--color-text-secondary)', fontSize: '13px' }}>
                            {user.email}
                        </span>
                    )}
                    <button onClick={handleLogout} className="btn-secondary" style={{ padding: '5px 14px', fontSize: '13px' }}>
                        Sign out
                    </button>
                </div>
            </nav>

            {/* Main content */}
            <main className="max-w-6xl mx-auto px-6 py-10 relative z-10">
                {/* Header */}
                <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3 }}
                    className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8"
                >
                    <div>
                        <h1 className="text-3xl font-bold">Dashboard</h1>
                        <p style={{ color: 'var(--color-text-secondary)', marginTop: '4px', fontSize: '14px' }}>
                            Document intelligence, automated template anomaly checking, and data export.
                        </p>
                    </div>
                    <div className="flex flex-wrap gap-3">
                        <button className="btn-secondary" onClick={() => navigate('/templates')}>
                            <SlidersHorizontal size={15} />
                            Manage Templates
                        </button>
                        <button className="btn-primary" onClick={() => navigate('/upload')}>
                            <Upload size={15} />
                            Upload Document
                        </button>
                    </div>
                </motion.div>

                {/* Overview Metrics Cards with Hover-Lift */}
                <div className="grid grid-cols-2 sm:grid-cols-5 gap-3.5 mb-8">
                    <motion.div
                        whileHover={{ y: -2 }}
                        transition={{ duration: 0.15 }}
                        className="card p-4 card-interactive"
                        style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                    >
                        <div className="flex items-center justify-between mb-2">
                            <span className="text-xs font-semibold uppercase tracking-wider text-purple-300">Total</span>
                            <FileText size={16} className="text-purple-400 opacity-80" />
                        </div>
                        <p className="text-2xl font-bold" style={{ color: 'var(--color-text-primary)' }}>{stats.total}</p>
                    </motion.div>

                    <motion.div
                        whileHover={{ y: -2 }}
                        transition={{ duration: 0.15 }}
                        className="card p-4 card-interactive"
                        style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                    >
                        <div className="flex items-center justify-between mb-2">
                            <span className="text-xs font-semibold uppercase tracking-wider text-emerald-300">Processed</span>
                            <CheckCircle2 size={16} className="text-emerald-400 opacity-80" />
                        </div>
                        <p className="text-2xl font-bold text-emerald-400">{stats.processed}</p>
                    </motion.div>

                    <motion.div
                        whileHover={{ y: -2 }}
                        transition={{ duration: 0.15 }}
                        className="card p-4 card-interactive"
                        style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                    >
                        <div className="flex items-center justify-between mb-2">
                            <span className="text-xs font-semibold uppercase tracking-wider text-amber-300">Flagged</span>
                            <AlertTriangle size={16} className="text-amber-400 opacity-80" />
                        </div>
                        <p className="text-2xl font-bold text-amber-400">{stats.flagged}</p>
                    </motion.div>

                    <motion.div
                        whileHover={{ y: -2 }}
                        transition={{ duration: 0.15 }}
                        className="card p-4 card-interactive"
                        style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                    >
                        <div className="flex items-center justify-between mb-2">
                            <span className="text-xs font-semibold uppercase tracking-wider text-cyan-300">Processing</span>
                            <Clock size={16} className="text-cyan-400 opacity-80" />
                        </div>
                        <p className="text-2xl font-bold text-cyan-400">{stats.pending}</p>
                    </motion.div>

                    <motion.div
                        whileHover={{ y: -2 }}
                        transition={{ duration: 0.15 }}
                        className="card p-4 card-interactive"
                        style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                    >
                        <div className="flex items-center justify-between mb-2">
                            <span className="text-xs font-semibold uppercase tracking-wider text-rose-300">Failed</span>
                            <XCircle size={16} className="text-rose-400 opacity-80" />
                        </div>
                        <p className="text-2xl font-bold text-rose-400">{stats.failed}</p>
                    </motion.div>
                </div>

                {/* Filter and Action Bar */}
                <div className="card p-4 mb-6" style={{ background: 'rgba(27, 24, 48, 0.85)' }}>
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
                                    className="input text-sm"
                                    style={{ paddingLeft: '34px' }}
                                />
                                <Search size={15} className="absolute left-2.5 top-3 text-gray-400" />
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
                                    style={{ padding: '6px 12px', fontSize: '13px' }}
                                    title="Reset all filters"
                                >
                                    <X size={13} />
                                    Clear
                                </button>
                            )}
                        </div>

                        {/* Bulk Export Actions */}
                        {documents.length > 0 && (
                            <div className="flex items-center gap-2 border-t md:border-t-0 pt-3 md:pt-0 shrink-0">
                                <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider mr-1">Bulk Export:</span>
                                <button
                                    className="btn-secondary"
                                    onClick={() => handleBulkExport('csv')}
                                    disabled={exporting}
                                    style={{ padding: '5px 12px', fontSize: '12px' }}
                                    title="Export all documents as CSV"
                                >
                                    <Download size={13} />
                                    CSV
                                </button>
                                <button
                                    className="btn-secondary"
                                    onClick={() => handleBulkExport('json')}
                                    disabled={exporting}
                                    style={{ padding: '5px 12px', fontSize: '12px' }}
                                    title="Export all documents as JSON"
                                >
                                    <Download size={13} />
                                    JSON
                                </button>
                            </div>
                        )}
                    </div>
                </div>

                {/* Documents Table */}
                {documents.length === 0 ? (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="card p-12 text-center"
                        style={{ background: 'rgba(27, 24, 48, 0.85)' }}
                    >
                        <div className="w-14 h-14 rounded-full flex items-center justify-center mx-auto mb-4"
                             style={{ background: 'rgba(124, 92, 252, 0.12)', color: 'var(--color-aurora-start)' }}>
                            <FileText size={28} />
                        </div>
                        <h3 className="text-lg font-bold mb-2">No documents yet</h3>
                        <p style={{ color: 'var(--color-text-secondary)', marginBottom: '24px', fontSize: '14px', maxWidth: '420px', margin: '0 auto 24px' }}>
                            Upload your first invoice, contract, or resume to get started with automated extraction and anomaly checking.
                        </p>
                        <button className="btn-primary" onClick={() => navigate('/upload')}>
                            <Upload size={15} />
                            Upload Document
                        </button>
                    </motion.div>
                ) : filteredDocuments.length === 0 ? (
                    <div className="card p-12 text-center" style={{ background: 'rgba(27, 24, 48, 0.85)' }}>
                        <h3 className="text-lg font-bold mb-2">No matching documents</h3>
                        <p style={{ color: 'var(--color-text-secondary)', marginBottom: '20px', fontSize: '14px' }}>
                            No documents matched your search and filter criteria.
                        </p>
                        <button className="btn-secondary" onClick={clearAllFilters}>
                            Clear filters
                        </button>
                    </div>
                ) : (
                    <div className="card overflow-hidden" style={{ background: 'rgba(27, 24, 48, 0.85)' }}>
                        <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr style={{ borderBottom: '1px solid var(--color-border)', backgroundColor: 'var(--color-surface-raised)' }}>
                                        <th className="py-3 px-4 font-semibold text-xs text-gray-400 uppercase tracking-wider">Type</th>
                                        <th className="py-3 px-4 font-semibold text-xs text-gray-400 uppercase tracking-wider">Document / File</th>
                                        <th className="py-3 px-4 font-semibold text-xs text-gray-400 uppercase tracking-wider">Status</th>
                                        <th className="py-3 px-4 font-semibold text-xs text-gray-400 uppercase tracking-wider">Anomaly Flags</th>
                                        <th className="py-3 px-4 font-semibold text-xs text-gray-400 uppercase tracking-wider">Uploaded At</th>
                                        <th className="py-3 px-4 text-right font-semibold text-xs text-gray-400 uppercase tracking-wider">Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredDocuments.map((doc, idx) => {
                                        const filename = doc.fileUrl ? doc.fileUrl.split('/').pop() : 'Document';
                                        return (
                                            <motion.tr
                                                key={doc.id}
                                                initial={{ opacity: 0, y: 6 }}
                                                animate={{ opacity: 1, y: 0 }}
                                                transition={{ duration: 0.25, delay: Math.min(idx * 0.04, 0.4) }}
                                                className="hover:bg-purple-950/20 cursor-pointer transition-colors"
                                                onClick={() => navigate(`/documents/${doc.id}`)}
                                                style={{ borderBottom: '1px solid var(--color-border)' }}
                                            >
                                                <td className="py-3.5 px-4">
                                                    <span className="font-semibold text-xs px-2.5 py-1 rounded"
                                                          style={{ background: 'rgba(124, 92, 252, 0.15)', color: 'var(--color-aurora-start)', border: '1px solid rgba(124, 92, 252, 0.25)' }}>
                                                        {doc.type}
                                                    </span>
                                                </td>
                                                <td className="py-3.5 px-4 text-sm font-medium" style={{ color: 'var(--color-text-primary)' }}>
                                                    <div>
                                                        <span className="hover:text-cyan-400 transition-colors">{filename}</span>
                                                        <p className="text-xs text-gray-500 font-normal">ID: #{doc.id}</p>
                                                    </div>
                                                </td>
                                                <td className="py-3.5 px-4">
                                                    <span className={`badge ${
                                                        doc.status === 'PENDING'
                                                            ? 'badge-warning badge-pulse'
                                                            : doc.status === 'PROCESSED'
                                                            ? 'badge-success'
                                                            : 'badge-danger'
                                                    }`}>
                                                        {doc.status}
                                                    </span>
                                                </td>
                                                <td className="py-3.5 px-4">
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
                                                        <span className="text-xs text-gray-500">—</span>
                                                    )}
                                                </td>
                                                <td className="py-3.5 px-4 text-sm" style={{ color: 'var(--color-text-secondary)' }}>
                                                    {new Date(doc.uploadedAt).toLocaleString()}
                                                </td>
                                                <td className="py-3.5 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                                                    <div className="flex items-center justify-end gap-2">
                                                        <button
                                                            className="text-xs font-semibold hover:underline"
                                                            style={{ color: 'var(--color-aurora-end)' }}
                                                            onClick={() => navigate(`/documents/${doc.id}`)}
                                                        >
                                                            View
                                                        </button>
                                                        <span className="text-gray-600">|</span>
                                                        <button
                                                            className="text-xs text-gray-400 hover:text-white transition-colors"
                                                            onClick={(e) => handleSingleExport(doc.id, 'csv', e)}
                                                            title="Export CSV"
                                                        >
                                                            CSV
                                                        </button>
                                                        <span className="text-gray-600">|</span>
                                                        <button
                                                            className="text-xs text-gray-400 hover:text-white transition-colors"
                                                            onClick={(e) => handleSingleExport(doc.id, 'json', e)}
                                                            title="Export JSON"
                                                        >
                                                            JSON
                                                        </button>
                                                    </div>
                                                </td>
                                            </motion.tr>
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
