import { Navigate } from 'react-router-dom';
import { isAuthenticated } from '../lib/api';

/**
 * Route wrapper that redirects to /login if no JWT token is present.
 * Used to protect authenticated pages (Dashboard, etc.).
 */
export default function ProtectedRoute({ children }) {
    if (!isAuthenticated()) {
        return <Navigate to="/login" replace />;
    }

    return children;
}
