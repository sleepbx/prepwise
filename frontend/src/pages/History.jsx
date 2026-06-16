import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import api from '../api/axios';
import { formatPattern, formatEnum, formatDate, difficultyClass } from '../utils/format';

const PLATFORMS = ['LEETCODE', 'CODEFORCES', 'GFG', 'HACKERRANK', 'MISCELLANEOUS'];
const TOPICS = ['ARRAY', 'STRING', 'LINKED_LIST', 'STACK', 'QUEUE', 'TREE', 'GRAPH', 'HEAP', 'GREEDY', 'DP', 'BACKTRACKING', 'BINARY_SEARCH'];
const PATTERNS = ['HASHING', 'TWO_POINTER', 'SLIDING_WINDOW', 'DFS', 'BFS', 'KNAPSACK', 'BINARY_SEARCH_PATTERN', 'GREEDY_PATTERN', 'BACKTRACKING_PATTERN', 'TOP_K', 'UNION_FIND'];
const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];

export default function History() {
  const [attempts, setAttempts] = useState([]);
  const [search, setSearch] = useState('');
  const [platform, setPlatform] = useState('');
  const [topic, setTopic] = useState('');
  const [pattern, setPattern] = useState('');
  const [difficulty, setDifficulty] = useState('');
  const [loading, setLoading] = useState(true);

  const fetchAttempts = () => {
    setLoading(true);
    const params = {};
    if (search) params.search = search;
    if (platform) params.platform = platform;
    if (topic) params.topic = topic;
    if (pattern) params.pattern = pattern;
    if (difficulty) params.difficulty = difficulty;

    api.get('/problems', { params })
      .then((res) => setAttempts(res.data))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchAttempts(); }, [platform, topic, pattern, difficulty]);

  const handleSearch = (e) => {
    e.preventDefault();
    fetchAttempts();
  };

  const clearFilters = () => {
    setSearch('');
    setPlatform('');
    setTopic('');
    setPattern('');
    setDifficulty('');
    setLoading(true);
    api.get('/problems').then((res) => setAttempts(res.data)).finally(() => setLoading(false));
  };

  const hasFilters = search || platform || topic || pattern || difficulty;

  return (
    <div className="p-6 md:p-8 space-y-6 max-w-[1400px]">
      <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
        <div>
          <p className="text-accent text-sm font-semibold tracking-widest uppercase mb-1">Ledger</p>
          <h1 className="text-3xl font-extrabold text-primary tracking-tight">Attempt Ledger</h1>
          <p className="text-slate-500 mt-1">{attempts.length} attempts recorded</p>
        </div>
        {hasFilters && (
          <button onClick={clearFilters} className="text-sm text-accent hover:underline font-medium self-start">
            Clear all filters
          </button>
        )}
      </div>

      <div className="bg-card rounded-[20px] p-5 md:p-6 border border-slate-100 shadow-sm space-y-4">
        <form onSubmit={handleSearch} className="flex gap-3">
          <div className="relative flex-1">
            <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">⌕</span>
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by problem name or slug..."
              className="w-full pl-10 pr-4 py-3.5 rounded-xl border border-slate-200 bg-slate-50/50 focus:outline-none focus:ring-2 focus:ring-accent/30 focus:bg-white text-sm transition"
            />
          </div>
          <button type="submit" className="px-6 py-3.5 rounded-xl bg-accent text-white font-semibold hover:bg-teal-600 transition text-sm shadow-md shadow-accent/20">
            Search
          </button>
        </form>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <FilterSelect label="Platform" value={platform} onChange={setPlatform} options={PLATFORMS} />
          <FilterSelect label="Topic" value={topic} onChange={setTopic} options={TOPICS} />
          <FilterSelect label="Pattern" value={pattern} onChange={setPattern} options={PATTERNS} />
          <FilterSelect label="Difficulty" value={difficulty} onChange={setDifficulty} options={DIFFICULTIES} />
        </div>
      </div>

      <div className="bg-card rounded-[20px] border border-slate-100 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-16 text-center">
            <div className="w-8 h-8 border-2 border-accent border-t-transparent rounded-full animate-spin mx-auto mb-3" />
            <p className="text-slate-400 text-sm">Loading attempts...</p>
          </div>
        ) : attempts.length === 0 ? (
          <div className="p-16 text-center">
            <p className="text-4xl mb-3">📋</p>
            <p className="text-primary font-semibold">No attempts found</p>
            <p className="text-slate-500 text-sm mt-1">Log a problem from the Dashboard to get started</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-400 bg-slate-50/80 border-b border-slate-100">
                  <th className="px-6 py-4 font-semibold">Date</th>
                  <th className="px-6 py-4 font-semibold">Problem</th>
                  <th className="px-6 py-4 font-semibold">Platform</th>
                  <th className="px-6 py-4 font-semibold">Topic</th>
                  <th className="px-6 py-4 font-semibold">Pattern</th>
                  <th className="px-6 py-4 font-semibold">Difficulty</th>
                  <th className="px-6 py-4 font-semibold">Time</th>
                  <th className="px-6 py-4 font-semibold">Hint</th>
                </tr>
              </thead>
              <tbody>
                {attempts.map((a, i) => (
                  <motion.tr
                    key={a.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: Math.min(i * 0.02, 0.3) }}
                    className="border-b border-slate-50 hover:bg-slate-50/60 transition group"
                  >
                    <td className="px-6 py-4 text-slate-500 whitespace-nowrap text-xs">{formatDate(a.solvedAt)}</td>
                    <td className="px-6 py-4">
                      <a href={a.problemUrl} target="_blank" rel="noreferrer"
                        className="font-semibold text-primary group-hover:text-accent transition line-clamp-1">
                        {a.title}
                      </a>
                      <p className="text-xs text-slate-400 mt-0.5">{a.slug}</p>
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-xs px-2.5 py-1 rounded-full bg-blue-50 text-secondary-blue font-semibold border border-blue-100">
                        {formatEnum(a.platform)}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-xs px-2.5 py-1 rounded-full bg-slate-100 text-slate-600 font-medium">
                        {formatEnum(a.topic)}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-xs px-2.5 py-1 rounded-full bg-teal-50 text-accent font-medium border border-teal-100">
                        {formatPattern(a.pattern)}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`text-xs px-2.5 py-1 rounded-full font-semibold ${difficultyClass(a.difficulty)}`}>
                        {a.difficulty}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-slate-600 font-medium">{a.timeSpentMinutes}m</td>
                    <td className="px-6 py-4">
                      {a.hintUsed ? (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-amber-50 text-amber-600 font-medium border border-amber-100">Yes</span>
                      ) : (
                        <span className="text-slate-300">—</span>
                      )}
                    </td>
                  </motion.tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function FilterSelect({ label, value, onChange, options }) {
  return (
    <div>
      <label className="text-xs font-semibold text-slate-500 mb-1.5 block uppercase tracking-wide">{label}</label>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full px-3 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-accent/30 bg-white font-medium text-slate-700"
      >
        <option value="">All</option>
        {options.map((o) => (
          <option key={o} value={o}>{o.replace(/_/g, ' ')}</option>
        ))}
      </select>
    </div>
  );
}
