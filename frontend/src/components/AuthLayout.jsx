import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import BrandLogo from './BrandLogo';

function AuthLayout({ title, subtitle, children, footerText, footerLink, footerLabel }) {
  return (
    <div className="min-h-screen flex">
      <div className="hidden lg:flex lg:w-1/2 gradient-hero items-center justify-center p-12 relative overflow-hidden">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="relative z-10 max-w-md"
        >
          <div className="mb-8">
            <BrandLogo />
          </div>
          <h1 className="text-4xl font-bold text-white leading-tight mb-4">
            {title}
          </h1>
          <p className="text-lg text-slate-300 leading-relaxed">
            {subtitle}
          </p>
        </motion.div>
      </div>

      <div className="flex-1 flex items-center justify-center p-8 bg-surface">
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5 }}
          className="w-full max-w-md"
        >
          <div className="lg:hidden mb-8">
            <div className="mb-4">
              <BrandLogo />
            </div>
          </div>
          {children}
          <p className="mt-6 text-center text-sm text-slate-500">
            {footerText}{' '}
            <Link to={footerLink} className="text-accent font-medium hover:underline">
              {footerLabel}
            </Link>
          </p>
        </motion.div>
      </div>
    </div>
  );
}

export default AuthLayout;
