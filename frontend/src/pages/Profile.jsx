import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import api from '../api/axios';
import CountUp from '../components/CountUp';
import { formatTime } from '../utils/format';

export default function Profile() {
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    api.get('/profile').then((res) => setProfile(res.data));
  }, []);

  if (!profile) {
    return <div className="flex items-center justify-center h-screen text-slate-400">Loading profile...</div>;
  }

  const weeks = groupHeatmapByWeek(profile.heatmap);

  return (
    <div className="p-8 space-y-8">
      <motion.section
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="gradient-hero rounded-[20px] p-8 text-white relative overflow-hidden"
      >
        <div className="absolute top-0 right-0 w-64 h-64 bg-accent/10 rounded-full blur-3xl" />
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

      <div className="bg-card rounded-[20px] p-6 border border-slate-100">
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
          {[0, 1, 2, 3, 4].map((l) => (
            <div key={l} className={`w-3 h-3 rounded-sm heatmap-${l}`} />
          ))}
          <span>More</span>
        </div>
      </div>
    </div>
  );
}

function HeatmapCell({ day }) {
  return (
    <div
      className={`w-4 h-4 rounded-sm heatmap-${day.level} cursor-pointer transition hover:ring-2 hover:ring-accent/50`}
      title={`${day.date}\n${day.problemsSolved} problems · ${day.minutesInvested} min`}
    />
  );
}

function groupHeatmapByWeek(heatmap) {
  if (!heatmap?.length) return [];
  const weeks = [];
  let currentWeek = [];

  heatmap.forEach((day, i) => {
    const date = new Date(day.date);
    const dayOfWeek = date.getDay();
    if (dayOfWeek === 1 && currentWeek.length > 0) {
      weeks.push(currentWeek);
      currentWeek = [];
    }
    currentWeek.push(day);
    if (i === heatmap.length - 1) weeks.push(currentWeek);
  });

  return weeks;
}
