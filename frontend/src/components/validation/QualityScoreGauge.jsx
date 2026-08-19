import './QualityViz.css';

function toneForScore(score) {
  if (score >= 95) return { color: 'var(--status-good)', label: 'Healthy' };
  if (score >= 80) return { color: 'var(--status-warning)', label: 'Needs attention' };
  return { color: 'var(--status-critical)', label: 'At risk' };
}

/**
 * Arc gauge for validation_runs.quality_score (accepted_records / total_records * 100,
 * see docs/state-machine.md § Quality score). Color is a threshold-driven status read,
 * not a decorative sequential ramp, so it shares the fixed status palette everywhere
 * else in the app uses for good/warning/critical.
 */
export function QualityScoreGauge({ score, acceptedRecords, totalRecords, size = 176 }) {
  const clamped = Math.max(0, Math.min(100, score ?? 0));
  const { color, label } = toneForScore(clamped);
  const stroke = 14;
  const radius = (size - stroke) / 2;
  const circumference = Math.PI * radius; // half circle
  const offset = circumference * (1 - clamped / 100);

  return (
    <div className="quality-gauge" style={{ width: size }}>
      <svg width={size} height={size / 2 + stroke} viewBox={`0 0 ${size} ${size / 2 + stroke}`}>
        <path
          d={`M ${stroke / 2} ${size / 2} A ${radius} ${radius} 0 0 1 ${size - stroke / 2} ${size / 2}`}
          fill="none"
          stroke="var(--gridline)"
          strokeWidth={stroke}
          strokeLinecap="round"
        />
        <path
          d={`M ${stroke / 2} ${size / 2} A ${radius} ${radius} 0 0 1 ${size - stroke / 2} ${size / 2}`}
          fill="none"
          stroke={color}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          style={{ transition: 'stroke-dashoffset 0.4s ease' }}
        />
      </svg>
      <div className="quality-gauge-value">
        <span className="quality-gauge-number">{Number.isFinite(score) ? clamped.toFixed(1) : '—'}</span>
        <span className="quality-gauge-unit">%</span>
      </div>
      <div className="quality-gauge-label" style={{ color }}>
        {label}
      </div>
      {(acceptedRecords !== undefined || totalRecords !== undefined) && (
        <div className="text-muted" style={{ fontSize: '0.78rem', textAlign: 'center' }}>
          {acceptedRecords ?? '—'} / {totalRecords ?? '—'} records accepted
        </div>
      )}
    </div>
  );
}
