import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import AuthLayout from '../components/AuthLayout';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const [email, setEmail] = useState('demo@prepwise.dev');
  const [password, setPassword] = useState('demo123');
  const [error, setError] = useState('');
  const [waking, setWaking] = useState(false);
  const { login, loading } = useAuth();
  const navigate = useNavigate();

  // The backend runs on a free tier that sleeps when idle; the first request
  // wakes it (~1 min). The axios layer retries automatically and emits this
  // event so we can reassure the user instead of showing a false error.
  useEffect(() => {
    const onWaking = (e) => setWaking(e.detail === true);
    window.addEventListener('prepwise:waking', onWaking);
    return () => window.removeEventListener('prepwise:waking', onWaking);
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid credentials');
    } finally {
      setWaking(false);
    }
  };

  return (
    <AuthLayout
      title="Track your preparation like a serious engineer."
      subtitle="Map sheet progress, pattern confidence, and daily consistency into one focused workspace."
      footerText="New to PrepWise?"
      footerLink="/register"
      footerLabel="Create account"
    >
      <div className="glass-card rounded-[20px] p-8 shadow-sm">
        <h2 className="text-2xl font-bold text-primary mb-1">Welcome back</h2>
        <p className="text-slate-500 mb-6">Sign in to your command center</p>

        {error && (
          <div className="mb-4 p-3 rounded-xl bg-red-50 text-red-600 text-sm">{error}</div>
        )}

        {waking && !error && (
          <div className="mb-4 p-3 rounded-xl bg-amber-50 text-amber-700 text-sm flex items-center gap-2">
            <span className="inline-block w-3.5 h-3.5 border-2 border-amber-400 border-t-transparent rounded-full animate-spin" />
            Waking up the server — this can take up to a minute on first load.
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-accent/40 focus:border-accent transition"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-accent/40 focus:border-accent transition"
              required
            />
          </div>
          <motion.button
            whileHover={{ scale: 1.01 }}
            whileTap={{ scale: 0.99 }}
            type="submit"
            disabled={loading}
            className="w-full py-3.5 rounded-xl bg-accent text-white font-semibold hover:bg-teal-600 transition disabled:opacity-60"
          >
            {loading ? (waking ? 'Waking server…' : 'Signing in...') : 'Sign In'}
          </motion.button>
        </form>

        <p className="mt-4 text-xs text-slate-400 text-center">
          Demo: demo@prepwise.dev / demo123
        </p>
      </div>
    </AuthLayout>
  );
}
