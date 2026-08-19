import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import { apiFetch, setToken } from '../lib/api';
import AmbientAurora from '../components/ui/AmbientAurora';

/**
 * Login page — email + password form with Aurora Obsidian visual styling.
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
        <div className="min-h-screen relative flex items-center justify-center px-4 overflow-hidden"
             style={{ background: 'var(--color-bg)' }}>
            {/* Ambient Aurora Light Canvas */}
            <AmbientAurora opacity={0.55} />

            <motion.div
                initial={{ opacity: 0, y: 14 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.35, ease: 'easeOut' }}
                className="card relative w-full max-w-md p-8 z-10"
                style={{
                    background: 'rgba(27, 24, 48, 0.82)',
                    boxShadow: '0 12px 40px rgba(0, 0, 0, 0.45), 0 0 60px rgba(124, 92, 252, 0.12)',
                }}
            >
                {/* Logo / Brand */}
                <div className="text-center mb-8">
                    <h1 className="text-3xl font-bold mb-2 tracking-tight">
                        <span className="text-aurora">Docket</span>
                    </h1>
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: '14px' }}>
                        Document intelligence & automated anomaly detection
                    </p>
                </div>

                {error && <div className="alert-error mb-5">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="mb-4">
                        <label htmlFor="login-email" className="label">Email</label>
                        <input
                            id="login-email"
                            type="email"
                            className="input"
                            placeholder="you@company.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            autoComplete="email"
                        />
                    </div>

                    <div className="mb-6">
                        <label htmlFor="login-password" className="label">Password</label>
                        <input
                            id="login-password"
                            type="password"
                            className="input"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            autoComplete="current-password"
                        />
                    </div>

                    <button
                        type="submit"
                        className="btn-primary w-full"
                        disabled={loading}
                    >
                        {loading ? 'Signing in…' : 'Sign In'}
                    </button>
                </form>

                <p className="text-center mt-6" style={{ color: 'var(--color-text-secondary)', fontSize: '14px' }}>
                    Don't have an account?{' '}
                    <Link to="/signup" style={{ fontWeight: 600, color: 'var(--color-aurora-end)' }}>Create one</Link>
                </p>
            </motion.div>
        </div>
    );
}
