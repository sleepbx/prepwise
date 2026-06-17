import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import api from '../api/axios';
import CountUp from '../components/CountUp';
import { formatTime } from '../utils/format';

const trackedSheets = ['Striver A2Z', 'CodeHelp DSA', 'NeetCode 150', 'Blind 75'];

export default function Profile() {
  const [profile, setProfile] = useState(null);
  const [achievements, setAchievements] = useState([]);
  const [sheets, setSheets] = useState([]);

  useEffect(() => {
    api.get('/profile').then((res) => setProfile(res.data));
    api.get('/achievements').then((res) => setAchievements(res.data)).catch(() => {});
    api.get('/analytics/dashboard').then((res) => setSheets(res.data.sheetProgress || [])).catch(() => {});
  }, []);

  if (!profile) {
    return <div className="flex items-center justify-center h-screen text-slate-400">Loading profile...</div>;
  }

  const weeks = groupHeatmapByWeek(profile.heatmap);

  return (
    <div className="p-6 md:p-8 space-y-8 max-w-[1400px]">
      <motion.section
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="gradient-hero rounded-[20px] p-8 text-white relative overflow-hidden"
      >
        <div className="relative z-10 flex items-center gap-6">
          <div className="w-20 h-20 rounded-2xl bg-accent/20 border-2 border-accent/40 flex items-center justify-center text-3xl font-bold text-accent">
            {profile.initials}
          </div>
          <div>
            <h1 className="text-3xl font-bold">{profile.name}</h1>
            <p className="text-slate-300 mt-1">{profile.email}</p>
          </div>
        </div>
      </motion.section>

      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4">
        {[
          { label: 'Current Streak', value: profile.currentStreak, suffix: ' days' },
          { label: 'Longest Streak', value: profile.longestStreak, suffix: ' days' },
          { label: 'Total Solved', value: profile.totalSolved },
          { label: 'Total Time', value: profile.totalTimeMinutes, format: 'time' },
          { label: 'Favorite Platform', value: profile.favoritePlatform, text: true },
          { label: 'Strongest Pattern', value: profile.strongestPattern, text: true },
          { label: 'Weakest Pattern', value: profile.weakestPattern, text: true },
        ].map((stat, i) => (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.05 }}
            className="bg-card rounded-[20px] p-5 border border-slate-100"
          >
            <p className="text-xs text-slate-500 mb-2">{stat.label}</p>
            {stat.text ? (
              <p className="text-sm font-bold text-primary">{stat.value}</p>
            ) : stat.format === 'time' ? (
              <p className="text-xl font-bold text-primary">{formatTime(stat.value)}</p>
            ) : (
              <p className="text-xl font-bold text-primary">
                <CountUp end={stat.value} suffix={stat.suffix || ''} />
              </p>
            )}
          </motion.div>
        ))}
      </div>

      <section className="bg-card rounded-[20px] p-6 border border-slate-100">
        <h2 className="text-lg font-bold text-primary mb-2">Consistency Heatmap</h2>
        <p className="text-sm text-slate-500 mb-6">Last 6 weeks of preparation activity</p>

        <div className="overflow-x-auto">
          <div className="inline-flex gap-1">
            {weeks.map((week, wi) => (
              <div key={wi} className="flex flex-col gap-1">
                {week.map((day) => (
                  <HeatmapCell key={day.date} day={day} />
                ))}
              </div>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-2 mt-4 text-xs text-slate-400">
          <span>Less</span>
          {[0, 1, 2, 3, 4].map((level) => (
            <div key={level} className={`w-3 h-3 rounded-sm heatmap-${level}`} />
          ))}
          <span>More</span>
        </div>
      </section>

      <section className="bg-card rounded-[20px] p-6 border border-slate-100">
        <h2 className="text-lg font-bold text-primary mb-2">Achievements</h2>
        <p className="text-sm text-slate-500 mb-6">Milestones for consistency, depth, sheet completion, and clean solving habits.</p>
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          {achievements.map((achievement) => (
            <motion.div
              key={achievement.key}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              className={`p-5 rounded-2xl border ${achievement.unlocked ? 'bg-teal-50 border-teal-100' : 'bg-slate-50 border-slate-100'}`}
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-bold text-primary">{achievement.name}</p>
                  <p className="text-xs text-slate-500 mt-1">{achievement.description}</p>
                </div>
                <span className={`text-xs px-2 py-1 rounded-full font-bold ${achievement.unlocked ? 'bg-accent text-white' : 'bg-slate-200 text-slate-500'}`}>
                  {achievement.unlocked ? 'Done' : `${achievement.progress}/${achievement.target}`}
                </span>
              </div>
              <div className="mt-4 h-2 rounded-full bg-white/80 overflow-hidden">
                <div className="h-full rounded-full bg-accent" style={{ width: `${progressWidth(achievement)}%` }} />
              </div>
            </motion.div>
          ))}
        </div>
      </section>

      <section className="bg-card rounded-[20px] p-6 border border-slate-100">
        <h2 className="text-lg font-bold text-primary mb-2">Completed Sheets</h2>
        <p className="text-sm text-slate-500 mb-6">Progress across the interview sheets PrepWise tracks.</p>
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
          {trackedSheets.map((name) => {
            const sheet = sheets.find((item) => item.name === name) || { solved: 0, total: 1, percentage: 0, patternsCovered: 0 };
            return (
              <div key={name} className="p-5 rounded-2xl bg-gradient-to-br from-slate-50 to-white border border-slate-100">
                <div className="flex justify-between items-start gap-3">
                  <p className="font-bold text-primary">{name}</p>
                  <span className="text-lg font-extrabold text-accent">{sheet.percentage}%</span>
                </div>
                <p className="text-sm text-slate-500 mt-2">{sheet.solved}/{sheet.total} solved - {sheet.patternsCovered} patterns</p>
                <div className="mt-4 h-2.5 bg-slate-200 rounded-full overflow-hidden">
                  <div className="h-full bg-accent rounded-full" style={{ width: `${Math.max(2, sheet.percentage)}%` }} />
                </div>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}

function HeatmapCell({ day }) {
  return (
    <div
      className={`w-4 h-4 rounded-sm heatmap-${day.level} cursor-pointer transition hover:ring-2 hover:ring-accent/50`}
      title={`${day.date}\n${day.problemsSolved} problems - ${day.minutesInvested} min`}
    />
  );
}

function progressWidth(achievement) {
  if (!achievement.target) return 0;
  return Math.min(100, Math.max(2, (achievement.progress / achievement.target) * 100));
}

function groupHeatmapByWeek(heatmap) {
  if (!heatmap?.length) return [];
  const weeks = [];
  let currentWeek = [];

  heatmap.forEach((day, index) => {
    const date = new Date(day.date);
    const dayOfWeek = date.getDay();
    if (dayOfWeek === 1 && currentWeek.length > 0) {
      weeks.push(currentWeek);
      currentWeek = [];
    }
    currentWeek.push(day);
    if (index === heatmap.length - 1) weeks.push(currentWeek);
  });

  return weeks;
}
