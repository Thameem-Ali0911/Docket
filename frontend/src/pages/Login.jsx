import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import { Mail, Lock, ArrowRight, Loader2 } from 'lucide-react';
import { apiFetch, setToken } from '../lib/api';
import AmbientAurora from '../components/ui/AmbientAurora';

/**
 * Login page — neomorphic auth card with Apple-style layout.
 * On success: stores JWT, redirects to /dashboard.
 */
export default function Login() {
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            const data = await apiFetch('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ email, password }),
            });
            setToken(data.token);
            navigate('/dashboard', { replace: true });
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="min-h-screen relative flex items-center justify-center px-4 overflow-hidden bg-grid"
             style={{ background: 'var(--color-bg)' }}>
            <AmbientAurora opacity={0.50} />

            {/* Centered auth card */}
            <motion.div
                initial={{ opacity: 0, y: 20, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
                className="card-lg relative w-full max-w-md z-10"
                style={{ padding: '44px 40px' }}
            >
                {/* Aurora top edge accent line */}
                <div style={{
                    position: 'absolute', top: 0, left: '15%', right: '15%', height: '1px',
                    background: 'linear-gradient(90deg, transparent, rgba(124,92,252,0.6), rgba(34,211,238,0.5), transparent)',
                    borderRadius: '9999px',
                }} />

                {/* Brand */}
                <div className="text-center mb-9">
                    <div style={{
                        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                        width: 52, height: 52, borderRadius: 14, marginBottom: 16,
                        background: 'linear-gradient(135deg, rgba(124,92,252,0.2), rgba(34,211,238,0.1))',
                        border: '1px solid rgba(124,92,252,0.3)',
                        boxShadow: 'var(--neo-shadow-sm)',
                    }}>
                        <span style={{
                            fontSize: 22, fontWeight: 800, fontFamily: 'var(--font-heading)',
                            background: 'linear-gradient(135deg, #7C5CFC, #22D3EE)',
                            WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
                        }}>D</span>
                    </div>
                    <h1 style={{ fontSize: 26, fontWeight: 800, letterSpacing: '-0.025em', marginBottom: 6 }}>
                        Welcome back
                    </h1>
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: 14 }}>
                        Sign in to your Docket workspace
                    </p>
                </div>

                {error && <div className="alert-error mb-5">{error}</div>}

                <form onSubmit={handleSubmit}>
                    {/* Email field */}
                    <div style={{ marginBottom: 16 }}>
                        <label htmlFor="login-email" className="label">Email address</label>
                        <div style={{ position: 'relative' }}>
                            <input
                                id="login-email"
                                type="email"
                                className="input"
                                placeholder="you@company.com"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                                autoComplete="email"
                                style={{ paddingLeft: 40 }}
                            />
                            <Mail size={15} style={{
                                position: 'absolute', left: 13, top: '50%', transform: 'translateY(-50%)',
                                color: 'var(--color-text-disabled)',
                                pointerEvents: 'none',
                            }} />
                        </div>
                    </div>

                    {/* Password field */}
                    <div style={{ marginBottom: 28 }}>
                        <label htmlFor="login-password" className="label">Password</label>
                        <div style={{ position: 'relative' }}>
                            <input
                                id="login-password"
                                type="password"
                                className="input"
                                placeholder="••••••••"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                autoComplete="current-password"
                                style={{ paddingLeft: 40 }}
                            />
                            <Lock size={15} style={{
                                position: 'absolute', left: 13, top: '50%', transform: 'translateY(-50%)',
                                color: 'var(--color-text-disabled)',
                                pointerEvents: 'none',
                            }} />
                        </div>
                    </div>

                    <button
                        type="submit"
                        className="btn-primary w-full"
                        disabled={loading}
                        style={{ padding: '12px 22px', fontSize: 15, justifyContent: 'center' }}
                    >
                        {loading ? (
                            <><Loader2 size={16} className="animate-spin" /> Signing in…</>
                        ) : (
                            <>Sign In <ArrowRight size={15} /></>
                        )}
                    </button>
                </form>

                <div className="divider" style={{ margin: '24px 0' }} />

                <p className="text-center" style={{ color: 'var(--color-text-secondary)', fontSize: 14 }}>
                    Don't have an account?{' '}
                    <Link to="/signup" style={{ fontWeight: 700, color: 'var(--color-aurora-end)' }}>
                        Create one
                    </Link>
                </p>
            </motion.div>
        </div>
    );
}
