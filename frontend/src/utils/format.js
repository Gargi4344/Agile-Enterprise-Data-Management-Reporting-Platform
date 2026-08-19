export function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatRelative(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  const diffMs = Date.now() - date.getTime();
  const diffSec = Math.round(diffMs / 1000);
  const abs = Math.abs(diffSec);
  if (abs < 60) return 'just now';
  if (abs < 3600) return `${Math.round(diffSec / 60)}m ago`;
  if (abs < 86400) return `${Math.round(diffSec / 3600)}h ago`;
  return `${Math.round(diffSec / 86400)}d ago`;
}

export function formatBytes(bytes) {
  if (bytes === undefined || bytes === null) return '—';
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const exp = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / 1024 ** exp).toFixed(exp === 0 ? 0 : 1)} ${units[exp]}`;
}

export function formatPercent(value, digits = 1) {
  if (value === undefined || value === null || Number.isNaN(value)) return '—';
  return `${Number(value).toFixed(digits)}%`;
}
