export const formatPattern = (pattern) => {
  if (!pattern) return '-';
  return pattern.replace(/_PATTERN/g, '').split('_').map(
    (word) => word.charAt(0) + word.slice(1).toLowerCase()
  ).join(' ');
};

export const formatEnum = (value) => {
  if (!value) return '-';
  return String(value).split('_').map((word) => word.charAt(0) + word.slice(1).toLowerCase()).join(' ');
};

export const formatDate = (iso) => {
  if (!iso) return '-';
  return new Date(iso).toLocaleDateString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
  });
};

export const formatTime = (minutes) => {
  if (!minutes) return '0m';
  if (minutes < 60) return `${minutes}m`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h}h ${m}m` : `${h}h`;
};

export const difficultyClass = (difficulty) => {
  const key = (difficulty || 'UNKNOWN').toLowerCase();
  return `difficulty-${key}`;
};
