export function UploadProgress({ fileName, percent, status = 'uploading' }) {
  const label = status === 'uploading' ? `Uploading ${fileName}…` : status === 'processing' ? 'Processing on server…' : 'Upload complete';
  return (
    <div className="upload-progress" role="status" aria-live="polite">
      <div className="row-between">
        <span className="text-secondary" style={{ fontSize: '0.85rem' }}>
          {label}
        </span>
        <span className="text-muted" style={{ fontSize: '0.8rem' }}>
          {status === 'processing' ? '' : `${percent}%`}
        </span>
      </div>
      <div className="progress-track">
        <div
          className={`progress-fill${status === 'processing' ? ' progress-fill-indeterminate' : ''}`}
          style={status === 'processing' ? undefined : { width: `${percent}%` }}
        />
      </div>
    </div>
  );
}
