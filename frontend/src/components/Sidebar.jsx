import { NavLink, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useAuth } from '../context/AuthContext';
import BrandLogo from './BrandLogo';

const links = [
  { to: '/', label: 'Dashboard', icon: 'Grid' },
  { to: '/analytics', label: 'Analytics', icon: 'Trend' },
  { to: '/practice', label: 'Practice Lab', icon: 'Queue' },
  { to: '/ai', label: 'AI Coach', icon: 'Coach' },
  { to: '/history', label: 'History', icon: 'Clock' },
  { to: '/profile', label: 'Profile', icon: 'User' },
];

export default function Sidebar() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <aside className="fixed left-0 top-0 h-full w-64 bg-primary text-white flex flex-col z-50">
      <div className="p-6 border-b border-white/10">
        <BrandLogo />
      </div>

      <nav className="flex-1 p-4 space-y-1">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            end={link.to === '/'}
            className={({ isActive }) =>
              `group relative flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-medium transition-all duration-300 overflow-hidden ${
                isActive
                  ? 'bg-accent/15 text-accent border border-accent/30 shadow-lg shadow-accent/10'
                  : 'text-slate-300 hover:bg-white/7 hover:text-white hover:translate-x-1'
              }`
            }
          >
            {({ isActive }) => (
              <>
                {isActive && (
                  <motion.div
                    layoutId="activeSidebarPill"
                    className="absolute inset-0 bg-gradient-to-r from-accent/15 to-transparent"
                    transition={{ type: 'spring', stiffness: 380, damping: 34 }}
                  />
                )}
                <span className="relative w-9 text-[11px] font-bold uppercase tracking-wider text-current/80">
                  {link.icon}
                </span>
                <span className="relative">{link.label}</span>
                {isActive && (
                  <motion.div
                    layoutId="activeIndicator"
                    className="relative ml-auto w-1.5 h-1.5 rounded-full bg-accent"
                  />
                )}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="p-4 border-t border-white/10">
        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-sm font-medium text-slate-300 hover:bg-red-500/10 hover:text-red-400 hover:translate-x-1 transition-all duration-300"
        >
          <span className="w-9 text-[11px] font-bold uppercase tracking-wider">Exit</span>
          Logout
        </button>
      </div>
    </aside>
  );
}
