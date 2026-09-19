import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'motion/react';
import {
    FileText, CheckCircle2, AlertTriangle, Clock, XCircle,
    Download, Upload, SlidersHorizontal, Search, X, LayoutDashboard, LogOut,
    TrendingUp, TrendingDown, Building2, Copy, DollarSign, Layers, ArrowUpRight, ShieldAlert,
    CreditCard
} from 'lucide-react';
import { apiFetch, clearToken, downloadExport } from '../lib/api';
import AmbientAurora from '../components/ui/AmbientAurora';

/**
 * Dashboard — Neomorphic Apple-style layout.
 * Overview metrics, multi-criteria filtering, staggered table, single/bulk export.
 */
export default function Dashboard() {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [documents, setDocuments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState(null);
    const [exporting, setExporting] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [typeFilter, setTypeFilter] = useState('ALL');
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [dateFilter, setDateFilter] = useState('ALL');
    const [llmUsage, setLlmUsage] = useState(null); // today's LLM call count
    const [trends, setTrends] = useState(null);
    const [activeTab, setActiveTab] = useState('documents'); // 'documents' | 'trends'
    const [vendorSearch, setVendorSearch] = useState('');
    const [vendorFilterAnomaliesOnly, setVendorFilterAnomaliesOnly] = useState(false);

    useEffect(() => { loadDashboard(); }, [navigate]);

    function loadDashboard() {
        setLoading(true);
        setLoadError(null);
        Promise.all([
            apiFetch('/api/auth/me'),
            apiFetch('/api/documents'),
            apiFetch('/api/documents/usage').catch(() => ({ todayLlmUsage: null })),
            apiFetch('/api/documents/trends').catch(() => null),
        ])
            .then(([userData, docsData, usageData, trendsData]) => {
                setUser(userData);
                setDocuments(docsData);
                if (usageData?.todayLlmUsage !== undefined) setLlmUsage(usageData.todayLlmUsage);
                if (trendsData) setTrends(trendsData);
            })
            .catch((err) => {
                if (err.status === 401) { clearToken(); navigate('/login', { replace: true }); return; }
                setLoadError(err.message || 'Failed to load dashboard.');
            })
            .finally(() => setLoading(false));
    }

    function handleLogout() { clearToken(); navigate('/login', { replace: true }); }

    async function handleBulkExport(format) {
        setExporting(true);
        try { await downloadExport(`/api/documents/export?format=${format}`, `docket-workspace-export.${format}`); }
        catch (err) { alert(`Bulk export failed: ${err.message}`); }
        finally { setExporting(false); }
    }

    async function handleSingleExport(docId, format, e) {
        e.stopPropagation();
        try { await downloadExport(`/api/documents/${docId}/export?format=${format}`, `document-${docId}-export.${format}`); }
        catch (err) { alert(`Document export failed: ${err.message}`); }
    }

    const filteredDocuments = useMemo(() => {
        return documents.filter(doc => {
            if (searchQuery.trim()) {
                const q = searchQuery.toLowerCase();
                const fn = doc.fileUrl ? doc.fileUrl.split('/').pop().toLowerCase() : '';
                if (!fn.includes(q) && !String(doc.id).includes(q) && !doc.type.toLowerCase().includes(q)) return false;
            }
            if (typeFilter !== 'ALL' && doc.type !== typeFilter) return false;
            if (statusFilter !== 'ALL') {
                if (statusFilter === 'FLAGGED' && !(doc.status === 'PROCESSED' && doc.anomalyCount > 0)) return false;
                if (statusFilter === 'CLEAN' && !(doc.status === 'PROCESSED' && doc.anomalyCount === 0)) return false;
                if (statusFilter === 'PENDING' && doc.status !== 'PENDING') return false;
                if (statusFilter === 'FAILED' && doc.status !== 'FAILED') return false;
            }
            if (dateFilter !== 'ALL' && doc.uploadedAt) {
                const d = new Date(doc.uploadedAt);
                const now = new Date();
                if (dateFilter === 'TODAY') {
                    if (d.toDateString() !== now.toDateString()) return false;
                } else {
                    const days = dateFilter === '7DAYS' ? 7 : 30;
                    const cutoff = new Date(now); cutoff.setDate(now.getDate() - days);
                    if (d < cutoff) return false;
                }
            }
            return true;
        });
    }, [documents, searchQuery, typeFilter, statusFilter, dateFilter]);

    const filteredVendors = useMemo(() => {
        if (!trends || !trends.vendors) return [];
        return trends.vendors.filter(v => {
            if (vendorFilterAnomaliesOnly && v.anomalyCount === 0) return false;
            if (vendorSearch.trim()) {
                const q = vendorSearch.toLowerCase();
                return v.vendorName.toLowerCase().includes(q);
            }
            return true;
        });
    }, [trends, vendorSearch, vendorFilterAnomaliesOnly]);

    const stats = useMemo(() => ({
        total: documents.length,
        processed: documents.filter(d => d.status === 'PROCESSED').length,
        flagged: documents.filter(d => d.status === 'PROCESSED' && d.anomalyCount > 0).length,
        pending: documents.filter(d => d.status === 'PENDING').length,
        failed: documents.filter(d => d.status === 'FAILED').length,
    }), [documents]);

    const isFiltered = searchQuery !== '' || typeFilter !== 'ALL' || statusFilter !== 'ALL' || dateFilter !== 'ALL';

    function clearAllFilters() {
        setSearchQuery(''); setTypeFilter('ALL'); setStatusFilter('ALL'); setDateFilter('ALL');
    }

    // ---- Metric card config ----
    const metricCards = [
        { label: 'Total', value: stats.total, color: '#7C5CFC', bg: 'rgba(124,92,252,0.14)', icon: FileText },
        { label: 'Processed', value: stats.processed, color: '#34D399', bg: 'rgba(52,211,153,0.12)', icon: CheckCircle2 },
        { label: 'Flagged', value: stats.flagged, color: '#F5A524', bg: 'rgba(245,165,36,0.12)', icon: AlertTriangle },
        { label: 'Processing', value: stats.pending, color: '#22D3EE', bg: 'rgba(34,211,238,0.12)', icon: Clock },
        { label: 'Failed', value: stats.failed, color: '#F65A5A', bg: 'rgba(246,90,90,0.12)', icon: XCircle },
    ];

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center relative overflow-hidden"
                 style={{ background: 'var(--color-bg)' }}>
                <AmbientAurora opacity={0.35} />
                <div className="flex flex-col items-center gap-3 z-10">
                    <div className="w-8 h-8 rounded-full border-2 animate-spin"
                         style={{ borderColor: 'var(--color-aurora-start)', borderTopColor: 'transparent' }} />
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: 14 }}>Loading workspace…</p>
                </div>
            </div>
        );
    }

    if (loadError) {
        return (
            <div className="min-h-screen flex items-center justify-center px-6 relative overflow-hidden"
                 style={{ background: 'var(--color-bg)' }}>
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
            <AmbientAurora opacity={0.35} />

            {/* ── Frosted nav strip (Apple-style) ── */}
            <nav className="nav-frosted sticky top-0 z-30 px-4 sm:px-8 py-0">
                <div className="w-full max-w-[1600px] mx-auto flex items-center justify-between h-[52px]">
                    {/* Brand */}
                    <div className="flex items-center gap-3">
                        <div style={{
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            width: 30, height: 30, borderRadius: 8,
                            background: 'linear-gradient(135deg, rgba(124,92,252,0.25), rgba(34,211,238,0.12))',
                            border: '1px solid rgba(124,92,252,0.30)',
                            boxShadow: 'var(--neo-shadow-sm)',
                        }}>
                            <span style={{
                                fontSize: 13, fontWeight: 800, fontFamily: 'var(--font-heading)',
                                background: 'linear-gradient(135deg, #7C5CFC, #22D3EE)',
                                WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
                            }}>D</span>
                        </div>
                        <span className="text-aurora" style={{ fontSize: 17, fontWeight: 800, letterSpacing: '-0.02em' }}>
                            Docket
                        </span>
                        {user && (
                            <span className="badge badge-info" style={{ fontSize: 10, padding: '2px 8px' }}>
                                {user.workspaceName}
                            </span>
                        )}
                    </div>

                    {/* Right side */}
                    <div className="flex items-center gap-3">
                        {user && (
                            <span style={{ color: 'var(--color-text-disabled)', fontSize: 12 }}>
                                {user.email}
                            </span>
                        )}
                        <button onClick={handleLogout} className="btn-ghost" style={{ fontSize: 12, gap: 5 }}>
                            <LogOut size={13} />
                            Sign out
                        </button>
                    </div>
                </div>
            </nav>

            {/* ── Main content ── */}
            <main className="w-full max-w-[1600px] mx-auto px-4 sm:px-8 py-6 relative z-10">

                {/* Page header */}
                <motion.div
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
                    className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-6"
                >
                    <div>
                        <div className="flex items-center gap-2.5 mb-1">
                            <LayoutDashboard size={18} style={{ color: 'var(--color-aurora-start)' }} />
                            <h1 style={{ fontSize: 22, fontWeight: 800, letterSpacing: '-0.025em' }}>Dashboard</h1>
                        </div>
                        <p style={{ color: 'var(--color-text-secondary)', fontSize: 13.5, marginLeft: 28 }}>
                            Document intelligence · Anomaly detection · Data export
                        </p>
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            className="btn-secondary"
                            onClick={() => navigate('/templates')}
                            style={{ padding: '7px 16px', fontSize: 13 }}
                        >
                            <SlidersHorizontal size={13} />
                            Templates
                        </button>
                        <button
                            className="btn-secondary"
                            onClick={() => navigate('/billing')}
                            style={{ padding: '7px 16px', fontSize: 13 }}
                        >
                            <CreditCard size={13} />
                            Billing
                        </button>
                        <button
                            className="btn-primary"
                            onClick={() => navigate('/upload')}
                            style={{ padding: '7px 18px', fontSize: 13 }}
                        >
                            <Upload size={13} />
                            Upload
                        </button>
                    </div>
                </motion.div>

                {/* ── Metric cards ── */}
                <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 mb-3">
                    {metricCards.map(({ label, value, color, bg, icon: Icon }, i) => (
                        <motion.div
                            key={label}
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ duration: 0.3, delay: i * 0.05, ease: [0.22, 1, 0.36, 1] }}
                            className="card-neo-sm p-4 cursor-default"
                            style={{ background: 'var(--color-surface)' }}
                        >
                            <div className="flex items-center justify-between mb-3">
                                <span style={{
                                    fontSize: 10.5, fontWeight: 700, textTransform: 'uppercase',
                                    letterSpacing: '0.06em', color: color,
                                }}>
                                    {label}
                                </span>
                                <div className="stat-icon" style={{ background: bg, color: color }}>
                                    <Icon size={14} />
                                </div>
                            </div>
                            <p style={{ fontSize: 28, fontWeight: 800, color, letterSpacing: '-0.03em', lineHeight: 1 }}>
                                {value}
                            </p>
                        </motion.div>
                    ))}
                </div>

                {/* ── LLM Usage Bar ── */}
                {llmUsage !== null && (
                    <motion.div
                        initial={{ opacity: 0, y: 6 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.3, duration: 0.25 }}
                        className="card-neo-sm px-4 py-2.5 mb-4 flex items-center gap-4"
                        style={{ background: 'var(--color-surface)' }}
                        title={`${llmUsage} AI operations used today out of 100 daily budget`}
                    >
                        <span style={{ fontSize: 10.5, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', color: '#7C5CFC', whiteSpace: 'nowrap' }}>
                            AI Budget
                        </span>
                        <div style={{ flex: 1, height: 5, background: 'rgba(255,255,255,0.06)', borderRadius: 999, overflow: 'hidden' }}>
                            <div style={{
                                height: '100%',
                                width: `${Math.min((llmUsage / 100) * 100, 100)}%`,
                                borderRadius: 999,
                                background: llmUsage >= 90
                                    ? 'linear-gradient(90deg, #F65A5A, #F5A524)'
                                    : llmUsage >= 70
                                    ? 'linear-gradient(90deg, #F5A524, #22D3EE)'
                                    : 'linear-gradient(90deg, #7C5CFC, #22D3EE)',
                                transition: 'width 0.5s ease',
                            }} />
                        </div>
                        <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', whiteSpace: 'nowrap' }}>
                            {llmUsage} / 100 today
                        </span>
                    </motion.div>
                )}

                {/* ── View Switcher Tabs ── */}
                <div className="flex items-center gap-2 mb-4 p-1 rounded-xl w-fit" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.07)' }}>
                    <button
                        onClick={() => setActiveTab('documents')}
                        className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-2 transition-all ${
                            activeTab === 'documents' ? 'text-white' : 'text-slate-400 hover:text-white'
                        }`}
                        style={activeTab === 'documents' ? {
                            background: 'linear-gradient(135deg, rgba(124,92,252,0.3), rgba(34,211,238,0.15))',
                            border: '1px solid rgba(124,92,252,0.4)',
                            boxShadow: 'var(--neo-shadow-sm)',
                        } : {}}
                    >
                        <FileText size={13} style={{ color: 'var(--color-aurora-start)' }} />
                        Documents ({documents.length})
                    </button>
                    <button
                        onClick={() => setActiveTab('trends')}
                        className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-2 transition-all ${
                            activeTab === 'trends' ? 'text-white' : 'text-slate-400 hover:text-white'
                        }`}
                        style={activeTab === 'trends' ? {
                            background: 'linear-gradient(135deg, rgba(34,211,238,0.25), rgba(124,92,252,0.2))',
                            border: '1px solid rgba(34,211,238,0.4)',
                            boxShadow: 'var(--neo-shadow-sm)',
                        } : {}}
                    >
                        <TrendingUp size={13} style={{ color: 'var(--color-aurora-end)' }} />
                        Vendor Trends & Cross-Doc Intelligence
                        {trends && trends.totalComparativeAnomalies > 0 && (
                            <span
                                className="px-1.5 py-0.5 rounded-full text-[10px] font-bold"
                                style={{
                                    background: 'rgba(245, 165, 36, 0.2)',
                                    color: '#F5A524',
                                    border: '1px solid rgba(245, 165, 36, 0.4)',
                                }}
                            >
                                {trends.totalComparativeAnomalies} alert{trends.totalComparativeAnomalies > 1 ? 's' : ''}
                            </span>
                        )}
                    </button>
                </div>

                {activeTab === 'documents' ? (
                <>
                {/* ── Filter + Export bar ── */}
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 0.28, duration: 0.25 }}
                    className="card px-4 py-3 mb-4"
                    style={{ background: 'var(--color-surface)' }}
                >
                    <div className="flex flex-col md:flex-row gap-2.5 justify-between items-stretch md:items-center">
                        {/* Filters */}
                        <div className="flex flex-wrap flex-1 gap-2 items-center">
                            {/* Search */}
                            <div style={{ position: 'relative', minWidth: 180, flex: 1 }}>
                                <input
                                    type="text"
                                    placeholder="Search by filename, ID, type…"
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                    className="input"
                                    style={{ paddingLeft: 34, paddingTop: 7, paddingBottom: 7, fontSize: 13 }}
                                />
                                <Search size={13} style={{
                                    position: 'absolute', left: 11, top: '50%', transform: 'translateY(-50%)',
                                    color: 'var(--color-text-disabled)',
                                }} />
                            </div>

                            <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}
                                className="input w-auto" style={{ padding: '7px 10px', fontSize: 13 }}>
                                <option value="ALL">All Types</option>
                                <option value="INVOICE">Invoices</option>
                                <option value="CONTRACT">Contracts</option>
                                <option value="RESUME">Resumes</option>
                                <option value="KYC_FORM">KYC Forms</option>
                            </select>

                            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}
                                className="input w-auto" style={{ padding: '7px 10px', fontSize: 13 }}>
                                <option value="ALL">All Statuses</option>
                                <option value="FLAGGED">⚠️ Flagged</option>
                                <option value="CLEAN">✅ Clean</option>
                                <option value="PENDING">Pending</option>
                                <option value="FAILED">Failed</option>
                            </select>

                            <select value={dateFilter} onChange={(e) => setDateFilter(e.target.value)}
                                className="input w-auto" style={{ padding: '7px 10px', fontSize: 13 }}>
                                <option value="ALL">All Time</option>
                                <option value="TODAY">Today</option>
                                <option value="7DAYS">Last 7 Days</option>
                                <option value="30DAYS">Last 30 Days</option>
                            </select>

                            <AnimatePresence>
                                {isFiltered && (
                                    <motion.button
                                        initial={{ opacity: 0, scale: 0.85 }}
                                        animate={{ opacity: 1, scale: 1 }}
                                        exit={{ opacity: 0, scale: 0.85 }}
                                        transition={{ duration: 0.15 }}
                                        onClick={clearAllFilters}
                                        className="btn-ghost"
                                        style={{ fontSize: 12, padding: '5px 10px' }}
                                    >
                                        <X size={12} /> Clear
                                    </motion.button>
                                )}
                            </AnimatePresence>
                        </div>

                        {/* Bulk export */}
                        {documents.length > 0 && (
                            <div className="flex items-center gap-1.5 shrink-0 border-t md:border-t-0 pt-2 md:pt-0">
                                <span style={{ fontSize: 10.5, fontWeight: 700, color: 'var(--color-text-disabled)', textTransform: 'uppercase', letterSpacing: '0.05em', marginRight: 2 }}>
                                    Export All:
                                </span>
                                <button className="btn-secondary" onClick={() => handleBulkExport('csv')}
                                    disabled={exporting} style={{ padding: '5px 11px', fontSize: 12, gap: 5 }}>
                                    <Download size={12} /> CSV
                                </button>
                                <button className="btn-secondary" onClick={() => handleBulkExport('json')}
                                    disabled={exporting} style={{ padding: '5px 11px', fontSize: 12, gap: 5 }}>
                                    <Download size={12} /> JSON
                                </button>
                            </div>
                        )}
                    </div>
                </motion.div>

                {/* ── Documents table ── */}
                {documents.length === 0 ? (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="card p-10 text-center"
                        style={{ background: 'var(--color-surface)' }}
                    >
                        <div style={{
                            width: 52, height: 52, borderRadius: 14, display: 'flex',
                            alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px',
                            background: 'rgba(124,92,252,0.12)', color: 'var(--color-aurora-start)',
                            boxShadow: 'var(--neo-shadow-sm)',
                        }}>
                            <FileText size={24} />
                        </div>
                        <h3 style={{ marginBottom: 8 }}>No documents yet</h3>
                        <p style={{ color: 'var(--color-text-secondary)', fontSize: 14, maxWidth: 360, margin: '0 auto 20px' }}>
                            Upload your first invoice, contract, or resume to begin.
                        </p>
                        <button className="btn-primary" onClick={() => navigate('/upload')}
                            style={{ fontSize: 13, padding: '8px 20px' }}>
                            <Upload size={14} /> Upload Document
                        </button>
                    </motion.div>
                ) : filteredDocuments.length === 0 ? (
                    <div className="card p-8 text-center" style={{ background: 'var(--color-surface)' }}>
                        <h3 style={{ marginBottom: 8 }}>No matches</h3>
                        <p style={{ color: 'var(--color-text-secondary)', fontSize: 14, marginBottom: 16 }}>
                            No documents matched your current filters.
                        </p>
                        <button className="btn-secondary" onClick={clearAllFilters} style={{ fontSize: 13 }}>
                            Clear filters
                        </button>
                    </div>
                ) : (
                    <div className="card overflow-hidden" style={{ background: 'var(--color-surface)' }}>
                        <div className="overflow-x-auto">
                            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                <thead>
                                    <tr style={{
                                        borderBottom: '1px solid var(--color-border)',
                                        background: 'var(--color-bg)',
                                    }}>
                                        {['Type', 'Document / File', 'Status', 'Anomaly Flags', 'Uploaded At', 'Actions'].map((h, i) => (
                                            <th key={h} style={{
                                                padding: '10px 14px',
                                                fontSize: 10.5, fontWeight: 700,
                                                color: 'var(--color-text-disabled)',
                                                textTransform: 'uppercase', letterSpacing: '0.06em',
                                                textAlign: i === 5 ? 'right' : 'left',
                                            }}>
                                                {h}
                                            </th>
                                        ))}
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredDocuments.map((doc, idx) => {
                                        const filename = doc.fileUrl ? doc.fileUrl.split('/').pop() : 'Document';
                                        return (
                                            <motion.tr
                                                key={doc.id}
                                                initial={{ opacity: 0, x: -6 }}
                                                animate={{ opacity: 1, x: 0 }}
                                                transition={{ duration: 0.22, delay: Math.min(idx * 0.035, 0.35), ease: [0.22, 1, 0.36, 1] }}
                                                onClick={() => navigate(`/documents/${doc.id}`)}
                                                style={{
                                                    borderBottom: '1px solid var(--color-border)',
                                                    cursor: 'pointer',
                                                    transition: 'background 0.15s ease',
                                                }}
                                                whileHover={{ backgroundColor: 'rgba(124,92,252,0.06)' }}
                                            >
                                                {/* Type chip */}
                                                <td style={{ padding: '11px 14px' }}>
                                                    <span style={{
                                                        fontSize: 10.5, fontWeight: 700, padding: '3px 9px',
                                                        borderRadius: 9999,
                                                        background: 'rgba(124,92,252,0.13)',
                                                        color: 'var(--color-aurora-start)',
                                                        border: '1px solid rgba(124,92,252,0.22)',
                                                        boxShadow: 'var(--neo-shadow-sm)',
                                                        textTransform: 'uppercase', letterSpacing: '0.05em',
                                                    }}>
                                                        {doc.type}
                                                    </span>
                                                </td>

                                                {/* Filename */}
                                                <td style={{ padding: '11px 14px' }}>
                                                    <span style={{ fontSize: 13.5, fontWeight: 600, color: 'var(--color-text-primary)', display: 'block' }}>
                                                        {filename}
                                                    </span>
                                                    <span style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>
                                                        #{doc.id}
                                                    </span>
                                                </td>

                                                {/* Status badge */}
                                                <td style={{ padding: '11px 14px' }}>
                                                    <span className={`badge ${
                                                        doc.status === 'PENDING' ? 'badge-warning badge-pulse'
                                                        : doc.status === 'PROCESSED' ? 'badge-success'
                                                        : 'badge-danger'
                                                    }`}>
                                                        {doc.status}
                                                    </span>
                                                </td>

                                                {/* Anomaly */}
                                                <td style={{ padding: '11px 14px' }}>
                                                    {doc.status === 'PROCESSED' ? (
                                                        doc.anomalyCount > 0
                                                            ? <span className="badge badge-warning">⚠️ {doc.anomalyCount} dev{doc.anomalyCount > 1 ? 's' : ''}</span>
                                                            : <span className="badge badge-success">✅ Clean</span>
                                                    ) : (
                                                        <span style={{ color: 'var(--color-text-disabled)', fontSize: 12 }}>—</span>
                                                    )}
                                                </td>

                                                {/* Date */}
                                                <td style={{ padding: '11px 14px', fontSize: 12, color: 'var(--color-text-secondary)' }}>
                                                    {new Date(doc.uploadedAt).toLocaleString()}
                                                </td>

                                                {/* Actions */}
                                                <td style={{ padding: '11px 14px', textAlign: 'right' }}
                                                    onClick={(e) => e.stopPropagation()}>
                                                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 8 }}>
                                                        <button
                                                            onClick={() => navigate(`/documents/${doc.id}`)}
                                                            style={{
                                                                fontSize: 12, fontWeight: 700, background: 'none', border: 'none',
                                                                color: 'var(--color-aurora-end)', cursor: 'pointer',
                                                                transition: 'opacity 0.15s',
                                                            }}
                                                            onMouseOver={e => e.currentTarget.style.opacity = '0.7'}
                                                            onMouseOut={e => e.currentTarget.style.opacity = '1'}
                                                        >
                                                            View
                                                        </button>
                                                        <span style={{ color: 'var(--color-border-hover)', fontSize: 14 }}>|</span>
                                                        <button className="btn-ghost" style={{ fontSize: 11, padding: '2px 7px', gap: 4 }}
                                                            onClick={(e) => handleSingleExport(doc.id, 'csv', e)}>
                                                            <Download size={11} /> CSV
                                                        </button>
                                                        <button className="btn-ghost" style={{ fontSize: 11, padding: '2px 7px', gap: 4 }}
                                                            onClick={(e) => handleSingleExport(doc.id, 'json', e)}>
                                                            <Download size={11} /> JSON
                                                        </button>
                                                    </div>
                                                </td>
                                            </motion.tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>

                        {/* Footer row count */}
                        <div style={{
                            padding: '10px 14px',
                            borderTop: '1px solid var(--color-border)',
                            fontSize: 11.5,
                            color: 'var(--color-text-disabled)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                        }}>
                            <span>
                                Showing <b style={{ color: 'var(--color-text-secondary)' }}>{filteredDocuments.length}</b> of{' '}
                                <b style={{ color: 'var(--color-text-secondary)' }}>{documents.length}</b> documents
                            </span>
                            {isFiltered && (
                                <button className="btn-ghost" onClick={clearAllFilters} style={{ fontSize: 11, padding: '2px 8px' }}>
                                    <X size={10} /> Clear filters
                                </button>
                            )}
                        </div>
                    </div>
                )}
                </>
                ) : (
                /* ── Vendor Trends View ── */
                <div className="space-y-4">
                    {/* Top Trends Metrics */}
                    <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                        <div className="card-neo-sm p-4" style={{ background: 'var(--color-surface)' }}>
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-[10px] font-bold uppercase tracking-wider text-purple-400">Tracked Vendors</span>
                                <Building2 size={14} className="text-purple-400" />
                            </div>
                            <p className="text-2xl font-bold text-white">{trends?.totalVendors || 0}</p>
                        </div>
                        <div className="card-neo-sm p-4" style={{ background: 'var(--color-surface)' }}>
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-[10px] font-bold uppercase tracking-wider text-cyan-400">Invoices Analyzed</span>
                                <FileText size={14} className="text-cyan-400" />
                            </div>
                            <p className="text-2xl font-bold text-white">{trends?.totalInvoicesAnalyzed || 0}</p>
                        </div>
                        <div className="card-neo-sm p-4" style={{ background: 'var(--color-surface)' }}>
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-[10px] font-bold uppercase tracking-wider text-amber-400">Price Surges (&gt;50%)</span>
                                <TrendingUp size={14} className="text-amber-400" />
                            </div>
                            <p className="text-2xl font-bold text-amber-400">{trends?.priceSurgesCount || 0}</p>
                        </div>
                        <div className="card-neo-sm p-4" style={{ background: 'var(--color-surface)' }}>
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-[10px] font-bold uppercase tracking-wider text-rose-400">Duplicate Invoices</span>
                                <Copy size={14} className="text-rose-400" />
                            </div>
                            <p className="text-2xl font-bold text-rose-400">{trends?.duplicateInvoicesCount || 0}</p>
                        </div>
                        <div className="card-neo-sm p-4" style={{ background: 'var(--color-surface)' }}>
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-400">Total Comparative Alerts</span>
                                <ShieldAlert size={14} className="text-emerald-400" />
                            </div>
                            <p className="text-2xl font-bold text-white">{trends?.totalComparativeAnomalies || 0}</p>
                        </div>
                    </div>

                    {/* Filter / Search Bar */}
                    <div className="card px-4 py-3 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3" style={{ background: 'var(--color-surface)' }}>
                        <div className="relative flex-1 max-w-md">
                            <input
                                type="text"
                                placeholder="Filter vendors by name..."
                                value={vendorSearch}
                                onChange={(e) => setVendorSearch(e.target.value)}
                                className="input w-full"
                                style={{ paddingLeft: 34, paddingTop: 7, paddingBottom: 7, fontSize: 13 }}
                            />
                            <Search size={13} style={{ position: 'absolute', left: 11, top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-disabled)' }} />
                        </div>
                        <div className="flex items-center gap-2">
                            <button
                                onClick={() => setVendorFilterAnomaliesOnly(false)}
                                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                                    !vendorFilterAnomaliesOnly ? 'bg-purple-600/30 text-white border border-purple-500/40' : 'text-slate-400 hover:text-white'
                                }`}
                            >
                                All ({trends?.vendors?.length || 0})
                            </button>
                            <button
                                onClick={() => setVendorFilterAnomaliesOnly(true)}
                                className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
                                    vendorFilterAnomaliesOnly ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' : 'text-slate-400 hover:text-white'
                                }`}
                            >
                                <AlertTriangle size={12} className="text-amber-400" />
                                Flagged Only ({trends?.vendors?.filter(v => v.anomalyCount > 0).length || 0})
                            </button>
                        </div>
                    </div>

                    {/* Vendor Trends Table */}
                    {filteredVendors.length === 0 ? (
                        <div className="card p-12 text-center" style={{ background: 'var(--color-surface)' }}>
                            <Building2 size={36} className="mx-auto mb-3 opacity-30 text-slate-400" />
                            <h3 className="text-base font-bold text-white mb-1">No vendor intelligence found</h3>
                            <p className="text-xs text-slate-400 max-w-sm mx-auto">
                                Processed invoice extractions will automatically populate vendor trends, spend curves, and cross-document anomaly detection.
                            </p>
                        </div>
                    ) : (
                        <div className="card overflow-hidden" style={{ background: 'var(--color-surface)', border: '1px solid var(--color-border)' }}>
                            <div className="overflow-x-auto">
                                <table className="w-full text-left" style={{ borderCollapse: 'collapse' }}>
                                    <thead>
                                        <tr style={{ background: 'rgba(255,255,255,0.02)', borderBottom: '1px solid var(--color-border)', fontSize: 11, color: 'var(--color-text-disabled)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                                            <th className="p-3.5">Vendor</th>
                                            <th className="p-3.5">Invoices</th>
                                            <th className="p-3.5">Total Spend</th>
                                            <th className="p-3.5">Avg Ticket</th>
                                            <th className="p-3.5">Price Range</th>
                                            <th className="p-3.5">Latest Invoice</th>
                                            <th className="p-3.5">Price Trend</th>
                                            <th className="p-3.5">Anomalies</th>
                                            <th className="p-3.5 text-right">Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {filteredVendors.map((v) => {
                                            const hasSpike = v.trendPercentage && v.trendPercentage >= 50;
                                            const hasDrop = v.trendPercentage && v.trendPercentage <= -40;
                                            return (
                                                <tr
                                                    key={v.vendorName}
                                                    style={{ borderBottom: '1px solid var(--color-border)', transition: 'background 0.15s' }}
                                                    className="hover:bg-white/[0.02]"
                                                >
                                                    <td className="p-3.5 font-medium text-white flex items-center gap-2">
                                                        <Building2 size={14} className="text-purple-400 shrink-0" />
                                                        <span>{v.vendorName}</span>
                                                    </td>
                                                    <td className="p-3.5 text-xs text-slate-300">{v.documentCount}</td>
                                                    <td className="p-3.5 text-xs font-semibold text-white">${v.totalSpend.toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
                                                    <td className="p-3.5 text-xs text-slate-300">${v.averageAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })}</td>
                                                    <td className="p-3.5 text-xs text-slate-400">${v.minAmount.toFixed(0)} – ${v.maxAmount.toFixed(0)}</td>
                                                    <td className="p-3.5 text-xs text-slate-300">
                                                        {v.latestAmount ? `$${v.latestAmount.toFixed(2)}` : '—'}
                                                        {v.latestInvoiceDate && <span className="block text-[10px] text-slate-500">{v.latestInvoiceDate}</span>}
                                                    </td>
                                                    <td className="p-3.5 text-xs">
                                                        {v.trendPercentage === null ? (
                                                            <span className="text-[11px] text-slate-500">Baseline (1 inv)</span>
                                                        ) : hasSpike ? (
                                                            <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-500/20 text-amber-300 border border-amber-500/40 flex items-center gap-1 w-fit">
                                                                <TrendingUp size={11} /> +{v.trendPercentage}% Surge
                                                            </span>
                                                        ) : hasDrop ? (
                                                            <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 flex items-center gap-1 w-fit">
                                                                <TrendingDown size={11} /> {v.trendPercentage}% Drop
                                                            </span>
                                                        ) : (
                                                            <span className="text-[11px] text-slate-300">
                                                                {v.trendPercentage > 0 ? `+${v.trendPercentage}%` : `${v.trendPercentage}%`}
                                                            </span>
                                                        )}
                                                    </td>
                                                    <td className="p-3.5 text-xs">
                                                        {v.anomalyCount > 0 ? (
                                                            <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-rose-500/20 text-rose-300 border border-rose-500/40 flex items-center gap-1 w-fit">
                                                                <AlertTriangle size={11} /> {v.anomalyCount} flag{v.anomalyCount > 1 ? 's' : ''}
                                                            </span>
                                                        ) : (
                                                            <span className="text-emerald-400 text-[11px] font-medium flex items-center gap-1">
                                                                <CheckCircle2 size={11} /> Healthy
                                                            </span>
                                                        )}
                                                    </td>
                                                    <td className="p-3.5 text-right text-xs">
                                                        <button
                                                            onClick={() => {
                                                                setSearchQuery(v.vendorName);
                                                                setTypeFilter('INVOICE');
                                                                setActiveTab('documents');
                                                            }}
                                                            className="text-cyan-400 hover:text-cyan-300 font-semibold flex items-center gap-1 ml-auto"
                                                        >
                                                            Filter Invoices <ArrowUpRight size={12} />
                                                        </button>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    )}
                </div>
                )}
            </main>
        </div>
    );
}
