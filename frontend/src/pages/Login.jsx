import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiFetch, setToken } from '../lib/api';

/**
 * Login page — email + password form.
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
        <div className="min-h-screen flex items-center justify-center px-4"
             style={{ background: 'var(--color-bg)' }}>
            <div className="card w-full max-w-md p-8">
                {/* Logo / Brand */}
                <div className="text-center mb-8">
                    <h1 className="text-3xl font-bold mb-2"
                        style={{ fontFamily: 'var(--font-heading)', color: 'var(--color-primary)' }}>
                        Docket
                    </h1>
                    <p style={{ color: 'var(--color-text-secondary)', fontSize: '15px' }}>
                        Sign in to your workspace
                    </p>
                </div>

                {error && <div className="alert-error mb-4">{error}</div>}

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
                        {loading ? 'Signing in…' : 'Sign in'}
                    </button>
                </form>

                <p className="text-center mt-6" style={{ color: 'var(--color-text-secondary)', fontSize: '14px' }}>
                    Don't have an account?{' '}
                    <Link to="/signup" style={{ fontWeight: 500 }}>Create one</Link>
                </p>
            </div>
        </div>
    );
}
