export function KpiTile({ label, value, unit, tone }) {
  return (
    <div className="kpi-tile">
      <div className="text-muted" style={{ fontSize: '0.76rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
        {label}
      </div>
      <div className="kpi-tile-value" style={tone ? { color: `var(--${tone})` } : undefined}>
        {value}
        {unit && <span className="kpi-tile-unit">{unit}</span>}
      </div>
    </div>
  );
}
