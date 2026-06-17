import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { RadialBarChart, RadialBar, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, Cell } from 'recharts';
import api from '../api/axios';
import CountUp from '../components/CountUp';

const COMPANIES = ['Google', 'Amazon', 'Microsoft', 'Adobe'];
const READINESS_COLORS = { EARLY_STAGE: '#ef4444', ON_TRACK: '#f59e0b', INTERVIEW_READY: '#14b8a6', STRONG_CANDIDATE: '#3b82f6' };

export default function AI() {
  const [readiness, setReadiness] = useState(null);
  const [coach, setCoach] = useState(null);
  const [weaknesses, setWeaknesses] = useState([]);
  const [revisions, setRevisions] = useState([]);
  const [insight, setInsight] = useState(null);
  const [companyAdvice, setCompanyAdvice] = useState(null);
  const [company, setCompany] = useState('Google');
  const [coachCompany, setCoachCompany] = useState('Google');
  const [coachPrompt, setCoachPrompt] = useState('What should I improve today and why?');
  const [coachLoading, setCoachLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    const load = async () => {
      const results = await Promise.allSettled([
        api.get('/ai/readiness'),
        api.get('/ai/daily-coach'),
        api.get('/ai/weakness-analysis'),
        api.get('/ai/revision-priorities'),
        api.get('/ai/insights'),
      ]);

      const failed = results.filter((r) => r.status === 'rejected');
      if (failed.length === results.length) setLoadError('Could not reach AI services. Is the backend running on port 8080?');
      else if (failed.length > 0) setLoadError('Some insights could not load. Showing available data.');

      if (results[0].status === 'fulfilled') setReadiness(results[0].value.data);
      if (results[1].status === 'fulfilled') setCoach(results[1].value.data);
      if (results[2].status === 'fulfilled') setWeaknesses(results[2].value.data.slice(0, 6));
      if (results[3].status === 'fulfilled') setRevisions(results[3].value.data.slice(0, 5));
      if (results[4].status === 'fulfilled') setInsight(results[4].value.data);
      setLoading(false);
    };
    load();
  }, []);

  const getCompanyAdvice = async () => {
    const { data } = await api.post('/ai/company-advisor', { company });
    setCompanyAdvice(data);
  };

  const askCoach = async () => {
    setCoachLoading(true);
    try {
      const { data } = await api.post('/ai/daily-coach', {
        company: coachCompany,
        prompt: coachPrompt,
      });
      setCoach(data);
    } finally {
      setCoachLoading(false);
    }
  };

  if (loading) {
    return <div className="flex items-center justify-center h-screen text-slate-400">Loading intelligence layer...</div>;
  }

  const weakChart = weaknesses.filter((w) => w.weaknessIndex > 30).slice(0, 5).map((w) => ({
    name: w.topic.length > 12 ? `${w.topic.slice(0, 11)}...` : w.topic,
    index: w.weaknessIndex,
    fill: w.classification === 'WEAK' ? '#ef4444' : w.classification === 'MODERATE' ? '#f59e0b' : '#14b8a6',
  }));

  return (
    <div className="p-6 md:p-8 space-y-8 max-w-[1400px]">
      <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
        className="gradient-hero rounded-[20px] p-8 md:p-10 text-white relative overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_rgba(59,130,246,0.12),_transparent_55%)]" />
        <div className="relative z-10">
          <p className="text-accent text-sm font-semibold tracking-widest uppercase mb-2">AI Coach</p>
          <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight mb-2">What should I improve today and why?</h1>
          <p className="text-slate-300 max-w-2xl">An intelligent mentor layer for readiness, weak topics, revision priorities, and company-specific focus.</p>
        </div>
      </motion.section>

      {loadError && <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-sm">{loadError}</div>}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card title="Interview Readiness Score" badge={readiness?.level?.replace(/_/g, ' ')}>
          <div className="flex items-center gap-6">
            <div className="relative w-28 h-28">
              <ResponsiveContainer width="100%" height="100%">
                <RadialBarChart cx="50%" cy="50%" innerRadius="70%" outerRadius="100%" data={[{ value: readiness?.score || 0 }]} startAngle={90} endAngle={-270}>
                  <RadialBar dataKey="value" cornerRadius={8} fill={READINESS_COLORS[readiness?.level] || '#14b8a6'} background={{ fill: '#e2e8f0' }} />
                </RadialBarChart>
              </ResponsiveContainer>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-2xl font-extrabold text-primary"><CountUp end={readiness?.score || 0} /></span>
              </div>
            </div>
            <div className="flex-1 space-y-1 text-sm">
              {readiness?.breakdown && Object.entries(readiness.breakdown).map(([k, v]) => (
                <div key={k} className="flex justify-between">
                  <span className="text-slate-500 capitalize">{k}</span>
                  <span className="font-semibold text-primary">{v}</span>
                </div>
              ))}
            </div>
          </div>
          <p className="mt-4 text-sm text-slate-600 leading-relaxed border-t border-slate-100 pt-4">{readiness?.explanation}</p>
        </Card>

        <Card title="Today&apos;s Coach" className="lg:col-span-2">
          <div className="grid grid-cols-1 md:grid-cols-[160px_1fr_auto] gap-3 mb-4">
            <select
              value={coachCompany}
              onChange={(e) => setCoachCompany(e.target.value)}
              className="px-3 py-2 rounded-xl border border-slate-200 text-sm"
            >
              {COMPANIES.map((c) => <option key={c}>{c}</option>)}
            </select>
            <input
              value={coachPrompt}
              onChange={(e) => setCoachPrompt(e.target.value)}
              className="px-3 py-2 rounded-xl border border-slate-200 text-sm"
              placeholder="Ask what to improve for your target company"
            />
            <button
              onClick={askCoach}
              disabled={coachLoading || !coachPrompt.trim()}
              className="px-5 py-2 rounded-xl bg-primary text-white font-semibold text-sm hover:bg-slate-800 transition disabled:opacity-60"
            >
              {coachLoading ? 'Asking...' : 'Ask'}
            </button>
          </div>
          <p className="text-lg font-bold text-primary mb-3">{coach?.headline}</p>
          <ul className="space-y-2 mb-4">
            {coach?.tasks?.map((task, i) => (
              <li key={i} className="flex items-start gap-2 text-sm text-slate-600">
                <span className="text-accent mt-0.5">-</span>{task}
              </li>
            ))}
          </ul>
          <span className="text-xs px-3 py-1 rounded-full bg-teal-50 text-accent font-semibold">{coach?.estimatedImpact}</span>
          <p className="mt-4 text-sm text-slate-500 leading-relaxed border-t border-slate-100 pt-4">{coach?.message}</p>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Weak Topic Analysis">
          {weakChart.length > 0 ? (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={weakChart} layout="vertical" margin={{ left: 10 }}>
                <XAxis type="number" domain={[0, 100]} tick={{ fontSize: 10 }} />
                <YAxis type="category" dataKey="name" tick={{ fontSize: 10 }} width={80} />
                <Tooltip formatter={(v) => [`${v}`, 'Weakness Index']} />
                <Bar dataKey="index" radius={[0, 6, 6, 0]}>
                  {weakChart.map((entry, i) => <Cell key={i} fill={entry.fill} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          ) : <p className="text-slate-400 text-sm">Solve more problems to unlock weakness analysis.</p>}
          <div className="mt-4 space-y-2">
            {weaknesses.slice(0, 3).map((w) => (
              <div key={w.topic} className="flex items-center justify-between p-2 rounded-xl bg-slate-50 text-sm">
                <span className="font-medium text-primary">{w.topic}</span>
                <span className={`text-xs px-2 py-0.5 rounded-full font-semibold ${w.classification === 'WEAK' ? 'bg-red-50 text-red-600' : w.classification === 'MODERATE' ? 'bg-amber-50 text-amber-600' : 'bg-teal-50 text-accent'}`}>{w.classification}</span>
              </div>
            ))}
          </div>
        </Card>

        <Card title="Revision Priorities">
          <div className="space-y-3">
            {revisions.map((r, i) => (
              <motion.div key={r.problemId} initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.05 }}
                className="p-3 rounded-xl border border-slate-100 hover:border-accent/20 transition">
                <div className="flex justify-between items-start gap-2">
                  <p className="font-semibold text-primary text-sm">{r.title}</p>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-bold shrink-0 ${r.priority === 'HIGH' ? 'bg-red-50 text-red-600' : r.priority === 'MEDIUM' ? 'bg-amber-50 text-amber-600' : 'bg-slate-100 text-slate-500'}`}>{r.priority}</span>
                </div>
                <p className="text-xs text-slate-500 mt-1">{r.reason}</p>
              </motion.div>
            ))}
          </div>
        </Card>
      </div>

      <Card title="Week-over-Week Insight">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
          {insight?.changes && Object.entries(insight.changes).slice(0, 3).map(([k, v]) => (
            <div key={k} className="p-3 rounded-xl bg-slate-50 text-center">
              <p className="text-xs text-slate-500 capitalize">{k.replace(/([A-Z])/g, ' $1')}</p>
              <p className="text-lg font-bold text-primary mt-1">{typeof v === 'number' ? v.toFixed?.(1) ?? v : v}</p>
            </div>
          ))}
        </div>
        <p className="text-sm text-slate-600 leading-relaxed">{insight?.insight}</p>
      </Card>

      <Card title="Company Advisor">
        <div className="flex gap-3 mb-4">
          <select value={company} onChange={(e) => setCompany(e.target.value)}
            className="flex-1 px-3 py-2 rounded-xl border border-slate-200 text-sm">
            {COMPANIES.map((c) => <option key={c}>{c}</option>)}
          </select>
          <button onClick={getCompanyAdvice}
            className="px-5 py-2 rounded-xl bg-primary text-white font-semibold text-sm hover:bg-slate-800 transition">
            Analyze
          </button>
        </div>
        {companyAdvice && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
            <div className="flex flex-wrap gap-2 mb-3">
              {companyAdvice.focusAreas?.map((area) => (
                <span key={area} className="px-3 py-1 rounded-full bg-blue-50 text-secondary-blue text-sm font-semibold border border-blue-100">{area}</span>
              ))}
            </div>
            <div className="space-y-1 mb-4">
              {companyAdvice.weeklyAllocation && Object.entries(companyAdvice.weeklyAllocation).map(([k, v]) => (
                <div key={k} className="flex justify-between text-sm">
                  <span className="text-slate-600">{k}</span>
                  <span className="font-bold text-primary">{v}</span>
                </div>
              ))}
            </div>
            <p className="text-sm text-slate-600 leading-relaxed border-t border-slate-100 pt-3">{companyAdvice.explanation}</p>
          </motion.div>
        )}
      </Card>
    </div>
  );
}

function Card({ title, badge, children, className = '' }) {
  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
      className={`bg-card rounded-[20px] p-6 border border-slate-100 shadow-sm ${className}`}>
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold text-primary">{title}</h2>
        {badge && <span className="text-xs px-2.5 py-1 rounded-full bg-accent/10 text-accent font-semibold">{badge}</span>}
      </div>
      {children}
    </motion.div>
  );
}
