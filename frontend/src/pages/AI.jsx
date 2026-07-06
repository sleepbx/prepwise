import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  RadialBarChart, RadialBar, ResponsiveContainer,
  BarChart, Bar, XAxis, YAxis, Tooltip, Cell,
} from 'recharts';
import api from '../api/axios';
import CountUp from '../components/CountUp';

const COMPANIES = ['Google', 'Amazon', 'Microsoft', 'Adobe', 'Meta', 'Uber', 'Stripe', 'Atlassian'];

const RISK_CONFIG = {
  LOW:    { color: 'text-emerald-600', bg: 'bg-emerald-50', border: 'border-emerald-200' },
  MEDIUM: { color: 'text-amber-600',   bg: 'bg-amber-50',   border: 'border-amber-200' },
  HIGH:   { color: 'text-red-600',     bg: 'bg-red-50',     border: 'border-red-200' },
};

const LEVEL_COLOR = {
  EARLY_STAGE:      '#ef4444',
  ON_TRACK:         '#f59e0b',
  INTERVIEW_READY:  '#14b8a6',
  STRONG_CANDIDATE: '#3b82f6',
};

const TREND_CONFIG = {
  IMPROVING: { icon: '↑', color: 'text-emerald-600' },
  STABLE:    { icon: '→', color: 'text-slate-500'   },
  DECLINING: { icon: '↓', color: 'text-red-500'     },
};

const URGENCY_CONFIG = {
  IMMEDIATE: { color: 'bg-red-50 text-red-700 border-red-200',        label: 'Immediate' },
  THIS_WEEK: { color: 'bg-amber-50 text-amber-700 border-amber-200',   label: 'This Week' },
  LATER:     { color: 'bg-slate-100 text-slate-600 border-slate-200',  label: 'Later'     },
};

export default function AI() {
  const [readiness, setReadiness]         = useState(null);
  const [coach, setCoach]                 = useState(null);
  const [weaknesses, setWeaknesses]       = useState([]);
  const [revisions, setRevisions]         = useState([]);
  const [insight, setInsight]             = useState(null);
  const [companyAdvice, setCompanyAdvice] = useState(null);
  const [studyPlan, setStudyPlan]         = useState(null);

  const [company, setCompany]             = useState('Google');
  const [coachCompany, setCoachCompany]   = useState('Google');
  const [coachPrompt, setCoachPrompt]     = useState('What should I improve today and why?');
  const [planCompany, setPlanCompany]     = useState('Google');
  const [planDate, setPlanDate]           = useState('');
  const [planHours, setPlanHours]         = useState(2);

  const [coachLoading, setCoachLoading]   = useState(false);
  const [planLoading, setPlanLoading]     = useState(false);
  const [adviceLoading, setAdviceLoading] = useState(false);
  const [loading, setLoading]             = useState(true);
  const [loadError, setLoadError]         = useState(null);
  const [activeTab, setActiveTab]         = useState('overview');

  useEffect(() => {
    const load = async () => {
      const results = await Promise.allSettled([
        api.get('/ai/readiness'),
        api.get('/ai/daily-coach'),
        api.get('/ai/weakness-analysis'),
        api.get('/ai/revision-priorities'),
        api.get('/ai/insights'),
      ]);

      const allFailed = results.every(r => r.status === 'rejected');
      if (allFailed) setLoadError('Could not reach AI services. Is the backend running on port 8080?');
      else if (results.some(r => r.status === 'rejected')) setLoadError('Some insights could not load. Showing available data.');

      if (results[0].status === 'fulfilled') setReadiness(results[0].value.data);
      if (results[1].status === 'fulfilled') setCoach(results[1].value.data);
      if (results[2].status === 'fulfilled') setWeaknesses(results[2].value.data.slice(0, 8));
      if (results[3].status === 'fulfilled') setRevisions(results[3].value.data.slice(0, 12));
      if (results[4].status === 'fulfilled') setInsight(results[4].value.data);
      setLoading(false);
    };
    load();
  }, []);

  const getCompanyAdvice = async () => {
    setAdviceLoading(true);
    try {
      const { data } = await api.post('/ai/company-advisor', { company });
      setCompanyAdvice(data);
    } finally {
      setAdviceLoading(false);
    }
  };

  const askCoach = async () => {
    setCoachLoading(true);
    try {
      const { data } = await api.post('/ai/daily-coach', { company: coachCompany, prompt: coachPrompt });
      setCoach(data);
    } finally {
      setCoachLoading(false);
    }
  };

  const generatePlan = async () => {
    if (!planDate) return;
    setPlanLoading(true);
    try {
      const { data } = await api.post('/ai/study-plan', {
        targetCompany: planCompany,
        interviewDate: planDate,
        hoursPerDay: planHours,
        experienceLevel: 'INTERMEDIATE',
      });
      setStudyPlan(data);
    } finally {
      setPlanLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-screen gap-4 text-slate-400">
        <div className="w-10 h-10 border-2 border-accent border-t-transparent rounded-full animate-spin" />
        <p className="text-sm font-medium">Loading intelligence layer…</p>
      </div>
    );
  }

  const levelColor = LEVEL_COLOR[readiness?.level] || '#14b8a6';
  const risk = readiness?.hiringRisk || 'MEDIUM';
  const riskCfg = RISK_CONFIG[risk];

  return (
    <div className="p-6 md:p-8 space-y-8 max-w-[1400px]">

      {/* Hero */}
      <motion.section
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="gradient-hero rounded-[20px] p-8 md:p-10 text-white relative overflow-hidden"
      >
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_rgba(59,130,246,0.12),_transparent_55%)]" />
        <div className="relative z-10 flex flex-col md:flex-row md:items-center gap-6">
          <div className="flex-1">
            <p className="text-accent text-sm font-semibold tracking-widest uppercase mb-2">AI Analytics</p>
            <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight mb-2">Interview Readiness Dashboard</h1>
            <p className="text-slate-300 max-w-2xl text-sm">
              Data-backed insights computed from your attempt history. No generic advice — every recommendation is tied to your actual metrics.
            </p>
          </div>
          {readiness && (
            <div className="flex items-center gap-4 shrink-0">
              <div className="text-center">
                <p className="text-5xl font-black" style={{ color: levelColor }}>
                  {Math.round(readiness.score)}
                </p>
                <p className="text-xs text-slate-400 mt-1">Readiness Score</p>
              </div>
              <div className={`px-4 py-2 rounded-xl border text-sm font-bold ${riskCfg.bg} ${riskCfg.border} ${riskCfg.color}`}>
                {risk} RISK
              </div>
            </div>
          )}
        </div>
      </motion.section>

      {loadError && (
        <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-sm flex gap-2">
          <span>⚠</span> {loadError}
        </div>
      )}

      {/* Tab navigation */}
      <div className="flex gap-2 border-b border-slate-100">
        {[
          { id: 'overview',  label: 'Overview' },
          { id: 'weaknesses', label: 'Weakness Analysis' },
          { id: 'revisions', label: 'Revision Priorities' },
          { id: 'company',   label: 'Company Advisor' },
          { id: 'plan',      label: 'Study Plan' },
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-4 py-2.5 text-sm font-semibold rounded-t-lg transition-all border-b-2 -mb-px ${
              activeTab === tab.id
                ? 'border-accent text-accent bg-accent/5'
                : 'border-transparent text-slate-500 hover:text-primary'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <AnimatePresence mode="wait">
        {activeTab === 'overview' && (
          <motion.div key="overview" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="space-y-6">
            <OverviewTab
              readiness={readiness}
              coach={coach}
              insight={insight}
              coachCompany={coachCompany}
              setCoachCompany={setCoachCompany}
              coachPrompt={coachPrompt}
              setCoachPrompt={setCoachPrompt}
              coachLoading={coachLoading}
              askCoach={askCoach}
              levelColor={levelColor}
              riskCfg={riskCfg}
              risk={risk}
            />
          </motion.div>
        )}

        {activeTab === 'weaknesses' && (
          <motion.div key="weaknesses" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <WeaknessTab weaknesses={weaknesses} />
          </motion.div>
        )}

        {activeTab === 'revisions' && (
          <motion.div key="revisions" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <RevisionTab revisions={revisions} />
          </motion.div>
        )}

        {activeTab === 'company' && (
          <motion.div key="company" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <CompanyTab
              company={company}
              setCompany={setCompany}
              companyAdvice={companyAdvice}
              adviceLoading={adviceLoading}
              getCompanyAdvice={getCompanyAdvice}
            />
          </motion.div>
        )}

        {activeTab === 'plan' && (
          <motion.div key="plan" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <StudyPlanTab
              planCompany={planCompany}
              setPlanCompany={setPlanCompany}
              planDate={planDate}
              setPlanDate={setPlanDate}
              planHours={planHours}
              setPlanHours={setPlanHours}
              studyPlan={studyPlan}
              planLoading={planLoading}
              generatePlan={generatePlan}
            />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

/* ─── Overview Tab ──────────────────────────────────────────── */
function OverviewTab({ readiness, coach, insight, coachCompany, setCoachCompany, coachPrompt, setCoachPrompt, coachLoading, askCoach, levelColor, riskCfg, risk }) {
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <ReadinessCard readiness={readiness} levelColor={levelColor} riskCfg={riskCfg} risk={risk} />
        <CoachCard
          coach={coach}
          coachCompany={coachCompany}
          setCoachCompany={setCoachCompany}
          coachPrompt={coachPrompt}
          setCoachPrompt={setCoachPrompt}
          coachLoading={coachLoading}
          askCoach={askCoach}
        />
      </div>
      <InsightCard insight={insight} />
    </div>
  );
}

/* ─── Readiness Card ─────────────────────────────────────────── */
function ReadinessCard({ readiness, levelColor, riskCfg, risk }) {
  if (!readiness) return <Card title="Interview Readiness"><p className="text-slate-400 text-sm">Loading…</p></Card>;

  const levelLabel = readiness.level?.replace(/_/g, ' ') || '';

  return (
    <Card title="Interview Readiness" badge={levelLabel}>
      <div className="flex items-start gap-4 mb-4">
        <div className="relative w-24 h-24 shrink-0">
          <ResponsiveContainer width="100%" height="100%">
            <RadialBarChart cx="50%" cy="50%" innerRadius="68%" outerRadius="100%"
              data={[{ value: readiness.score }]} startAngle={90} endAngle={-270}>
              <RadialBar dataKey="value" cornerRadius={6} fill={levelColor} background={{ fill: '#f1f5f9' }} />
            </RadialBarChart>
          </ResponsiveContainer>
          <div className="absolute inset-0 flex items-center justify-center flex-col">
            <span className="text-xl font-extrabold text-primary leading-none">
              <CountUp end={Math.round(readiness.score)} />
            </span>
            <span className="text-[9px] text-slate-400 font-medium">/ 100</span>
          </div>
        </div>
        <div className="flex-1 space-y-1">
          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">Breakdown</p>
          {readiness.breakdown && Object.entries(readiness.breakdown).map(([k, v]) => (
            <div key={k}>
              <div className="flex justify-between text-xs mb-0.5">
                <span className="text-slate-500 capitalize">{k}</span>
                <span className="font-bold text-primary">{v}</span>
              </div>
              <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
                <div className="h-full rounded-full transition-all duration-700"
                  style={{ width: `${v}%`, backgroundColor: v >= 70 ? '#14b8a6' : v >= 40 ? '#f59e0b' : '#ef4444' }} />
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="flex gap-2 mb-3">
        <div className={`flex-1 px-3 py-2 rounded-xl border text-center ${riskCfg.bg} ${riskCfg.border}`}>
          <p className="text-[10px] text-slate-500 font-medium">Hiring Risk</p>
          <p className={`text-sm font-bold ${riskCfg.color}`}>{risk}</p>
        </div>
        <div className="flex-1 px-3 py-2 rounded-xl border border-blue-100 bg-blue-50 text-center">
          <p className="text-[10px] text-slate-500 font-medium">Expected Outcome</p>
          <p className="text-sm font-bold text-secondary-blue leading-tight">{readiness.expectedOutcome}</p>
        </div>
      </div>

      {(readiness.blind75Solved !== undefined || readiness.currentStreak !== undefined) && (
        <div className="flex gap-3 mb-3">
          {readiness.blind75Solved !== undefined && (
            <Stat label="Blind 75" value={`${readiness.blind75Solved}/75`} />
          )}
          {readiness.currentStreak !== undefined && (
            <Stat label="Streak" value={`${readiness.currentStreak}d`} />
          )}
        </div>
      )}

      {readiness.explanation && (
        <p className="text-xs text-slate-600 leading-relaxed border-t border-slate-100 pt-3">
          {readiness.explanation}
        </p>
      )}

      {readiness.improvementAreas?.length > 0 && (
        <div className="mt-3 space-y-1">
          {readiness.improvementAreas.map((a, i) => (
            <div key={i} className="flex items-start gap-1.5 text-xs text-slate-600">
              <span className="text-amber-500 mt-0.5">•</span>{a}
            </div>
          ))}
        </div>
      )}
    </Card>
  );
}

/* ─── Daily Coach Card ───────────────────────────────────────── */
function CoachCard({ coach, coachCompany, setCoachCompany, coachPrompt, setCoachPrompt, coachLoading, askCoach }) {
  return (
    <Card title="Daily Coach" className="lg:col-span-2">
      <div className="flex flex-col sm:flex-row gap-2 mb-4">
        <select value={coachCompany} onChange={e => setCoachCompany(e.target.value)}
          className="px-3 py-2 rounded-xl border border-slate-200 text-sm bg-white min-w-[140px]">
          {COMPANIES.map(c => <option key={c}>{c}</option>)}
        </select>
        <input value={coachPrompt} onChange={e => setCoachPrompt(e.target.value)}
          className="flex-1 px-3 py-2 rounded-xl border border-slate-200 text-sm"
          placeholder="Ask what to improve for your target company" />
        <button onClick={askCoach} disabled={coachLoading || !coachPrompt.trim()}
          className="px-5 py-2 rounded-xl bg-primary text-white font-semibold text-sm hover:bg-slate-800 transition disabled:opacity-60 shrink-0">
          {coachLoading ? <span className="flex items-center gap-2"><span className="w-3 h-3 border border-white border-t-transparent rounded-full animate-spin" />Asking…</span> : 'Ask'}
        </button>
      </div>

      {coach && (
        <div className="space-y-3">
          {coach.warning && (
            <div className="flex items-center gap-2 p-3 rounded-xl bg-red-50 border border-red-100 text-sm text-red-700">
              <span>⚠</span> {coach.warning}
            </div>
          )}

          {coach.yesterdaySummary && (
            <div className="text-xs text-slate-500 px-1">Yesterday: {coach.yesterdaySummary}</div>
          )}

          <p className="text-base font-bold text-primary">{coach.headline}</p>

          <ul className="space-y-1.5">
            {coach.tasks?.map((task, i) => (
              <li key={i} className="flex items-start gap-2 text-sm text-slate-600">
                <span className="text-accent mt-0.5 font-bold">{i + 1}.</span>{task}
              </li>
            ))}
          </ul>

          {coach.measurableGoal && (
            <div className="p-3 rounded-xl bg-teal-50 border border-teal-100">
              <p className="text-xs font-semibold text-accent mb-0.5">Today's Goal</p>
              <p className="text-sm text-teal-800">{coach.measurableGoal}</p>
            </div>
          )}

          {coach.highestROITask && (
            <div className="flex items-start gap-2 p-2.5 rounded-xl bg-blue-50 border border-blue-100 text-sm text-slate-700">
              <span className="text-secondary-blue font-bold shrink-0">★</span>
              <span><span className="font-semibold text-secondary-blue">Highest ROI:</span> {coach.highestROITask}</span>
            </div>
          )}

          <div className="flex items-center justify-between border-t border-slate-100 pt-3">
            <span className="text-xs px-3 py-1 rounded-full bg-teal-50 text-accent font-semibold border border-teal-100">
              {coach.estimatedImpact}
            </span>
            {coach.currentStreak > 0 && (
              <span className="text-xs text-slate-500">🔥 {coach.currentStreak} day streak</span>
            )}
          </div>

          {coach.message && (
            <p className="text-xs text-slate-500 leading-relaxed border-t border-slate-100 pt-3">
              {coach.message}
            </p>
          )}
        </div>
      )}
    </Card>
  );
}

/* ─── Insights Card ──────────────────────────────────────────── */
function InsightCard({ insight }) {
  if (!insight) return null;
  const { changes = {} } = insight;

  const trendCfg = TREND_CONFIG[insight.trend] || TREND_CONFIG.STABLE;

  const statItems = [
    { key: 'last7Problems',      label: 'This Week',   suffix: ' solved' },
    { key: 'prev7Problems',      label: 'Last Week',   suffix: ' solved' },
    { key: 'problemsSolvedDiff', label: 'Δ Problems',  suffix: '',        delta: true },
    { key: 'last7ActiveDays',    label: 'Active Days', suffix: '/7' },
    { key: 'avgSolveTimeDiff',   label: 'Δ Solve Time',suffix: 'm',       delta: true },
    { key: 'hintUsageDiff',      label: 'Δ Hint %',    suffix: '%',       delta: true },
  ];

  return (
    <Card title="Week-over-Week Intelligence"
      badge={insight.trend ? `${trendCfg.icon} ${insight.trend}` : undefined}>
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-4">
        {statItems.map(({ key, label, suffix, delta }) => {
          const v = changes[key];
          if (v === undefined) return null;
          const num = typeof v === 'number' ? v : parseFloat(v);
          const color = delta
            ? (num > 0 ? 'text-emerald-600' : num < 0 ? 'text-red-500' : 'text-slate-500')
            : 'text-primary';
          return (
            <div key={key} className="p-3 rounded-xl bg-slate-50 border border-slate-100 text-center">
              <p className="text-[10px] text-slate-400 font-medium uppercase tracking-wide mb-1">{label}</p>
              <p className={`text-lg font-extrabold ${color}`}>
                {delta && num > 0 ? '+' : ''}{isNaN(num) ? v : num}{suffix}
              </p>
            </div>
          );
        })}
      </div>

      {(insight.biggestWin || insight.biggestRisk) && (
        <div className="flex flex-col sm:flex-row gap-3 mb-4">
          {insight.biggestWin && (
            <div className="flex-1 p-3 rounded-xl bg-emerald-50 border border-emerald-100 flex items-start gap-2">
              <span className="text-emerald-600 font-bold shrink-0">↑</span>
              <div>
                <p className="text-[10px] font-semibold text-emerald-700 mb-0.5">Biggest Win</p>
                <p className="text-xs text-emerald-800">{insight.biggestWin}</p>
              </div>
            </div>
          )}
          {insight.biggestRisk && (
            <div className="flex-1 p-3 rounded-xl bg-red-50 border border-red-100 flex items-start gap-2">
              <span className="text-red-500 font-bold shrink-0">↓</span>
              <div>
                <p className="text-[10px] font-semibold text-red-700 mb-0.5">Risk Signal</p>
                <p className="text-xs text-red-800">{insight.biggestRisk}</p>
              </div>
            </div>
          )}
        </div>
      )}

      {insight.insight && (
        <p className="text-sm text-slate-600 leading-relaxed border-t border-slate-100 pt-3">
          {insight.insight}
        </p>
      )}
    </Card>
  );
}

/* ─── Weakness Analysis Tab ──────────────────────────────────── */
function WeaknessTab({ weaknesses }) {
  const [filter, setFilter] = useState('ALL');
  const filtered = filter === 'ALL' ? weaknesses : weaknesses.filter(w => w.classification === filter);

  const chartData = weaknesses
    .filter(w => w.weaknessIndex > 20)
    .slice(0, 6)
    .map(w => ({
      name: w.topic,
      index: w.weaknessIndex,
      fill: w.classification === 'WEAK' ? '#ef4444' : w.classification === 'MODERATE' ? '#f59e0b' : '#14b8a6',
    }));

  return (
    <div className="space-y-6">
      {chartData.length > 0 && (
        <Card title="Weakness Index by Topic">
          <ResponsiveContainer width="100%" height={180}>
            <BarChart data={chartData} layout="vertical" margin={{ left: 16, right: 16 }}>
              <XAxis type="number" domain={[0, 100]} tick={{ fontSize: 10 }} />
              <YAxis type="category" dataKey="name" tick={{ fontSize: 11 }} width={90} />
              <Tooltip formatter={v => [`${v}`, 'Weakness Index']} />
              <Bar dataKey="index" radius={[0, 6, 6, 0]}>
                {chartData.map((e, i) => <Cell key={i} fill={e.fill} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
          <div className="flex gap-3 mt-3 text-xs text-slate-500">
            <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-red-400 inline-block" />Weak (&gt;60)</span>
            <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-amber-400 inline-block" />Moderate (30–60)</span>
            <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-teal-400 inline-block" />Strong (&lt;30)</span>
          </div>
        </Card>
      )}

      <Card title="Topic Analysis">
        <div className="flex gap-2 mb-4">
          {['ALL', 'WEAK', 'MODERATE', 'STRONG'].map(f => (
            <button key={f} onClick={() => setFilter(f)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${filter === f ? 'bg-primary text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}>
              {f}
            </button>
          ))}
        </div>
        <div className="space-y-3">
          {filtered.map(w => <WeaknessRow key={w.topic} w={w} />)}
          {filtered.length === 0 && <p className="text-slate-400 text-sm">No topics in this category yet.</p>}
        </div>
      </Card>
    </div>
  );
}

function WeaknessRow({ w }) {
  const [expanded, setExpanded] = useState(false);
  const trendCfg = TREND_CONFIG[w.recentTrend] || TREND_CONFIG.STABLE;
  const classColor = {
    WEAK:     'bg-red-50 text-red-600 border-red-100',
    MODERATE: 'bg-amber-50 text-amber-600 border-amber-100',
    STRONG:   'bg-teal-50 text-teal-600 border-teal-100',
  }[w.classification] || 'bg-slate-50 text-slate-600 border-slate-100';

  return (
    <motion.div layout className="border border-slate-100 rounded-xl overflow-hidden">
      <button onClick={() => setExpanded(x => !x)}
        className="w-full p-4 text-left hover:bg-slate-50 transition">
        <div className="flex items-center gap-3">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="font-semibold text-primary text-sm">{w.topic}</span>
              <span className={`text-[10px] px-2 py-0.5 rounded-full border font-bold ${classColor}`}>{w.classification}</span>
              {w.recentTrend && (
                <span className={`text-xs font-bold ${trendCfg.color}`}>{trendCfg.icon} {w.recentTrend}</span>
              )}
            </div>
            <div className="flex gap-4 text-xs text-slate-500">
              <span>Coverage: <strong className="text-primary">{w.coverage}%</strong></span>
              <span>Accuracy: <strong className="text-primary">{w.accuracy}%</strong></span>
              <span>Avg time: <strong className="text-primary">{w.averageSolveTime}m</strong></span>
            </div>
          </div>
          <div className="flex items-center gap-3 shrink-0">
            {w.interviewImportance && (
              <div className="text-center">
                <div className="w-8 h-8 rounded-full border-2 flex items-center justify-center text-xs font-bold"
                  style={{
                    borderColor: w.interviewImportance >= 80 ? '#ef4444' : w.interviewImportance >= 65 ? '#f59e0b' : '#94a3b8',
                    color: w.interviewImportance >= 80 ? '#ef4444' : w.interviewImportance >= 65 ? '#f59e0b' : '#94a3b8',
                  }}>
                  {w.interviewImportance}
                </div>
                <p className="text-[9px] text-slate-400 mt-0.5">Importance</p>
              </div>
            )}
            <span className="text-slate-300 text-sm">{expanded ? '▲' : '▼'}</span>
          </div>
        </div>
      </button>

      <AnimatePresence>
        {expanded && (
          <motion.div initial={{ height: 0 }} animate={{ height: 'auto' }} exit={{ height: 0 }}
            className="overflow-hidden border-t border-slate-100">
            <div className="p-4 space-y-3 bg-slate-50/50">
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <MiniStat label="Hint Usage" value={`${w.hintUsageRate}%`} />
                <MiniStat label="Revision Rate" value={`${w.revisionCompletionRate}%`} />
                <MiniStat label="Consistency" value={`${w.consistencyScore}%`} />
                <MiniStat label="Weakness Index" value={w.weaknessIndex} />
              </div>

              <div className="flex flex-wrap gap-2 text-xs">
                {w.confidence && (
                  <Badge label="Confidence" value={w.confidence}
                    color={w.confidence === 'HIGH' ? 'teal' : w.confidence === 'MEDIUM' ? 'amber' : 'red'} />
                )}
                {w.readinessImpact && (
                  <Badge label="Readiness Impact" value={w.readinessImpact} color="blue" />
                )}
                {w.estimatedEffort && (
                  <Badge label="Effort" value={w.estimatedEffort} color="slate" />
                )}
              </div>

              {w.recommendation && (
                <p className="text-xs text-slate-600 leading-relaxed">{w.recommendation}</p>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

/* ─── Revision Priority Tab ──────────────────────────────────── */
function RevisionTab({ revisions }) {
  const buckets = {
    IMMEDIATE: revisions.filter(r => r.urgencyCategory === 'IMMEDIATE'),
    THIS_WEEK: revisions.filter(r => r.urgencyCategory === 'THIS_WEEK'),
    LATER:     revisions.filter(r => r.urgencyCategory === 'LATER' || !r.urgencyCategory),
  };

  return (
    <div className="space-y-6">
      {Object.entries(buckets).map(([category, items]) => {
        if (items.length === 0) return null;
        const cfg = URGENCY_CONFIG[category];
        return (
          <Card key={category} title={`${cfg.label} (${items.length})`}>
            <div className="space-y-2">
              {items.map((r, i) => (
                <motion.div key={r.problemId}
                  initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.04 }}
                  className="p-3 rounded-xl border border-slate-100 hover:border-accent/20 bg-white transition">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 flex-wrap mb-1">
                        <p className="font-semibold text-primary text-sm truncate">{r.title}</p>
                        {r.difficulty && (
                          <span className={`text-[10px] px-1.5 py-0.5 rounded font-bold ${
                            r.difficulty === 'HARD' ? 'bg-red-50 text-red-600' :
                            r.difficulty === 'MEDIUM' ? 'bg-amber-50 text-amber-600' :
                            'bg-emerald-50 text-emerald-600'}`}>
                            {r.difficulty}
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-slate-500">{r.reason}</p>
                    </div>
                    <div className="flex flex-col items-end gap-1 shrink-0">
                      <span className={`text-[10px] px-2 py-0.5 rounded-full border font-bold ${cfg.color}`}>
                        {r.priority}
                      </span>
                      {r.daysSinceSolved > 0 && (
                        <span className="text-[10px] text-slate-400">{r.daysSinceSolved}d ago</span>
                      )}
                    </div>
                  </div>
                  {(r.revisionCount !== undefined || r.confidence) && (
                    <div className="flex gap-3 mt-2 text-[10px] text-slate-400">
                      {r.revisionCount !== undefined && <span>Revised {r.revisionCount}× before</span>}
                      {r.confidence && <span>Confidence: {r.confidence}</span>}
                    </div>
                  )}
                </motion.div>
              ))}
            </div>
          </Card>
        );
      })}
      {revisions.length === 0 && (
        <Card title="Revision Priorities">
          <p className="text-slate-400 text-sm">Solve more problems to unlock spaced revision recommendations.</p>
        </Card>
      )}
    </div>
  );
}

/* ─── Company Advisor Tab ────────────────────────────────────── */
function CompanyTab({ company, setCompany, companyAdvice, adviceLoading, getCompanyAdvice }) {
  return (
    <div className="space-y-6">
      <Card title="Company Interview Analyzer">
        <div className="flex gap-3 mb-4">
          <select value={company} onChange={e => setCompany(e.target.value)}
            className="flex-1 px-3 py-2.5 rounded-xl border border-slate-200 text-sm bg-white">
            {COMPANIES.map(c => <option key={c}>{c}</option>)}
          </select>
          <button onClick={getCompanyAdvice} disabled={adviceLoading}
            className="px-6 py-2.5 rounded-xl bg-primary text-white font-semibold text-sm hover:bg-slate-800 transition disabled:opacity-60 flex items-center gap-2">
            {adviceLoading
              ? <><span className="w-3.5 h-3.5 border border-white border-t-transparent rounded-full animate-spin" />Analyzing…</>
              : 'Analyze'}
          </button>
        </div>

        {!companyAdvice && !adviceLoading && (
          <p className="text-slate-400 text-sm">Select a company and click Analyze to see how your preparation aligns with their interview expectations.</p>
        )}
      </Card>

      {companyAdvice && (
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">

          {/* Topic comparison table */}
          {companyAdvice.topicComparison && Object.keys(companyAdvice.topicComparison).length > 0 && (
            <Card title={`${companyAdvice.company} Topic Coverage vs. Expectations`}>
              <div className="space-y-3">
                {Object.entries(companyAdvice.topicComparison).map(([topic, tg]) => {
                  const pct = Math.min(100, tg.current);
                  const isGap = tg.gap > 5;
                  const isSufficient = tg.current >= tg.expected * 0.9;
                  return (
                    <div key={topic}>
                      <div className="flex justify-between items-center text-xs mb-1">
                        <span className="font-semibold text-primary">{topic}</span>
                        <div className="flex items-center gap-2">
                          {isSufficient && <span className="text-[10px] text-emerald-600 font-bold">✓ Sufficient</span>}
                          {isGap && <span className="text-[10px] text-red-500 font-bold">Gap: {tg.gap}%</span>}
                          <span className="text-slate-400">{tg.current}% / {tg.expected}% expected</span>
                        </div>
                      </div>
                      <div className="relative h-2 bg-slate-100 rounded-full overflow-hidden">
                        <div className="absolute h-full rounded-full transition-all duration-700"
                          style={{
                            width: `${pct}%`,
                            backgroundColor: isSufficient ? '#14b8a6' : isGap ? '#ef4444' : '#f59e0b',
                          }} />
                        {/* Expected marker */}
                        <div className="absolute top-0 h-full w-0.5 bg-slate-400"
                          style={{ left: `${tg.expected}%` }} />
                      </div>
                    </div>
                  );
                })}
              </div>
              <p className="text-[10px] text-slate-400 mt-3">Vertical marker = company's expected coverage. Bar = your current coverage.</p>
            </Card>
          )}

          {/* High ROI items */}
          {companyAdvice.highROIItems?.length > 0 && (
            <Card title="Highest-ROI Improvements">
              <div className="space-y-3">
                {companyAdvice.highROIItems.map((item, i) => (
                  <div key={item.topic} className="flex items-center gap-4 p-3 rounded-xl bg-slate-50 border border-slate-100">
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-extrabold text-white shrink-0 ${
                      i === 0 ? 'bg-primary' : i === 1 ? 'bg-slate-600' : 'bg-slate-400'}`}>
                      {i + 1}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-primary text-sm">{item.topic}</p>
                      <p className="text-xs text-slate-500">{item.expectedGain}</p>
                    </div>
                    <span className={`text-[10px] px-2 py-1 rounded-full font-bold border ${
                      item.effortLevel === 'HIGH' ? 'bg-red-50 text-red-600 border-red-100' :
                      item.effortLevel === 'MEDIUM' ? 'bg-amber-50 text-amber-600 border-amber-100' :
                      'bg-emerald-50 text-emerald-600 border-emerald-100'}`}>
                      {item.effortLevel} EFFORT
                    </span>
                  </div>
                ))}
              </div>
            </Card>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Focus areas + allocation */}
            <Card title="Weekly Time Allocation">
              <div className="space-y-3">
                {companyAdvice.focusAreas?.map((area, i) => {
                  const alloc = companyAdvice.weeklyAllocation?.[area];
                  const pct = alloc ? parseInt(alloc) : 33;
                  return (
                    <div key={area}>
                      <div className="flex justify-between text-sm mb-1">
                        <span className="font-medium text-primary">{area}</span>
                        <span className="font-bold text-accent">{alloc || '—'}</span>
                      </div>
                      <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                        <div className="h-full bg-accent rounded-full transition-all duration-700"
                          style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                  );
                })}
              </div>
            </Card>

            {/* Already sufficient + over-practiced */}
            <div className="space-y-4">
              {companyAdvice.topicsAlreadySufficient?.length > 0 && (
                <Card title="Already Sufficient">
                  <div className="flex flex-wrap gap-2">
                    {companyAdvice.topicsAlreadySufficient.map(t => (
                      <span key={t} className="px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-100 text-sm font-semibold">
                        ✓ {t}
                      </span>
                    ))}
                  </div>
                </Card>
              )}
              {companyAdvice.topicsOverPracticed?.length > 0 && (
                <Card title="Over-practiced">
                  <p className="text-xs text-slate-500 mb-2">Reduce time here — redirect to gaps.</p>
                  <div className="flex flex-wrap gap-2">
                    {companyAdvice.topicsOverPracticed.map(t => (
                      <span key={t} className="px-3 py-1 rounded-full bg-blue-50 text-secondary-blue border border-blue-100 text-sm font-semibold">
                        {t}
                      </span>
                    ))}
                  </div>
                </Card>
              )}
            </div>
          </div>

          {companyAdvice.explanation && (
            <Card title="Analysis">
              <p className="text-sm text-slate-600 leading-relaxed">{companyAdvice.explanation}</p>
            </Card>
          )}
        </motion.div>
      )}
    </div>
  );
}

/* ─── Study Plan Tab ─────────────────────────────────────────── */
function StudyPlanTab({ planCompany, setPlanCompany, planDate, setPlanDate, planHours, setPlanHours, studyPlan, planLoading, generatePlan }) {
  const today = new Date().toISOString().split('T')[0];
  return (
    <div className="space-y-6">
      <Card title="Generate Study Plan">
        <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 mb-4">
          <div>
            <label className="block text-xs text-slate-500 font-medium mb-1">Target Company</label>
            <select value={planCompany} onChange={e => setPlanCompany(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm bg-white">
              {COMPANIES.map(c => <option key={c}>{c}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs text-slate-500 font-medium mb-1">Interview Date</label>
            <input type="date" min={today} value={planDate} onChange={e => setPlanDate(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm" />
          </div>
          <div>
            <label className="block text-xs text-slate-500 font-medium mb-1">Hours/Day</label>
            <select value={planHours} onChange={e => setPlanHours(Number(e.target.value))}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm bg-white">
              {[1,2,3,4,5,6].map(h => <option key={h} value={h}>{h}h</option>)}
            </select>
          </div>
          <div className="flex items-end">
            <button onClick={generatePlan} disabled={planLoading || !planDate}
              className="w-full px-4 py-2 rounded-xl bg-primary text-white font-semibold text-sm hover:bg-slate-800 transition disabled:opacity-60 flex items-center justify-center gap-2">
              {planLoading
                ? <><span className="w-3.5 h-3.5 border border-white border-t-transparent rounded-full animate-spin" />Generating…</>
                : 'Generate Plan'}
            </button>
          </div>
        </div>
      </Card>

      {studyPlan && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-4">
          <div className="grid grid-cols-3 gap-4">
            <Stat label="Days Remaining" value={studyPlan.daysRemaining} />
            <Stat label="Total Hours" value={`${studyPlan.totalHours}h`} />
            <Stat label="Total Weeks" value={studyPlan.totalWeeks} />
          </div>

          {studyPlan.explanation && (
            <Card title="Plan Rationale">
              <p className="text-sm text-slate-600 leading-relaxed">{studyPlan.explanation}</p>
            </Card>
          )}

          <div className="space-y-4">
            {studyPlan.weeks?.map(week => (
              <Card key={week.week} title={`Week ${week.week} — ${week.focusTopics?.join(', ')}`}
                badge={week.expectedReadinessGain ? `+${week.expectedReadinessGain} pts` : undefined}>
                {week.focusReason && (
                  <p className="text-xs text-slate-500 mb-3">{week.focusReason}</p>
                )}
                <div className="grid grid-cols-2 gap-3 mb-3">
                  <MiniStat label="Problems/Day" value={week.problemsPerDay} />
                  <MiniStat label="Est. Hours" value={`${week.estimatedHours}h`} />
                </div>
                {week.dailyTasks?.length > 0 && (
                  <div>
                    <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-2">Daily Tasks</p>
                    <ul className="space-y-1.5">
                      {week.dailyTasks.map((task, i) => (
                        <li key={i} className="flex items-start gap-2 text-sm text-slate-600">
                          <span className="text-accent font-bold mt-0.5">{i + 1}.</span>{task}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </Card>
            ))}
          </div>
        </motion.div>
      )}
    </div>
  );
}

/* ─── Primitives ─────────────────────────────────────────────── */
function Card({ title, badge, children, className = '' }) {
  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
      className={`bg-card rounded-[20px] p-6 border border-slate-100 shadow-sm ${className}`}>
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-base font-bold text-primary">{title}</h2>
        {badge && (
          <span className="text-[10px] px-2.5 py-1 rounded-full bg-accent/10 text-accent font-bold uppercase tracking-wide">
            {badge}
          </span>
        )}
      </div>
      {children}
    </motion.div>
  );
}

function Stat({ label, value }) {
  return (
    <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 text-center">
      <p className="text-xs text-slate-400 font-medium mb-1">{label}</p>
      <p className="text-lg font-extrabold text-primary">{value}</p>
    </div>
  );
}

function MiniStat({ label, value }) {
  return (
    <div className="p-2.5 rounded-lg bg-white border border-slate-100 text-center">
      <p className="text-[10px] text-slate-400 font-medium">{label}</p>
      <p className="text-sm font-bold text-primary">{value}</p>
    </div>
  );
}

function Badge({ label, value, color }) {
  const colors = {
    teal:  'bg-teal-50 text-teal-700 border-teal-100',
    amber: 'bg-amber-50 text-amber-700 border-amber-100',
    red:   'bg-red-50 text-red-700 border-red-100',
    blue:  'bg-blue-50 text-secondary-blue border-blue-100',
    slate: 'bg-slate-100 text-slate-600 border-slate-200',
  };
  return (
    <span className={`px-2.5 py-1 rounded-full border text-[10px] font-semibold ${colors[color] || colors.slate}`}>
      {label}: {value}
    </span>
  );
}
