export function PermissionDenied({ message = "You don't have permission to view this page." }) {
  return (
    <div className="state-block state-block-center" role="alert">
      <div className="state-icon" aria-hidden="true">
        🔒
      </div>
      <h3>Permission denied</h3>
      <p className="text-secondary">{message}</p>
    </div>
  );
}
