export function LoadingState({ label = 'Loading…', rows = 3 }) {
  return (
    <div className="state-block" role="status" aria-live="polite">
      <div className="stack">
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="skeleton" style={{ height: 18, width: `${92 - i * 12}%` }} />
        ))}
      </div>
      <span className="visually-hidden">{label}</span>
    </div>
  );
}

export function LoadingInline({ label = 'Loading…' }) {
  return (
    <span className="row text-muted" role="status">
      <span className="spinner" aria-hidden="true" />
      {label}
    </span>
  );
}
