const STATUS_TONE = {
  UPLOADED: 'info',
  VALIDATING: 'info',
  READY: 'good',
  REJECTED: 'critical',
  PROCESSING: 'info',
  COMPLETED: 'good',
  FAILED: 'critical',
  RETRY: 'warning',
  REVIEW: 'warning',
  PENDING: 'neutral',
  RUNNING: 'info',
  RETRYING: 'warning',
  ACTIVE: 'good',
  INACTIVE: 'neutral',
  PASSED: 'good',
  BLOCKER: 'critical',
  WARNING: 'warning',
};

export function StatusBadge({ status, label }) {
  const tone = STATUS_TONE[status] || 'neutral';
  return (
    <span className={`badge badge-${tone}`}>
      <span className="badge-dot" aria-hidden="true" />
      {label || status}
    </span>
  );
}
