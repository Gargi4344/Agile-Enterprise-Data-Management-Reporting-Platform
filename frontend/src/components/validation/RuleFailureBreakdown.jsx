import './QualityViz.css';

const SEVERITY_COLOR = {
  BLOCKER: 'var(--status-critical)',
  WARNING: 'var(--status-warning)',
};

/**
 * Horizontal bar breakdown of failure counts by rule. Color encodes severity (a fixed
 * status read, BLOCKER vs WARNING) rather than rule identity, since severity is the
 * dimension that matters for triage; rule name + count are direct labels so the color
 * never carries meaning alone.
 */
export function RuleFailureBreakdown({ items }) {
  if (!items || items.length === 0) return null;
  const max = Math.max(...items.map((item) => item.failureCount), 1);

  return (
    <div className="rule-breakdown">
      <div className="rule-breakdown-legend">
        <span className="row" style={{ fontSize: '0.78rem' }}>
          <span className="badge-dot" style={{ background: 'var(--status-critical)' }} /> Blocker
        </span>
        <span className="row" style={{ fontSize: '0.78rem' }}>
          <span className="badge-dot" style={{ background: 'var(--status-warning)' }} /> Warning
        </span>
      </div>
      <ul className="rule-breakdown-list">
        {items.map((item) => (
          <li key={item.ruleId ?? item.ruleName}>
            <div className="row-between" style={{ marginBottom: 3 }}>
              <span className="rule-breakdown-name">{item.ruleName}</span>
              <span className="text-muted" style={{ fontSize: '0.78rem' }}>
                {item.failureCount}
              </span>
            </div>
            <div className="rule-breakdown-track">
              <div
                className="rule-breakdown-fill"
                style={{
                  width: `${(item.failureCount / max) * 100}%`,
                  background: SEVERITY_COLOR[item.severity] || 'var(--series-1)',
                }}
                role="img"
                aria-label={`${item.ruleName}: ${item.failureCount} ${item.severity?.toLowerCase()} failures`}
              />
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
