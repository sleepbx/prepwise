import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer,
  RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar,
  PieChart, Pie, Cell, BarChart, Bar,
} from 'recharts';
import api from '../api/axios';
import CountUp from '../components/CountUp';

const COLORS = ['#14B8A6', '#3B82F6', '#F59E0B', '#EC4899', '#8B5CF6', '#EF4444'];
const DIFF_COLORS = { EASY: '#16a34a', MEDIUM: '#d97706', HARD: '#dc2626' };

export default function Analytics() {
  const [velocity, setVelocity] = useState([]);
  const [radar, setRadar] = useState([]);
  const [difficulty, setDifficulty] = useState([]);
  const [topics, setTopics] = useState([]);
  const [patterns, setPatterns] = useState([]);
  const [timeAnalysis, setTimeAnalysis] = useState(null);
  const [hints, setHints] = useState([]);
  const [improvement, setImprovement] = useState(null);
  const [weakZones, setWeakZones] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.get('/analytics/velocity'),
      api.get('/analytics/radar'),
      api.get('/analytics/difficulty'),
      api.get('/analytics/topics'),
      api.get('/analytics/patterns'),
      api.get('/analytics/time-analysis'),
      api.get('/analytics/hints'),
      api.get('/analytics/improvement'),
      api.get('/analytics/weak-zones'),
    ]).then(([v, r, d, t, p, ta, h, imp, wz]) => {
      setVelocity(v.data);
      setRadar(r.data);
      setDifficulty(d.data);
      setTopics(t.data.slice(0, 8));
      setPatterns(p.data.slice(0, 8));
      setTimeAnalysis(ta.data);
      setHints(h.data);
      setImprovement(imp.data);
      setWeakZones(wz.data);
      setLoading(false);
    });
  }, []);

  if (loading) {
    return <div className="flex items-center justify-center h-screen text-slate-400">Loading analytics...</div>;
  }

  return (
    <div className="p-8 space-y-8">
      <motion.section
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="gradient-hero rounded-[20px] p-8 text-white"
      >
        <h1 className="text-3xl font-bold mb-2">Striver-style clarity with a real analytics layer.</h1>
        <p className="text-slate-300">Pattern confidence, velocity trends, and improvement signals — all in one view.</p>
      </motion.section>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCard title="Daily Solve Velocity">
          <ResponsiveContainer width="100%" height={280}>
            <AreaChart data={velocity}>
              <defs>
                <linearGradient id="velGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#14B8A6" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#14B8A6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis dataKey="date" tick={{ fontSize: 10 }} tickFormatter={(d) => d.slice(5)} />
              <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
              <Tooltip />
              <Area type="monotone" dataKey="count" stroke="#14B8A6" fill="url(#velGrad)" strokeWidth={2} />
            </AreaChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Pattern Radar">
          <ResponsiveContainer width="100%" height={280}>
            <RadarChart data={radar}>
              <PolarGrid stroke="#e2e8f0" />
              <PolarAngleAxis dataKey="pattern" tick={{ fontSize: 10 }} />
              <PolarRadiusAxis angle={30} domain={[0, 100]} tick={{ fontSize: 9 }} />
              <Radar dataKey="confidence" stroke="#14B8A6" fill="#14B8A6" fillOpacity={0.25} strokeWidth={2} />
              <Tooltip />
            </RadarChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <ChartCard title="Difficulty Mix">
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie data={difficulty} dataKey="count" nameKey="label" cx="50%" cy="50%" innerRadius={50} outerRadius={80} paddingAngle={3}>
                {difficulty.map((entry) => (
                  <Cell key={entry.label} fill={DIFF_COLORS[entry.label] || COLORS[0]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
          <div className="flex justify-center gap-4 mt-2">
            {difficulty.map((d) => (
              <span key={d.label} className="text-xs text-slate-500">
                <span className="inline-block w-2 h-2 rounded-full mr-1" style={{ background: DIFF_COLORS[d.label] }} />
                {d.label} ({d.count})
              </span>
            ))}
          </div>
        </ChartCard>

        <ChartCard title="Topic Distribution">
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={topics} layout="vertical" margin={{ left: 60 }}>
              <XAxis type="number" tick={{ fontSize: 10 }} />
              <YAxis type="category" dataKey="label" tick={{ fontSize: 10 }} width={55} />
              <Tooltip />
              <Bar dataKey="count" fill="#3B82F6" radius={[0, 6, 6, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Pattern Distribution">
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={patterns} layout="vertical" margin={{ left: 80 }}>
              <XAxis type="number" tick={{ fontSize: 10 }} />
              <YAxis type="category" dataKey="label" tick={{ fontSize: 9 }} width={75} />
              <Tooltip />
              <Bar dataKey="count" fill="#14B8A6" radius={[0, 6, 6, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-card rounded-[20px] p-6 border border-slate-100">
          <h3 className="font-bold text-primary mb-4">Time Analytics</h3>
          <p className="text-sm text-slate-500 mb-4">Average solve time by difficulty</p>
          <div className="grid grid-cols-3 gap-4">
            {['easy', 'medium', 'hard'].map((d) => (
              <div key={d} className="text-center p-4 rounded-2xl bg-slate-50">
                <p className={`text-xs font-medium mb-1 capitalize difficulty-${d}`}>{d}</p>
                <p className="text-2xl font-bold text-primary">
                  <CountUp end={timeAnalysis?.[d] || 0} suffix="m" decimals={1} />
                </p>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-card rounded-[20px] p-6 border border-slate-100">
          <h3 className="font-bold text-primary mb-4">Hint Analytics</h3>
          <p className="text-sm text-slate-500 mb-4">Hint usage by difficulty</p>
          <div className="space-y-3">
            {hints.map((h) => (
              <div key={h.difficulty}>
                <div className="flex justify-between text-sm mb-1">
                  <span className="font-medium">{h.difficulty}</span>
                  <span className="text-slate-500">{h.hintPercent}% ({h.total} problems)</span>
                </div>
                <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${h.hintPercent}%` }}
                    className="h-full bg-secondary-pink rounded-full"
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="bg-card rounded-[20px] p-6 border border-slate-100">
        <h3 className="font-bold text-primary mb-4">Improvement Trends</h3>
        <p className="text-sm text-slate-500 mb-6">Last 7 days vs previous 7 days</p>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <TrendCard
            label="Problems Solved"
            diff={improvement?.problemsSolvedDiff}
            current={improvement?.last7Problems}
            previous={improvement?.prev7Problems}
            suffix=""
          />
          <TrendCard
            label="Avg Solve Time"
            diff={improvement?.avgSolveTimeDiff}
            current={improvement?.last7AvgTime}
            previous={improvement?.prev7AvgTime}
            suffix="m"
            invert
          />
          <TrendCard
            label="Hint Usage"
            diff={improvement?.hintUsageDiff}
            current={improvement?.last7HintPercent}
            previous={improvement?.prev7HintPercent}
            suffix="%"
            invert
          />
        </div>
      </div>

      <div className="bg-card rounded-[20px] p-6 border border-slate-100">
        <h3 className="font-bold text-primary mb-4">Weak Zones</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {weakZones.map((zone, i) => (
            <motion.div
              key={zone.pattern}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
              className="p-4 rounded-2xl bg-red-50/50 border border-red-100"
            >
              <p className="font-semibold text-primary text-sm">{zone.label}</p>
              <p className="text-xs text-red-500 mt-1">{zone.reason}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </div>
  );
}

function ChartCard({ title, children }) {
  return (
    <div className="bg-card rounded-[20px] p-6 border border-slate-100">
      <h3 className="font-bold text-primary mb-4">{title}</h3>
      {children}
    </div>
  );
}

function TrendCard({ label, diff, current, previous, suffix, invert = false }) {
  const isPositive = diff > 0;
  const isGood = invert ? !isPositive : isPositive;
  const color = diff === 0 ? 'text-slate-500' : isGood ? 'text-emerald-600' : 'text-red-500';
  const arrow = diff > 0 ? '↑' : diff < 0 ? '↓' : '→';

  return (
    <div className="p-5 rounded-2xl bg-slate-50 border border-slate-100">
      <p className="text-sm text-slate-500 mb-2">{label}</p>
      <p className={`text-2xl font-bold ${color}`}>
        {arrow} {Math.abs(diff || 0).toFixed(1)}{suffix}
      </p>
      <p className="text-xs text-slate-400 mt-2">
        {current?.toFixed?.(1) ?? current}{suffix} vs {previous?.toFixed?.(1) ?? previous}{suffix}
      </p>
    </div>
  );
}
