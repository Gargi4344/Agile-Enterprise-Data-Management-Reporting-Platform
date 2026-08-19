export function EmptyState({ title = 'Nothing here yet', description, action, icon = '📭' }) {
  return (
    <div className="state-block state-block-center">
      <div className="state-icon" aria-hidden="true">
        {icon}
      </div>
      <h3>{title}</h3>
      {description && <p className="text-secondary">{description}</p>}
      {action}
    </div>
  );
}

export function NoResultsState({ query, onClear }) {
  return (
    <div className="state-block state-block-center">
      <div className="state-icon" aria-hidden="true">
        🔍
      </div>
      <h3>No results{query ? ` for "${query}"` : ''}</h3>
      <p className="text-secondary">Try a different search term or clear your filters.</p>
      {onClear && (
        <button type="button" className="btn btn-sm" onClick={onClear}>
          Clear filters
        </button>
      )}
    </div>
  );
}
