import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiFetch, setToken } from '../lib/api';

/**
 * Signup page — email + password + workspace name form.
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
                        Create your workspace
                    </p>
                </div>

                {error && <div className="alert-error mb-4">{error}</div>}

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
                        {loading ? 'Creating account…' : 'Create account'}
                    </button>
                </form>

                <p className="text-center mt-6" style={{ color: 'var(--color-text-secondary)', fontSize: '14px' }}>
                    Already have an account?{' '}
                    <Link to="/login" style={{ fontWeight: 500 }}>Sign in</Link>
                </p>
            </div>
        </div>
    );
}
