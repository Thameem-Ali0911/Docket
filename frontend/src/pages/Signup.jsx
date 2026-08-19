import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import { apiFetch, setToken } from '../lib/api';
import AmbientAurora from '../components/ui/AmbientAurora';

/**
 * Signup page — email + password + workspace name form with Aurora Obsidian visual styling.
 * On success: stores JWT, redirects to /dashboard.
 */
export default function Signup() {
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [workspaceName, setWorkspaceName] = useState('');
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            const data = await apiFetch('/api/auth/signup', {
                method: 'POST',
                body: JSON.stringify({ email, password, workspaceName }),
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
                        Create your workspace to start analyzing documents
                    </p>
                </div>

                {error && <div className="alert-error mb-5">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="mb-4">
                        <label htmlFor="signup-workspace" className="label">Workspace Name</label>
                        <input
                            id="signup-workspace"
                            type="text"
                            className="input"
                            placeholder="Acme Corp"
                            value={workspaceName}
                            onChange={(e) => setWorkspaceName(e.target.value)}
                            required
                        />
                    </div>

                    <div className="mb-4">
                        <label htmlFor="signup-email" className="label">Email</label>
                        <input
                            id="signup-email"
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
                        <label htmlFor="signup-password" className="label">Password</label>
                        <input
                            id="signup-password"
                            type="password"
                            className="input"
                            placeholder="Min. 8 characters"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            minLength={8}
                            autoComplete="new-password"
                        />
                    </div>

                    <button
                        type="submit"
                        className="btn-primary w-full"
                        disabled={loading}
                    >
                        {loading ? 'Creating workspace…' : 'Create Workspace'}
                    </button>
                </form>

                <p className="text-center mt-6" style={{ color: 'var(--color-text-secondary)', fontSize: '14px' }}>
                    Already have an account?{' '}
                    <Link to="/login" style={{ fontWeight: 600, color: 'var(--color-aurora-end)' }}>Sign in</Link>
                </p>
            </motion.div>
        </div>
    );
}
