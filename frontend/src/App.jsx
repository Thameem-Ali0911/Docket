import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Dashboard from './pages/Dashboard';
import UploadDocument from './pages/UploadDocument';
import DocumentDetail from './pages/DocumentDetail';
import TemplateManager from './pages/TemplateManager';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                <Route path="/login" element={<Login />} />
                <Route path="/signup" element={<Signup />} />
                <Route
                    path="/dashboard"
                    element={
                        <ProtectedRoute>
                            <Dashboard />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/upload"
                    element={
                        <ProtectedRoute>
                            <UploadDocument />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/documents/:id"
                    element={
                        <ProtectedRoute>
                            <DocumentDetail />
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
