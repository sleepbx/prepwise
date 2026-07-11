import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Analytics from './pages/Analytics';
import Practice from './pages/Practice';
import History from './pages/History';
import Profile from './pages/Profile';
import AI from './pages/AI';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/" element={
            <ProtectedRoute>
              <Layout><Dashboard /></Layout>
            </ProtectedRoute>
          } />
          <Route path="/analytics" element={
            <ProtectedRoute>
              <Layout><Analytics /></Layout>
            </ProtectedRoute>
          } />
          <Route path="/practice" element={
            <ProtectedRoute>
              <Layout><Practice /></Layout>
            </ProtectedRoute>
          } />
          <Route path="/ai" element={
            <ProtectedRoute>
              <Layout><AI /></Layout>
            </ProtectedRoute>
          } />
          <Route path="/history" element={
            <ProtectedRoute>
              <Layout><History /></Layout>
            </ProtectedRoute>
          } />
          <Route path="/profile" element={
            <ProtectedRoute>
              <Layout><Profile /></Layout>
            </ProtectedRoute>
          } />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
