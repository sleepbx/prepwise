import { useEffect, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import api from '../api/axios';
import CountUp from '../components/CountUp';
import { formatPattern, formatEnum, formatDate, formatTime, difficultyClass } from '../utils/format';

const statCards = [
  { key: 'problemsSolved', label: 'Problems Solved', icon: '✦', gradient: 'from-teal-500/10 to-teal-500/5', accent: 'text-accent' },
  { key: 'timeInvestedMinutes', label: 'Time Invested', icon: '⏱', gradient: 'from-blue-500/10 to-blue-500/5', accent: 'text-secondary-blue', format: 'time' },
  { key: 'averageSolveTime', label: 'Avg Solve Time', icon: '⚡', gradient: 'from-amber-500/10 to-amber-500/5', accent: 'text-secondary-amber', suffix: 'm', decimals: 1 },
  { key: 'hintUsagePercent', label: 'Hint Usage', icon: '💡', gradient: 'from-pink-500/10 to-pink-500/5', accent: 'text-secondary-pink', suffix: '%', decimals: 1 },
];

const QUICK_LINKS = [
  { label: 'Two Sum', url: 'https://leetcode.com/problems/two-sum/' },
  { label: '3Sum', url: 'https://leetcode.com/problems/3sum/' },
  { label: 'Valid Parentheses', url: 'https://leetcode.com/problems/valid-parentheses/' },
  { label: 'Number of Islands', url: 'https://leetcode.com/problems/number-of-islands/' },
];

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [url, setUrl] = useState('');
  const [timeSpent, setTimeSpent] = useState(30);
  const [hintUsed, setHintUsed] = useState(false);
  const [saving, setSaving] = useState(false);
  const [savedMeta, setSavedMeta] = useState(null);
  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchDashboard = () => {
    api.get('/analytics/dashboard').then((res) => setData(res.data));
  };

  useEffect(() => { fetchDashboard(); }, []);

  useEffect(() => {
    if (!url.trim() || url.length < 12) {
      setPreview(null);
      return;
    }
    const timer = setTimeout(async () => {
      setPreviewLoading(true);
      try {
        const { data: resolved } = await api.get('/problems/resolve', { params: { url: url.trim() } });
        setPreview(resolved);
        setError('');
      } catch {
        setPreview(null);
      } finally {
        setPreviewLoading(false);
      }
    }, 400);
    return () => clearTimeout(timer);
  }, [url]);

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const { data: attempt } = await api.post('/problems', {
        problemUrl: url.trim(),
        timeSpentMinutes: Number(timeSpent),
        hintUsed,
      });
      setSavedMeta(attempt);
      setUrl('');
      setPreview(null);
      setTimeSpent(30);
      setHintUsed(false);
      fetchDashboard();
      setTimeout(() => setSavedMeta(null), 5000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save attempt');
    } finally {
      setSaving(false);
    }
  };

  if (!data) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-2 border-accent border-t-transparent rounded-full animate-spin" />
          <p className="text-slate-400 text-sm">Loading your command center...</p>
        </div>
      </div>
    );
  }

  const canSave = preview?.found && !saving;

  return (
    <div className="p-6 md:p-8 space-y-8 max-w-[1400px]">
      <motion.section
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="gradient-hero rounded-[20px] p-8 md:p-10 text-white relative overflow-hidden"
      >
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_rgba(20,184,166,0.15),_transparent_60%)]" />
        <div className="absolute -bottom-20 -right-20 w-80 h-80 bg-accent/10 rounded-full blur-3xl" />
        <div className="relative z-10">
          <p className="text-accent/90 text-sm font-semibold tracking-widest uppercase mb-3">Performance Desk</p>
          <h1 className="text-3xl md:text-[2.6rem] font-extrabold leading-tight mb-3 max-w-3xl tracking-tight">
            Your DSA prep, mapped like a performance desk.
          </h1>
          <p className="text-slate-300 text-lg max-w-2xl mb-10 leading-relaxed">
            Blend sheet progress, daily attempts, pattern confidence, and review discipline in one focused workspace.
          </p>
          <div className="flex flex-wrap gap-4">
            <HeroPill label="Current Streak" value={data.currentStreak} suffix=" days" icon="🔥" />
            <HeroPill label="Sprint Progress" value={data.sprintProgress} suffix=" this week" icon="📈" />
            <HeroPill label="Total Solved" value={data.totalSolved} icon="✓" />
          </div>
        </div>
      </motion.section>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {statCards.map((card, i) => (
          <motion.div
            key={card.key}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.07 }}
            className={`relative overflow-hidden bg-card rounded-[20px] p-6 border border-slate-100/80 hover:border-accent/20 hover:shadow-lg hover:shadow-accent/5 transition-all duration-300`}
          >
            <div className={`absolute inset-0 bg-gradient-to-br ${card.gradient} opacity-60`} />
            <div className="relative">
              <div className="flex items-center justify-between mb-4">
                <span className="text-sm text-slate-500 font-medium">{card.label}</span>
                <span className={`text-xl ${card.accent}`}>{card.icon}</span>
              </div>
              <p className="text-3xl font-extrabold text-primary tracking-tight">
                {card.format === 'time' ? (
                  formatTime(data[card.key])
                ) : (
                  <CountUp end={data[card.key]} suffix={card.suffix || ''} decimals={card.decimals || 0} />
                )}
              </p>
            </div>
          </motion.div>
        ))}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="xl:col-span-2 bg-card rounded-[20px] p-6 md:p-7 border border-slate-100 shadow-sm"
        >
          <div className="flex items-center gap-2 mb-1">
            <span className="w-2 h-2 rounded-full bg-accent animate-pulse" />
            <h2 className="text-lg font-bold text-primary">Log Today&apos;s Problem</h2>
          </div>
          <p className="text-sm text-slate-500 mb-5">Paste a URL — we auto-detect title, difficulty, topic & pattern</p>

          <form onSubmit={handleSave} className="space-y-4">
            <div>
              <label className="text-sm font-medium text-slate-700 mb-1.5 block">Problem URL</label>
              <input
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="https://leetcode.com/problems/two-sum/"
                className="w-full px-4 py-3.5 rounded-xl border border-slate-200 bg-slate-50/50 focus:outline-none focus:ring-2 focus:ring-accent/30 focus:border-accent focus:bg-white text-sm transition"
                required
              />
            </div>

            <div className="flex flex-wrap gap-2">
              {QUICK_LINKS.map((q) => (
                <button
                  key={q.url}
                  type="button"
                  onClick={() => setUrl(q.url)}
                  className="text-xs px-3 py-1.5 rounded-full bg-slate-100 text-slate-600 hover:bg-accent/10 hover:text-accent transition font-medium"
                >
                  {q.label}
                </button>
              ))}
            </div>

            <AnimatePresence mode="wait">
              {previewLoading && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                  className="p-4 rounded-xl bg-slate-50 border border-slate-100 text-sm text-slate-400">
                  Detecting problem...
                </motion.div>
              )}
              {!previewLoading && preview && (
                <motion.div
                  initial={{ opacity: 0, y: 4 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  className={`p-4 rounded-xl border ${preview.found ? 'bg-teal-50/80 border-teal-200' : 'bg-amber-50/80 border-amber-200'}`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className={`text-xs font-semibold mb-1 ${preview.found ? 'text-accent' : 'text-amber-600'}`}>
                        {preview.found ? '✓ Detected' : '⚠ Not in dataset'}
                      </p>
                      <p className="font-semibold text-primary">{preview.title}</p>
                      <p className="text-xs text-slate-500 mt-1">{formatEnum(preview.platform)} · {preview.slug}</p>
                    </div>
                    {preview.found && (
                      <span className={`text-xs px-2 py-0.5 rounded-full shrink-0 ${difficultyClass(preview.difficulty)}`}>
                        {preview.difficulty}
                      </span>
                    )}
                  </div>
                  {preview.found && (
                    <div className="flex flex-wrap gap-1.5 mt-2">
                      <span className="text-xs px-2 py-0.5 rounded-full bg-white/80 text-slate-600 border border-slate-200">
                        {formatEnum(preview.topic)}
                      </span>
                      <span className="text-xs px-2 py-0.5 rounded-full bg-white/80 text-accent border border-teal-200">
                        {formatPattern(preview.pattern)}
                      </span>
                    </div>
                  )}
                  {!preview.found && (
                    <p className="text-xs text-amber-700 mt-2">{preview.message}</p>
                  )}
                </motion.div>
              )}
            </AnimatePresence>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-sm font-medium text-slate-700 mb-1.5 block">Time (min)</label>
                <input
                  type="number"
                  min={1}
                  value={timeSpent}
                  onChange={(e) => setTimeSpent(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl border border-slate-200 bg-slate-50/50 focus:outline-none focus:ring-2 focus:ring-accent/30 text-sm"
                />
              </div>
              <div className="flex items-end">
                <label className="flex items-center gap-2.5 px-4 py-3 rounded-xl border border-slate-200 bg-slate-50/50 w-full cursor-pointer hover:border-accent/30 transition">
                  <input type="checkbox" checked={hintUsed} onChange={(e) => setHintUsed(e.target.checked)} className="rounded accent-accent w-4 h-4" />
                  <span className="text-sm text-slate-600 font-medium">Hint used</span>
                </label>
              </div>
            </div>

            {error && (
              <div className="p-3 rounded-xl bg-red-50 border border-red-100 text-red-600 text-sm">{error}</div>
            )}

            <motion.button
              whileHover={{ scale: canSave ? 1.01 : 1 }}
              whileTap={{ scale: canSave ? 0.99 : 1 }}
              type="submit"
              disabled={!canSave}
              className="w-full py-3.5 rounded-xl bg-accent text-white font-semibold hover:bg-teal-600 transition disabled:opacity-40 disabled:cursor-not-allowed shadow-lg shadow-accent/20"
            >
              {saving ? 'Saving...' : preview?.found ? 'Save Attempt' : 'Detect a valid problem first'}
            </motion.button>
          </form>

          <AnimatePresence>
            {savedMeta && (
              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: 8 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0 }}
                className="mt-4 p-4 rounded-xl bg-emerald-50 border border-emerald-200"
              >
                <p className="text-emerald-700 font-semibold text-sm mb-2">✓ Logged to your ledger</p>
                <p className="font-bold text-primary">{savedMeta.title}</p>
                <div className="flex flex-wrap gap-2 mt-2">
                  <span className={`text-xs px-2 py-0.5 rounded-full ${difficultyClass(savedMeta.difficulty)}`}>{savedMeta.difficulty}</span>
                  <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">{formatEnum(savedMeta.topic)}</span>
                  <span className="text-xs px-2 py-0.5 rounded-full bg-teal-50 text-accent">{formatPattern(savedMeta.pattern)}</span>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>

        <div className="xl:col-span-3 bg-card rounded-[20px] p-6 md:p-7 border border-slate-100 shadow-sm">
          <h2 className="text-lg font-bold text-primary mb-1">Sheet Intelligence</h2>
          <p className="text-sm text-slate-500 mb-6">Progress across curated interview sheets</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {data.sheetProgress?.map((sheet, i) => (
              <motion.div
                key={sheet.name}
                initial={{ opacity: 0, x: 12 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.06 }}
                className="group p-5 rounded-2xl bg-gradient-to-br from-slate-50 to-white border border-slate-100 hover:border-accent/30 hover:shadow-md transition-all duration-300"
              >
                <div className="flex justify-between items-start mb-3">
                  <h3 className="font-bold text-primary group-hover:text-accent transition">{sheet.name}</h3>
                  <span className="text-lg font-extrabold text-accent">{sheet.percentage}%</span>
                </div>
                <p className="text-sm text-slate-500 mb-4">
                  <span className="font-semibold text-primary">{sheet.solved}</span> / {sheet.total} solved
                  <span className="mx-1.5 text-slate-300">·</span>
                  {sheet.patternsCovered} patterns
                </p>
                <div className="h-2.5 bg-slate-200/80 rounded-full overflow-hidden">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${Math.max(sheet.percentage, 2)}%` }}
                    transition={{ duration: 1.2, delay: 0.15 + i * 0.08, ease: 'easeOut' }}
                    className="h-full bg-gradient-to-r from-accent to-teal-400 rounded-full"
                  />
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </div>

      <div className="bg-card rounded-[20px] p-6 md:p-7 border border-slate-100 shadow-sm">
        <h2 className="text-lg font-bold text-primary mb-1">Pattern Snapshot</h2>
        <p className="text-sm text-slate-500 mb-6">Your most practiced interview patterns</p>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {data.topPatterns?.map((p, i) => (
            <motion.div
              key={p.pattern}
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: i * 0.08 }}
              className="relative p-5 rounded-2xl bg-gradient-to-br from-primary/5 via-transparent to-accent/5 border border-slate-100 hover:border-accent/20 transition group"
            >
              <p className="text-sm text-slate-500 group-hover:text-accent transition">{p.label}</p>
              <p className="text-3xl font-extrabold text-primary mt-2 tracking-tight">
                <CountUp end={p.count} />
              </p>
            </motion.div>
          ))}
        </div>
      </div>

      <div className="bg-card rounded-[20px] p-6 md:p-7 border border-slate-100 shadow-sm overflow-hidden">
        <h2 className="text-lg font-bold text-primary mb-5">Recent Attempts</h2>
        <div className="overflow-x-auto -mx-2">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-slate-400 border-b border-slate-100">
                <th className="pb-3 px-2 font-semibold">Problem</th>
                <th className="pb-3 px-2 font-semibold">Pattern</th>
                <th className="pb-3 px-2 font-semibold">Difficulty</th>
                <th className="pb-3 px-2 font-semibold">Time</th>
                <th className="pb-3 px-2 font-semibold">Hint</th>
                <th className="pb-3 px-2 font-semibold">Date</th>
              </tr>
            </thead>
            <tbody>
              {data.recentAttempts?.map((a) => (
                <tr key={a.id} className="border-b border-slate-50 hover:bg-slate-50/80 transition">
                  <td className="py-3.5 px-2 font-semibold text-primary">{a.title}</td>
                  <td className="py-3.5 px-2">
                    <span className="text-xs px-2 py-0.5 rounded-full bg-teal-50 text-accent">{formatPattern(a.pattern)}</span>
                  </td>
                  <td className="py-3.5 px-2">
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${difficultyClass(a.difficulty)}`}>{a.difficulty}</span>
                  </td>
                  <td className="py-3.5 px-2 text-slate-600">{a.timeSpentMinutes}m</td>
                  <td className="py-3.5 px-2">{a.hintUsed ? <span className="text-amber-500">✓</span> : <span className="text-slate-300">—</span>}</td>
                  <td className="py-3.5 px-2 text-slate-500">{formatDate(a.solvedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function HeroPill({ label, value, suffix = '', icon }) {
  return (
    <div className="flex items-center gap-3 px-5 py-3 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-sm">
      <span className="text-xl">{icon}</span>
      <div>
        <p className="text-slate-400 text-xs font-medium">{label}</p>
        <p className="text-xl font-bold">
          <CountUp end={value} />
          {suffix && <span className="text-sm font-normal text-slate-400 ml-0.5">{suffix}</span>}
        </p>
      </div>
    </div>
  );
}
