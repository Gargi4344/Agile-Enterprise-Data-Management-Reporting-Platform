export function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="row" style={{ justifyContent: 'center', marginTop: 16 }} aria-label="Pagination">
      <button className="btn btn-sm" onClick={() => onChange(page - 1)} disabled={page <= 0}>
        Previous
      </button>
      <span className="text-muted" style={{ fontSize: '0.82rem' }}>
        Page {page + 1} of {totalPages}
      </span>
      <button className="btn btn-sm" onClick={() => onChange(page + 1)} disabled={page + 1 >= totalPages}>
        Next
      </button>
    </nav>
  );
}
