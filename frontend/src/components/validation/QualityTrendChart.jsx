import { useMemo, useState } from 'react';
import { formatDateTime } from '../../utils/format.js';
import './QualityViz.css';

const WIDTH = 560;
const HEIGHT = 180;
const PAD = { top: 16, right: 16, bottom: 26, left: 36 };

/**
 * Quality-score trend across a version's run history — a single series, so no legend
 * box is needed (the card title names it); a hover crosshair + tooltip surfaces the
 * exact score and timestamp per run, per the interaction spec for line charts.
 */
export function QualityTrendChart({ runs }) {
  const [hoverIndex, setHoverIndex] = useState(null);
  const points = useMemo(() => {
    const chronological = [...runs].reverse();
    const innerW = WIDTH - PAD.left - PAD.right;
    const innerH = HEIGHT - PAD.top - PAD.bottom;
    return chronological.map((run, i) => {
      const x = chronological.length === 1 ? PAD.left + innerW / 2 : PAD.left + (i / (chronological.length - 1)) * innerW;
      const score = run.qualityScore ?? 0;
      const y = PAD.top + innerH * (1 - score / 100);
      return { ...run, x, y };
    });
  }, [runs]);

  if (points.length === 0) return null;

  const path = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');
  const gridLines = [0, 25, 50, 75, 100];
  const active = hoverIndex !== null ? points[hoverIndex] : null;

  return (
    <div className="quality-trend">
      <svg width="100%" viewBox={`0 0 ${WIDTH} ${HEIGHT}`} role="img" aria-label="Quality score trend across validation runs">
        {gridLines.map((g) => {
          const y = PAD.top + (HEIGHT - PAD.top - PAD.bottom) * (1 - g / 100);
          return (
            <g key={g}>
              <line x1={PAD.left} x2={WIDTH - PAD.right} y1={y} y2={y} stroke="var(--gridline)" strokeWidth="1" />
              <text x={PAD.left - 8} y={y + 3} textAnchor="end" fontSize="9" fill="var(--text-muted)">
                {g}
              </text>
            </g>
          );
        })}
        <path d={path} fill="none" stroke="var(--series-1)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        {points.map((p, i) => (
          <circle
            key={p.id ?? i}
            cx={p.x}
            cy={p.y}
            r={hoverIndex === i ? 5 : 3.5}
            fill="var(--surface-1)"
            stroke="var(--series-1)"
            strokeWidth="2"
            onMouseEnter={() => setHoverIndex(i)}
            onMouseLeave={() => setHoverIndex(null)}
            style={{ cursor: 'pointer' }}
          />
        ))}
        {active && (
          <line x1={active.x} x2={active.x} y1={PAD.top} y2={HEIGHT - PAD.bottom} stroke="var(--border-strong)" strokeDasharray="3 3" />
        )}
      </svg>
      <div className="quality-trend-tooltip" aria-live="polite">
        {active ? (
          <>
            <strong>{active.qualityScore?.toFixed(1)}%</strong> quality · run #{active.id} · {formatDateTime(active.createdAt)}
          </>
        ) : (
          <span className="text-muted">Hover a point to inspect a run</span>
        )}
      </div>
    </div>
  );
}
