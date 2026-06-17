import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import api from '../api/axios';
import { difficultyClass, formatDate, formatEnum, formatPattern } from '../utils/format';

const COMPANIES = ['Google', 'Amazon', 'Microsoft', 'Adobe'];
const QUEUE_ITEMS = [
  ['Warm-Up', 'Easy confidence builder', 'warmUp'],
  ['Core Challenge', 'Medium interview signal', 'coreChallenge'],
  ['Stretch Challenge', 'Medium or hard progression', 'stretchChallenge'],
];

export default function Practice() {
  const [qod, setQod] = useState(null);
  const [queue, setQueue] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [patterns, setPatterns] = useState([]);
  const [studyPlan, setStudyPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [planLoading, setPlanLoading] = useState(false);
  const [savingUrl, setSavingUrl] = useState('');
  const [notice, setNotice] = useState('');
  const [loadError, setLoadError] = useState('');
  const [planForm, setPlanForm] = useState({
    targetCompany: 'Google',
    interviewDate: '2026-09-01',
    hoursPerDay: 3,
    experienceLevel: 'INTERMEDIATE',
  });

  const loadPractice = async () => {
    const results = await Promise.allSettled([
      api.get('/practice/question-of-day'),
      api.get('/practice/today'),
      api.get('/practice/review-queue'),
      api.get('/practice/recommended-patterns'),
    ]);

    if (results[0].status === 'fulfilled') setQod(results[0].value.data);
    if (results[1].status === 'fulfilled') setQueue(results[1].value.data);
    if (results[2].status === 'fulfilled') setReviews(results[2].value.data);
    if (results[3].status === 'fulfilled') setPatterns(results[3].value.data);
    if (results.some((result) => result.status === 'rejected')) {
      setLoadError('Some practice data could not load. Showing everything available.');
    } else {
      setLoadError('');
    }
    setLoading(false);
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      loadPractice();
    }, 0);
    return () => clearTimeout(timer);
  }, []);

  const markSolved = async (problem) => {
    if (!problem?.url) return;
    setSavingUrl(problem.url);
    setNotice('');
    try {
      await api.post('/problems', {
        problemUrl: problem.url,
        timeSpentMinutes: problem.estimatedSolveTimeMinutes || 30,
        hintUsed: false,
      });
      setNotice(`${problem.title} was logged as solved.`);
      await loadPractice();
    } catch (err) {
      setNotice(err.response?.data?.message || 'Could not mark this problem solved.');
    } finally {
      setSavingUrl('');
    }
  };

  const markReviewed = async (attemptId) => {
    await api.post(`/practice/reviewed/${attemptId}`);
    await loadPractice();
  };

  const generatePlan = async () => {
    setPlanLoading(true);
    try {
      const { data } = await api.post('/planner/generate', planForm);
      setStudyPlan(data);
    } finally {
      setPlanLoading(false);
    }
  };

  if (loading) {
    return <div className="flex items-center justify-center h-screen text-slate-400">Loading Practice Lab...</div>;
  }

  return (
    <div className="p-6 md:p-8 space-y-8 max-w-[1400px]">
      <motion.section
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="gradient-hero rounded-[20px] p-8 md:p-10 text-white relative overflow-hidden"
      >
        <div className="relative z-10">
          <p className="text-accent/90 text-sm font-semibold tracking-widest uppercase mb-3">Practice Lab</p>
          <h1 className="text-3xl md:text-[2.6rem] font-extrabold leading-tight mb-2">Know what to solve next.</h1>
          <p className="text-slate-300 text-lg max-w-2xl">
            A daily queue based on weak patterns, neglected topics, recent activity, and spaced revision.
          </p>
        </div>
      </motion.section>

      {(loadError || notice) && (
        <div className={`p-4 rounded-xl border text-sm ${notice.includes('Could not') ? 'bg-red-50 border-red-100 text-red-600' : 'bg-teal-50 border-teal-100 text-accent'}`}>
          {notice || loadError}
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <Card title="Question of the Day" eyebrow="Daily solve">
          <ProblemBlock problem={qod} onMarkSolved={markSolved} savingUrl={savingUrl} />
        </Card>

        <Card title="Today's Practice Queue" eyebrow="Generated from your signals" className="xl:col-span-2">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {QUEUE_ITEMS.map(([label, subtitle, key]) => (
              <QueuePanel
                key={key}
                label={label}
                subtitle={subtitle}
                problem={queue?.[key]}
                onMarkSolved={markSolved}
                savingUrl={savingUrl}
              />
            ))}
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
        <Card title="Review Queue" eyebrow="1d, 7d, 14d, 30d intervals" className="xl:col-span-3">
          <div className="space-y-3">
            {reviews.length === 0 && <p className="text-sm text-slate-500">No reviews due today. Nice recovery discipline.</p>}
            {reviews.map((item) => (
              <div key={item.attemptId} className="flex flex-col md:flex-row md:items-center gap-3 justify-between p-4 rounded-2xl bg-slate-50 border border-slate-100">
                <div>
                  <p className="font-bold text-primary">{item.problem}</p>
                  <p className="text-xs text-slate-500 mt-1">
                    {formatEnum(item.platform)} - solved {formatDate(item.solvedDate)} - due {formatDate(item.reviewDue)}
                  </p>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className={`text-xs px-2.5 py-1 rounded-full font-bold ${priorityClass(item.priority)}`}>{item.priority}</span>
                  <a href={item.url} target="_blank" rel="noreferrer" className="px-3 py-2 rounded-xl bg-primary text-white text-sm font-semibold hover:bg-slate-800 transition">Review</a>
                  <button onClick={() => markReviewed(item.attemptId)} className="px-3 py-2 rounded-xl bg-accent text-white text-sm font-semibold hover:bg-teal-600 transition">Mark Reviewed</button>
                </div>
              </div>
            ))}
          </div>
        </Card>

        <Card title="Recommended Patterns" eyebrow="Confidence and recency" className="xl:col-span-2">
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-1 gap-3">
            {patterns.map((pattern) => (
              <div key={pattern.pattern} className="p-4 rounded-2xl border border-slate-100 bg-gradient-to-br from-slate-50 to-white">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-bold text-primary">{pattern.label}</p>
                    <p className="text-xs text-slate-500 mt-1">{pattern.reason}</p>
                  </div>
                  <span className="text-lg font-extrabold text-accent">{pattern.confidenceScore}%</span>
                </div>
                <div className="mt-4 h-2 rounded-full bg-slate-200 overflow-hidden">
                  <div className="h-full rounded-full bg-accent" style={{ width: `${pattern.confidenceScore}%` }} />
                </div>
                <p className="text-xs text-slate-500 mt-3">
                  Last practiced {pattern.lastPracticed ? formatDate(pattern.lastPracticed) : 'never'} - {pattern.problemsSolved} solved
                </p>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <Card title="Interview Planner" eyebrow="Week-by-week roadmap">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-3 mb-4">
          <Field label="Company">
            <select value={planForm.targetCompany} onChange={(e) => setPlanForm({ ...planForm, targetCompany: e.target.value })}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm">
              {COMPANIES.map((c) => <option key={c}>{c}</option>)}
            </select>
          </Field>
          <Field label="Interview Date">
            <input type="date" value={planForm.interviewDate}
              onChange={(e) => setPlanForm({ ...planForm, interviewDate: e.target.value })}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm" />
          </Field>
          <Field label="Hours Per Day">
            <input type="number" min={1} max={12} value={planForm.hoursPerDay}
              onChange={(e) => setPlanForm({ ...planForm, hoursPerDay: Number(e.target.value) })}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm" />
          </Field>
          <Field label="Experience Level">
            <select value={planForm.experienceLevel} onChange={(e) => setPlanForm({ ...planForm, experienceLevel: e.target.value })}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 text-sm">
              <option>BEGINNER</option>
              <option>INTERMEDIATE</option>
              <option>ADVANCED</option>
            </select>
          </Field>
        </div>
        <button onClick={generatePlan} disabled={planLoading}
          className="w-full lg:w-auto px-6 py-3 rounded-xl bg-accent text-white font-semibold hover:bg-teal-600 transition disabled:opacity-60">
          {planLoading ? 'Generating...' : 'Generate Roadmap'}
        </button>
        {studyPlan && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mt-5 space-y-3">
            <p className="text-sm text-slate-500">{studyPlan.daysRemaining} days - {studyPlan.totalHours}h total - {studyPlan.totalWeeks} weeks</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {studyPlan.weeks?.map((week) => (
                <div key={week.week} className="p-4 rounded-2xl bg-slate-50 border border-slate-100 text-sm">
                  <span className="font-bold text-primary">Week {week.week}</span>
                  <span className="text-slate-500"> - {week.focusTopics?.join(', ')} - {week.problemsPerDay}/day</span>
                </div>
              ))}
            </div>
            <p className="text-sm text-slate-600 leading-relaxed border-t border-slate-100 pt-4">{studyPlan.explanation}</p>
          </motion.div>
        )}
      </Card>
    </div>
  );
}

function ProblemBlock({ problem, onMarkSolved, savingUrl }) {
  if (!problem) return <p className="text-sm text-slate-500">No recommendation available yet.</p>;
  const isSaving = savingUrl === problem.url;

  return (
    <div>
      <p className="text-2xl font-extrabold text-primary tracking-tight">{problem.title}</p>
      <div className="flex flex-wrap gap-2 mt-4">
        <span className="text-xs px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">{formatEnum(problem.platform)}</span>
        <span className={`text-xs px-2.5 py-1 rounded-full ${difficultyClass(problem.difficulty)}`}>{problem.difficulty}</span>
        <span className="text-xs px-2.5 py-1 rounded-full bg-blue-50 text-secondary-blue">{formatEnum(problem.topic)}</span>
        <span className="text-xs px-2.5 py-1 rounded-full bg-teal-50 text-accent">{formatPattern(problem.pattern)}</span>
      </div>
      <p className="text-sm text-slate-500 mt-4">{problem.reason}</p>
      <p className="text-sm font-semibold text-primary mt-3">{problem.estimatedSolveTimeMinutes} min estimated</p>
      <div className="flex flex-wrap gap-2 mt-5">
        <a href={problem.url} target="_blank" rel="noreferrer" className="px-4 py-2.5 rounded-xl bg-primary text-white font-semibold text-sm hover:bg-slate-800 transition">Attempt</a>
        <button
          type="button"
          disabled={isSaving}
          onClick={() => onMarkSolved(problem)}
          className="px-4 py-2.5 rounded-xl bg-accent text-white font-semibold text-sm hover:bg-teal-600 transition disabled:opacity-60"
        >
          {isSaving ? 'Saving...' : 'Mark Solved'}
        </button>
      </div>
    </div>
  );
}

function QueuePanel({ label, subtitle, problem, onMarkSolved, savingUrl }) {
  return (
    <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100">
      <p className="text-xs uppercase tracking-wider text-slate-500 font-bold">{label}</p>
      <p className="text-xs text-slate-400 mb-3">{subtitle}</p>
      <ProblemBlock problem={problem} onMarkSolved={onMarkSolved} savingUrl={savingUrl} />
    </div>
  );
}

function Card({ title, eyebrow, children, className = '' }) {
  return (
    <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
      className={`bg-card rounded-[20px] p-6 border border-slate-100 shadow-sm ${className}`}>
      <div className="mb-4">
        {eyebrow && <p className="text-xs font-bold uppercase tracking-wider text-accent mb-1">{eyebrow}</p>}
        <h2 className="text-lg font-bold text-primary">{title}</h2>
      </div>
      {children}
    </motion.div>
  );
}

function Field({ label, children }) {
  return (
    <div>
      <label className="text-xs font-semibold text-slate-500 mb-1 block uppercase tracking-wide">{label}</label>
      {children}
    </div>
  );
}

function priorityClass(priority) {
  if (priority === 'HIGH') return 'bg-red-50 text-red-600';
  if (priority === 'MEDIUM') return 'bg-amber-50 text-amber-600';
  return 'bg-slate-200 text-slate-600';
}
