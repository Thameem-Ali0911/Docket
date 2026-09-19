import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import {
    CreditCard, CheckCircle2, AlertTriangle, XCircle, Zap, ShieldCheck,
    ArrowRight, Clock, Download, RefreshCw, FileText, SlidersHorizontal,
    Upload, LayoutDashboard, LogOut, Copy, Check, Sparkles, Receipt
} from 'lucide-react';
import { apiFetch, clearToken } from '../lib/api';
import AmbientAurora from '../components/ui/AmbientAurora';

export default function Billing() {
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [subscription, setSubscription] = useState(null);
    const [invoices, setInvoices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);
    const [error, setError] = useState(null);
    const [toastMessage, setToastMessage] = useState(null);

    // Webhook simulator form state
    const [webhookEvent, setWebhookEvent] = useState('invoice.payment_succeeded');
    const [webhookTier, setWebhookTier] = useState('PRO');
    const [webhookResult, setWebhookResult] = useState(null);
    const [copiedId, setCopiedId] = useState(null);

    useEffect(() => {
        loadBillingData();
    }, [navigate]);

    function loadBillingData() {
        setLoading(true);
        setError(null);
        Promise.all([
            apiFetch('/api/auth/me'),
            apiFetch('/api/billing/subscription'),
            apiFetch('/api/billing/invoices').catch(() => [])
        ])
            .then(([userData, subData, invData]) => {
                setUser(userData);
                setSubscription(subData);
                setInvoices(invData || []);
            })
            .catch((err) => {
                if (err.status === 401) {
                    clearToken();
                    navigate('/login', { replace: true });
                    return;
                }
                setError(err.message || 'Failed to load billing data.');
            })
            .finally(() => setLoading(false));
    }

    function handleLogout() {
        clearToken();
        navigate('/login', { replace: true });
    }

    async function handleUpgrade(planTier) {
        setActionLoading(true);
        setError(null);
        try {
            const updated = await apiFetch('/api/billing/upgrade', {
                method: 'POST',
                body: JSON.stringify({ planTier })
            });
            setSubscription(updated);
            showToast(`Successfully switched to ${updated.planDisplayName}!`);
            const invData = await apiFetch('/api/billing/invoices').catch(() => []);
            setInvoices(invData || []);
        } catch (err) {
            setError(err.message || 'Failed to change subscription.');
        } finally {
            setActionLoading(false);
        }
    }

    async function handleSimulateWebhook(e) {
        e.preventDefault();
        setActionLoading(true);
        setWebhookResult(null);
        try {
            const updated = await apiFetch('/api/billing/webhook/simulate', {
                method: 'POST',
                body: JSON.stringify({
                    eventType: webhookEvent,
                    planTier: webhookEvent === 'customer.subscription.updated' ? webhookTier : null
                })
            });
            setSubscription(updated);
            setWebhookResult({
                success: true,
                event: webhookEvent,
                timestamp: new Date().toLocaleTimeString(),
                message: `Processed simulated webhook: ${webhookEvent}. Subscription is now ${updated.subscriptionStatus} on ${updated.planDisplayName}.`
            });
            showToast(`Simulated webhook '${webhookEvent}' dispatched successfully!`);
            const invData = await apiFetch('/api/billing/invoices').catch(() => []);
            setInvoices(invData || []);
        } catch (err) {
            setWebhookResult({
                success: false,
                event: webhookEvent,
                timestamp: new Date().toLocaleTimeString(),
                message: err.message || 'Webhook simulation failed.'
            });
        } finally {
            setActionLoading(false);
        }
    }

    function showToast(msg) {
        setToastMessage(msg);
        setTimeout(() => setToastMessage(null), 4000);
    }

    function copyToClipboard(text, idKey) {
        if (!text) return;
        navigator.clipboard.writeText(text);
        setCopiedId(idKey);
        setTimeout(() => setCopiedId(null), 2000);
    }

    const docPercent = subscription
        ? Math.min(100, Math.round((subscription.documentsUsedThisPeriod / Math.max(1, subscription.monthlyDocumentLimit)) * 100))
        : 0;

    const llmPercent = subscription
        ? Math.min(100, Math.round((subscription.dailyLlmUsage / Math.max(1, subscription.dailyLlmBudget)) * 100))
        : 0;

    const statusBadge = (status) => {
        const s = (status || 'ACTIVE').toUpperCase();
        if (s === 'ACTIVE') {
            return (
                <span className="badge badge-accent flex items-center gap-1.5" style={{ fontSize: 11 }}>
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                    ACTIVE
                </span>
            );
        }
        if (s === 'PAST_DUE') {
            return (
                <span className="badge badge-warning flex items-center gap-1.5" style={{ fontSize: 11 }}>
                    <AlertTriangle size={12} />
                    PAST DUE
                </span>
            );
        }
        return (
            <span className="badge badge-danger flex items-center gap-1.5" style={{ fontSize: 11 }}>
                <XCircle size={12} />
                {s}
            </span>
        );
    };

    return (
        <div className="min-h-screen relative flex flex-col" style={{ background: 'var(--color-bg)' }}>
            <AmbientAurora />

            {/* ── Top Navigation Bar ── */}
            <nav
                className="w-full border-b sticky top-0 z-30 backdrop-blur-md"
                style={{
                    background: 'rgba(27, 24, 48, 0.85)',
                    borderColor: 'var(--color-border)'
                }}
            >
                <div className="w-full max-w-[1600px] mx-auto px-4 sm:px-8 h-14 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div
                            onClick={() => navigate('/dashboard')}
                            className="flex items-center gap-2 cursor-pointer group"
                        >
                            <div
                                className="w-8 h-8 rounded-lg flex items-center justify-center font-bold text-white text-base shadow-sm group-hover:scale-105 transition-transform"
                                style={{
                                    background: 'linear-gradient(135deg, var(--color-aurora-start), var(--color-aurora-end))'
                                }}
                            >
                                D
                            </div>
                            <span style={{ fontSize: 18, fontWeight: 700, letterSpacing: '-0.02em' }}>Docket</span>
                        </div>
                        {user && (
                            <span
                                className="hidden sm:inline-flex items-center px-2 py-0.5 rounded text-xs"
                                style={{
                                    background: 'var(--color-surface-raised)',
                                    color: 'var(--color-text-secondary)',
                                    border: '1px solid var(--color-border)'
                                }}
                            >
                                {user.workspaceName}
                            </span>
                        )}
                    </div>

                    <div className="flex items-center gap-2 sm:gap-3">
                        <button
                            onClick={() => navigate('/dashboard')}
                            className="btn-ghost"
                            style={{ fontSize: 12.5, gap: 5 }}
                        >
                            <LayoutDashboard size={14} />
                            <span className="hidden sm:inline">Dashboard</span>
                        </button>
                        <button
                            onClick={() => navigate('/templates')}
                            className="btn-ghost"
                            style={{ fontSize: 12.5, gap: 5 }}
                        >
                            <SlidersHorizontal size={14} />
                            <span className="hidden sm:inline">Templates</span>
                        </button>
                        <button
                            className="btn-ghost"
                            style={{
                                fontSize: 12.5,
                                gap: 5,
                                color: 'var(--color-aurora-end)',
                                background: 'rgba(34, 211, 238, 0.1)',
                                border: '1px solid rgba(34, 211, 238, 0.3)'
                            }}
                        >
                            <CreditCard size={14} />
                            <span>Billing</span>
                        </button>
                        <button
                            onClick={() => navigate('/upload')}
                            className="btn-primary"
                            style={{ padding: '6px 14px', fontSize: 12.5, gap: 5 }}
                        >
                            <Upload size={13} />
                            <span className="hidden sm:inline">Upload</span>
                        </button>

                        <div className="h-4 w-px bg-white/10 mx-1 hidden sm:block" />

                        {user && (
                            <span style={{ color: 'var(--color-text-disabled)', fontSize: 12 }} className="hidden md:inline">
                                {user.email}
                            </span>
                        )}
                        <button onClick={handleLogout} className="btn-ghost" style={{ fontSize: 12, gap: 5 }}>
                            <LogOut size={13} />
                            <span className="hidden sm:inline">Sign out</span>
                        </button>
                    </div>
                </div>
            </nav>

            {/* ── Toast Notification ── */}
            {toastMessage && (
                <div
                    className="fixed bottom-6 right-6 z-50 px-4 py-3 rounded-lg shadow-xl flex items-center gap-2.5 text-sm text-white"
                    style={{
                        background: 'linear-gradient(135deg, #1e1b4b, #0f172a)',
                        border: '1px solid var(--color-aurora-start)',
                        boxShadow: '0 8px 30px rgba(124, 92, 252, 0.3)'
                    }}
                >
                    <Sparkles size={16} style={{ color: 'var(--color-aurora-end)' }} />
                    <span>{toastMessage}</span>
                </div>
            )}

            {/* ── Main Content Container ── */}
            <main className="w-full max-w-[1400px] mx-auto px-4 sm:px-8 py-8 relative z-10 flex-1">
                
                {/* Header Title */}
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
                    <div>
                        <div className="flex items-center gap-2.5 mb-1.5">
                            <CreditCard size={22} style={{ color: 'var(--color-aurora-end)' }} />
                            <h1 style={{ fontSize: 24, fontWeight: 800, letterSpacing: '-0.02em' }}>
                                Subscription & Billing
                            </h1>
                            <span
                                className="px-2.5 py-0.5 rounded-full text-xs font-semibold tracking-wide"
                                style={{
                                    background: 'rgba(52, 211, 153, 0.15)',
                                    color: '#34D399',
                                    border: '1px solid rgba(52, 211, 153, 0.3)'
                                }}
                            >
                                STRIPE TEST MODE
                            </span>
                        </div>
                        <p style={{ color: 'var(--color-text-secondary)', fontSize: 14 }}>
                            Manage your workspace subscription tier, monitor usage quotas, and simulate Stripe webhook lifecycle events.
                        </p>
                    </div>

                    <button
                        onClick={loadBillingData}
                        disabled={loading}
                        className="btn-secondary"
                        style={{ padding: '7px 16px', fontSize: 13, gap: 6 }}
                    >
                        <RefreshCw size={13} className={loading ? 'animate-spin' : ''} />
                        Refresh
                    </button>
                </div>

                {error && (
                    <div
                        className="p-4 rounded-xl mb-6 flex items-start gap-3"
                        style={{
                            background: 'rgba(246, 90, 90, 0.1)',
                            border: '1px solid rgba(246, 90, 90, 0.3)',
                            color: 'var(--color-danger)'
                        }}
                    >
                        <AlertTriangle size={18} className="shrink-0 mt-0.5" />
                        <div className="text-sm">{error}</div>
                    </div>
                )}

                {/* ── Usage & Quotas Cards ── */}
                {subscription && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-5 mb-10">
                        {/* Monthly Document Quota Card */}
                        <div
                            className="card-neo p-6 relative overflow-hidden"
                            style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}
                        >
                            <div className="flex items-center justify-between mb-4">
                                <div className="flex items-center gap-2">
                                    <FileText size={18} style={{ color: 'var(--color-aurora-start)' }} />
                                    <span style={{ fontSize: 15, fontWeight: 700 }}>Monthly Document Quota</span>
                                </div>
                                {statusBadge(subscription.subscriptionStatus)}
                            </div>

                            <div className="flex items-baseline gap-2 mb-2">
                                <span style={{ fontSize: 32, fontWeight: 800, color: 'var(--color-text-primary)' }}>
                                    {subscription.documentsUsedThisPeriod}
                                </span>
                                <span style={{ fontSize: 16, color: 'var(--color-text-secondary)' }}>
                                    / {subscription.monthlyDocumentLimit} documents
                                </span>
                            </div>

                            {/* Progress bar */}
                            <div className="w-full h-3 rounded-full bg-black/30 overflow-hidden mb-3 border border-white/5">
                                <div
                                    className="h-full rounded-full transition-all duration-500"
                                    style={{
                                        width: `${docPercent}%`,
                                        background: docPercent >= 90
                                            ? 'var(--color-danger)'
                                            : docPercent >= 70
                                                ? 'var(--color-warning)'
                                                : 'linear-gradient(90deg, var(--color-aurora-start), var(--color-aurora-end))'
                                    }}
                                />
                            </div>

                            <div className="flex items-center justify-between text-xs" style={{ color: 'var(--color-text-secondary)' }}>
                                <span>{docPercent}% of monthly quota consumed</span>
                                <span>
                                    Cycle ends: {subscription.billingPeriodEnd ? new Date(subscription.billingPeriodEnd).toLocaleDateString() : '30 days'}
                                </span>
                            </div>
                        </div>

                        {/* Daily LLM Budget Card */}
                        <div
                            className="card-neo p-6 relative overflow-hidden"
                            style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}
                        >
                            <div className="flex items-center justify-between mb-4">
                                <div className="flex items-center gap-2">
                                    <Zap size={18} style={{ color: 'var(--color-aurora-end)' }} />
                                    <span style={{ fontSize: 15, fontWeight: 700 }}>Daily AI Operations (LLM)</span>
                                </div>
                                <span className="badge badge-info" style={{ fontSize: 11 }}>
                                    Auto-resets 00:00 UTC
                                </span>
                            </div>

                            <div className="flex items-baseline gap-2 mb-2">
                                <span style={{ fontSize: 32, fontWeight: 800, color: 'var(--color-text-primary)' }}>
                                    {subscription.dailyLlmUsage}
                                </span>
                                <span style={{ fontSize: 16, color: 'var(--color-text-secondary)' }}>
                                    / {subscription.dailyLlmBudget} calls today
                                </span>
                            </div>

                            {/* Progress bar */}
                            <div className="w-full h-3 rounded-full bg-black/30 overflow-hidden mb-3 border border-white/5">
                                <div
                                    className="h-full rounded-full transition-all duration-500"
                                    style={{
                                        width: `${llmPercent}%`,
                                        background: llmPercent >= 90
                                            ? 'var(--color-danger)'
                                            : llmPercent >= 70
                                                ? 'var(--color-warning)'
                                                : 'linear-gradient(90deg, var(--color-info), var(--color-accent))'
                                    }}
                                />
                            </div>

                            <div className="flex items-center justify-between text-xs" style={{ color: 'var(--color-text-secondary)' }}>
                                <span>{llmPercent}% of daily allowance used</span>
                                <span>Denial-of-wallet protection enabled</span>
                            </div>
                        </div>
                    </div>
                )}

                {/* Stripe Test Identifiers Strip */}
                {subscription && (
                    <div
                        className="p-3.5 rounded-xl mb-10 flex flex-wrap items-center justify-between gap-3 text-xs"
                        style={{
                            background: 'var(--color-surface-raised)',
                            border: '1px solid var(--color-border)'
                        }}
                    >
                        <div className="flex items-center gap-2 text-white/70">
                            <ShieldCheck size={14} style={{ color: 'var(--color-aurora-end)' }} />
                            <span>Simulated Stripe Credentials:</span>
                        </div>
                        <div className="flex items-center gap-6">
                            <div className="flex items-center gap-1.5">
                                <span style={{ color: 'var(--color-text-disabled)' }}>Customer:</span>
                                <code style={{ color: 'var(--color-text-primary)', fontFamily: 'var(--font-mono)' }}>
                                    {subscription.stripeCustomerId || 'cus_sim_unassigned'}
                                </code>
                                <button
                                    onClick={() => copyToClipboard(subscription.stripeCustomerId, 'cust')}
                                    className="hover:text-white transition-colors"
                                    title="Copy Customer ID"
                                >
                                    {copiedId === 'cust' ? <Check size={12} className="text-emerald-400" /> : <Copy size={12} />}
                                </button>
                            </div>
                            <div className="flex items-center gap-1.5">
                                <span style={{ color: 'var(--color-text-disabled)' }}>Subscription:</span>
                                <code style={{ color: 'var(--color-text-primary)', fontFamily: 'var(--font-mono)' }}>
                                    {subscription.stripeSubscriptionId || 'sub_sim_unassigned'}
                                </code>
                                <button
                                    onClick={() => copyToClipboard(subscription.stripeSubscriptionId, 'sub')}
                                    className="hover:text-white transition-colors"
                                    title="Copy Subscription ID"
                                >
                                    {copiedId === 'sub' ? <Check size={12} className="text-emerald-400" /> : <Copy size={12} />}
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* ── Subscription Tiers Section ── */}
                <div className="mb-12">
                    <h2 style={{ fontSize: 19, fontWeight: 700, marginBottom: 6 }}>Available Subscription Tiers</h2>
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: 13.5, marginBottom: 20 }}>
                        Select a plan to simulate instant Stripe checkout and tier upgrade in test mode.
                    </p>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        {/* Free Tier */}
                        <div
                            className="card-neo p-6 flex flex-col justify-between"
                            style={{
                                background: 'var(--color-surface)',
                                borderColor: subscription?.planTier === 'FREE' ? 'var(--color-border)' : 'var(--color-border)'
                            }}
                        >
                            <div>
                                <div className="flex items-center justify-between mb-3">
                                    <h3 style={{ fontSize: 18, fontWeight: 700 }}>Free Tier</h3>
                                    {subscription?.planTier === 'FREE' && (
                                        <span className="badge badge-info" style={{ fontSize: 10 }}>CURRENT</span>
                                    )}
                                </div>
                                <div className="flex items-baseline gap-1 mb-4">
                                    <span style={{ fontSize: 28, fontWeight: 800 }}>$0</span>
                                    <span style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>/ month</span>
                                </div>
                                <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginBottom: 20 }}>
                                    Ideal for evaluation and light testing of document parsing.
                                </p>
                                <ul className="space-y-2.5 text-xs mb-6" style={{ color: 'var(--color-text-secondary)' }}>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-emerald-400" />
                                        10 documents / month
                                    </li>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-emerald-400" />
                                        50 AI operations / day
                                    </li>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-emerald-400" />
                                        1 standard template per type
                                    </li>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-emerald-400" />
                                        CSV document export
                                    </li>
                                </ul>
                            </div>

                            <button
                                onClick={() => handleUpgrade('FREE')}
                                disabled={actionLoading || subscription?.planTier === 'FREE'}
                                className="btn-secondary w-full justify-center"
                                style={{ fontSize: 13 }}
                            >
                                {subscription?.planTier === 'FREE' ? 'Current Plan' : 'Downgrade to Free'}
                            </button>
                        </div>

                        {/* Pro Tier (Popular) */}
                        <div
                            className="card-neo p-6 flex flex-col justify-between relative"
                            style={{
                                background: 'linear-gradient(180deg, rgba(124, 92, 252, 0.12), rgba(34, 211, 238, 0.05))',
                                borderColor: 'rgba(124, 92, 252, 0.4)',
                                boxShadow: '0 8px 32px rgba(124, 92, 252, 0.15)'
                            }}
                        >
                            <div className="absolute -top-3 right-6">
                                <span
                                    className="px-3 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wider text-white shadow-sm"
                                    style={{
                                        background: 'linear-gradient(135deg, var(--color-aurora-start), var(--color-aurora-end))'
                                    }}
                                >
                                    RECOMMENDED
                                </span>
                            </div>

                            <div>
                                <div className="flex items-center justify-between mb-3">
                                    <h3 style={{ fontSize: 18, fontWeight: 700, color: 'var(--color-text-primary)' }}>
                                        Professional
                                    </h3>
                                    {subscription?.planTier === 'PRO' && (
                                        <span className="badge badge-accent" style={{ fontSize: 10 }}>CURRENT</span>
                                    )}
                                </div>
                                <div className="flex items-baseline gap-1 mb-4">
                                    <span style={{ fontSize: 28, fontWeight: 800 }}>$49</span>
                                    <span style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>/ month</span>
                                </div>
                                <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginBottom: 20 }}>
                                    For growing teams needing cross-doc intelligence and high throughput.
                                </p>
                                <ul className="space-y-2.5 text-xs mb-6" style={{ color: 'var(--color-text-secondary)' }}>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-cyan-400" />
                                        <strong className="text-white">100 documents / month</strong>
                                    </li>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-cyan-400" />
                                        250 AI operations / day
                                    </li>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-cyan-400" />
                                        Multi-document anomaly detection & vendor trends
                                    </li>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-cyan-400" />
                                        Unlimited template variations
                                    </li>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-cyan-400" />
                                        Full JSON & CSV intelligence export
                                    </li>
                                </ul>
                            </div>

                            <button
                                onClick={() => handleUpgrade('PRO')}
                                disabled={actionLoading || subscription?.planTier === 'PRO'}
                                className="btn-primary w-full justify-center"
                                style={{ fontSize: 13 }}
                            >
                                {subscription?.planTier === 'PRO' ? 'Current Plan' : 'Upgrade to Pro ($49/mo)'}
                            </button>
                        </div>

                        {/* Enterprise Tier */}
                        <div
                            className="card-neo p-6 flex flex-col justify-between"
                            style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}
                        >
                            <div>
                                <div className="flex items-center justify-between mb-3">
                                    <h3 style={{ fontSize: 18, fontWeight: 700 }}>Enterprise</h3>
                                    {subscription?.planTier === 'ENTERPRISE' && (
                                        <span className="badge badge-accent" style={{ fontSize: 10 }}>CURRENT</span>
                                    )}
                                </div>
                                <div className="flex items-baseline gap-1 mb-4">
                                    <span style={{ fontSize: 28, fontWeight: 800 }}>$199</span>
                                    <span style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>/ month</span>
                                </div>
                                <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginBottom: 20 }}>
                                    Enterprise scale with dedicated queue workers and 24/7 SLA.
                                </p>
                                <ul className="space-y-2.5 text-xs mb-6" style={{ color: 'var(--color-text-secondary)' }}>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-emerald-400" />
                                        <strong className="text-white">1,000 documents / month</strong>
                                    </li>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-emerald-400" />
                                        500 AI operations / day
                                    </li>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-emerald-400" />
                                        Dedicated RabbitMQ background queue priority
                                    </li>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-emerald-400" />
                                        Full KYC, Resume, Invoice, and Contract parsing
                                    </li>
                                    <li className="flex items-center gap-2">
                                        <CheckCircle2 size={13} className="text-emerald-400" />
                                        Audit logs & dedicated support SLA
                                    </li>
                                </ul>
                            </div>

                            <button
                                onClick={() => handleUpgrade('ENTERPRISE')}
                                disabled={actionLoading || subscription?.planTier === 'ENTERPRISE'}
                                className="btn-secondary w-full justify-center"
                                style={{ fontSize: 13 }}
                            >
                                {subscription?.planTier === 'ENTERPRISE' ? 'Current Plan' : 'Upgrade to Enterprise ($199/mo)'}
                            </button>
                        </div>
                    </div>
                </div>

                {/* ── Interactive Stripe Webhook Simulator ── */}
                <div
                    className="card-neo p-6 mb-12"
                    style={{
                        background: 'var(--color-surface)',
                        borderColor: 'rgba(34, 211, 238, 0.25)'
                    }}
                >
                    <div className="flex items-center gap-2 mb-2">
                        <Zap size={18} style={{ color: 'var(--color-aurora-end)' }} />
                        <h2 style={{ fontSize: 18, fontWeight: 700 }}>Interactive Stripe Webhook Simulator</h2>
                    </div>
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: 13, marginBottom: 20 }}>
                        Simulate asynchronous Stripe webhook notifications in real time to verify subscription state transitions, payment failure alerts, and quota handling.
                    </p>

                    <form onSubmit={handleSimulateWebhook} className="flex flex-wrap items-end gap-4 mb-4">
                        <div className="flex-1 min-w-[240px]">
                            <label className="block text-xs font-semibold mb-1.5" style={{ color: 'var(--color-text-secondary)' }}>
                                Webhook Event Type
                            </label>
                            <select
                                value={webhookEvent}
                                onChange={(e) => setWebhookEvent(e.target.value)}
                                className="w-full px-3 py-2 rounded-lg text-sm bg-black/40 border border-white/10 text-white focus:outline-none focus:border-cyan-400"
                            >
                                <option value="invoice.payment_succeeded">invoice.payment_succeeded (Renews Active Period)</option>
                                <option value="invoice.payment_failed">invoice.payment_failed (Sets Status to PAST_DUE)</option>
                                <option value="customer.subscription.updated">customer.subscription.updated (Modifies Tier)</option>
                                <option value="customer.subscription.deleted">customer.subscription.deleted (Cancels & Downgrades)</option>
                            </select>
                        </div>

                        {webhookEvent === 'customer.subscription.updated' && (
                            <div className="w-[180px]">
                                <label className="block text-xs font-semibold mb-1.5" style={{ color: 'var(--color-text-secondary)' }}>
                                    Target Tier
                                </label>
                                <select
                                    value={webhookTier}
                                    onChange={(e) => setWebhookTier(e.target.value)}
                                    className="w-full px-3 py-2 rounded-lg text-sm bg-black/40 border border-white/10 text-white focus:outline-none focus:border-cyan-400"
                                >
                                    <option value="FREE">Free Tier</option>
                                    <option value="PRO">Professional</option>
                                    <option value="ENTERPRISE">Enterprise</option>
                                </select>
                            </div>
                        )}

                        <button
                            type="submit"
                            disabled={actionLoading}
                            className="btn-primary"
                            style={{
                                padding: '8px 20px',
                                fontSize: 13,
                                gap: 6,
                                background: 'linear-gradient(135deg, var(--color-aurora-start), var(--color-aurora-end))'
                            }}
                        >
                            <Zap size={14} className={actionLoading ? 'animate-spin' : ''} />
                            Dispatch Webhook
                        </button>
                    </form>

                    {webhookResult && (
                        <div
                            className="p-3.5 rounded-lg text-xs flex items-start gap-2.5 font-mono"
                            style={{
                                background: webhookResult.success ? 'rgba(52, 211, 153, 0.1)' : 'rgba(246, 90, 90, 0.1)',
                                border: `1px solid ${webhookResult.success ? 'rgba(52, 211, 153, 0.3)' : 'rgba(246, 90, 90, 0.3)'}`,
                                color: webhookResult.success ? '#34D399' : 'var(--color-danger)'
                            }}
                        >
                            {webhookResult.success ? <CheckCircle2 size={14} className="shrink-0 mt-0.5" /> : <AlertTriangle size={14} className="shrink-0 mt-0.5" />}
                            <div>
                                <span className="text-white/60">[{webhookResult.timestamp}]</span> {webhookResult.message}
                            </div>
                        </div>
                    )}
                </div>

                {/* ── Simulated Invoice & Receipt History ── */}
                <div>
                    <div className="flex items-center gap-2 mb-4">
                        <Receipt size={18} style={{ color: 'var(--color-aurora-start)' }} />
                        <h2 style={{ fontSize: 18, fontWeight: 700 }}>Simulated Billing History & Receipts</h2>
                    </div>

                    {invoices.length === 0 ? (
                        <div
                            className="card-neo p-8 text-center rounded-xl"
                            style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}
                        >
                            <Receipt size={32} className="mx-auto mb-2 text-white/20" />
                            <p style={{ color: 'var(--color-text-secondary)', fontSize: 13.5 }}>
                                No invoices generated yet. Upgrade to a paid plan or trigger a test webhook to generate simulated receipts.
                            </p>
                        </div>
                    ) : (
                        <div
                            className="card-neo rounded-xl overflow-hidden"
                            style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}
                        >
                            <table className="w-full text-left border-collapse text-xs">
                                <thead>
                                    <tr
                                        className="border-b"
                                        style={{
                                            background: 'var(--color-surface-raised)',
                                            borderColor: 'var(--color-border)',
                                            color: 'var(--color-text-secondary)'
                                        }}
                                    >
                                        <th className="py-3 px-4 font-semibold">Invoice #</th>
                                        <th className="py-3 px-4 font-semibold">Date</th>
                                        <th className="py-3 px-4 font-semibold">Description</th>
                                        <th className="py-3 px-4 font-semibold">Amount</th>
                                        <th className="py-3 px-4 font-semibold">Status</th>
                                        <th className="py-3 px-4 font-semibold text-right">Receipt</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-white/5">
                                    {invoices.map((inv) => (
                                        <tr key={inv.id} className="hover:bg-white/[0.02] transition-colors">
                                            <td className="py-3.5 px-4 font-mono font-medium text-white">
                                                {inv.invoiceNumber}
                                            </td>
                                            <td className="py-3.5 px-4" style={{ color: 'var(--color-text-secondary)' }}>
                                                {inv.createdAt ? new Date(inv.createdAt).toLocaleDateString() : '—'}
                                            </td>
                                            <td className="py-3.5 px-4" style={{ color: 'var(--color-text-secondary)' }}>
                                                {inv.description || 'Docket Subscription'}
                                            </td>
                                            <td className="py-3.5 px-4 font-semibold text-white">
                                                ${(inv.amountCents / 100).toFixed(2)} {inv.currency}
                                            </td>
                                            <td className="py-3.5 px-4">
                                                {inv.status === 'PAID' ? (
                                                    <span className="badge badge-accent" style={{ fontSize: 10 }}>PAID</span>
                                                ) : inv.status === 'FAILED' ? (
                                                    <span className="badge badge-danger" style={{ fontSize: 10 }}>FAILED</span>
                                                ) : (
                                                    <span className="badge badge-warning" style={{ fontSize: 10 }}>{inv.status}</span>
                                                )}
                                            </td>
                                            <td className="py-3.5 px-4 text-right">
                                                <button
                                                    onClick={() => showToast(`Receipt download simulation: ${inv.invoiceNumber} ($${(inv.amountCents / 100).toFixed(2)})`)}
                                                    className="btn-ghost hover:text-cyan-300"
                                                    style={{ padding: '3px 8px', fontSize: 11, gap: 4 }}
                                                >
                                                    <Download size={12} />
                                                    PDF
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}
