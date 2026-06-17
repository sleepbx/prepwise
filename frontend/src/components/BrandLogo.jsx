export default function BrandLogo({ compact = false }) {
  const squares = [
    [0, 0, 'bg-accent'],
    [1, 0, 'bg-teal-300'],
    [2, 0, 'bg-accent'],
    [0, 1, 'bg-accent'],
    [3, 1, 'bg-teal-400'],
    [0, 2, 'bg-accent'],
    [1, 2, 'bg-teal-300'],
    [2, 2, 'bg-accent'],
    [0, 3, 'bg-accent'],
    [0, 4, 'bg-accent'],
  ];

  return (
    <div className={`flex items-center ${compact ? 'gap-2' : 'gap-3'}`}>
      <div className="grid grid-cols-4 grid-rows-5 gap-0.5 p-2 rounded-xl bg-white/5 border border-accent/30 shadow-lg shadow-accent/10">
        {Array.from({ length: 20 }).map((_, index) => {
          const x = index % 4;
          const y = Math.floor(index / 4);
          const active = squares.find(([sx, sy]) => sx === x && sy === y);
          return (
            <span
              key={`${x}-${y}`}
              className={`h-1.5 w-1.5 rounded-[2px] ${active ? active[2] : 'bg-slate-700/70'}`}
            />
          );
        })}
      </div>
      {!compact && (
        <div>
          <h1 className="text-lg font-bold tracking-tight">PrepWise</h1>
          <p className="text-xs text-slate-400">Build Consistency. Crack Interviews.</p>
        </div>
      )}
    </div>
  );
}
