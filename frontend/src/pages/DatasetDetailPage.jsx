import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAsync } from '../hooks/useAsync.js';
import { datasetsApi, uploadVersionWithProgress } from '../api/datasets.js';
import { LoadingState } from '../components/states/LoadingState.jsx';
import { ErrorState, ValidationErrorBanner } from '../components/states/ErrorState.jsx';
import { PermissionDenied } from '../components/states/PermissionDenied.jsx';
import { EmptyState } from '../components/states/EmptyState.jsx';
import { UploadProgress } from '../components/states/UploadProgress.jsx';
import { Modal } from '../components/common/Modal.jsx';
import { RoleGate } from '../components/common/RoleGate.jsx';
import { StatusBadge } from '../components/common/StatusBadge.jsx';
import { LifecycleStepper } from '../components/datasets/LifecycleStepper.jsx';
import { formatDateTime } from '../utils/format.js';
import { ApiError } from '../api/client.js';

export function DatasetDetailPage() {
  const { id } = useParams();
  const [uploadOpen, setUploadOpen] = useState(false);

  const datasetQuery = useAsync(() => datasetsApi.get(id), [id]);
  const versionsQuery = useAsync(() => datasetsApi.versions(id), [id]);

  const isForbidden = (err) => err instanceof ApiError && err.status === 403;

  if (datasetQuery.status === 'loading') {
    return (
      <div className="page">
        <LoadingState rows={6} />
      </div>
    );
  }
  if (datasetQuery.status === 'error') {
    return (
      <div className="page">
        {isForbidden(datasetQuery.error) ? (
          <PermissionDenied message="You don't have permission to view this dataset." />
        ) : (
          <ErrorState error={datasetQuery.error} onRetry={datasetQuery.reload} />
        )}
      </div>
    );
  }

  const dataset = datasetQuery.data;
  const latestVersion = dataset?.latestVersion;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <Link to="/datasets" className="text-muted" style={{ fontSize: '0.8rem', textDecoration: 'none' }}>
            ← Datasets
          </Link>
          <h1 style={{ marginTop: 4 }}>{dataset.name}</h1>
          <p className="page-subtitle">{dataset.description || 'No description provided.'}</p>
        </div>
        <div className="row">
          {dataset.status && <StatusBadge status={dataset.status} />}
          <RoleGate permission="DATASET_UPLOAD" fallbackTitle="Requires DATASET_UPLOAD permission">
            <button type="button" className="btn btn-primary" onClick={() => setUploadOpen(true)}>
              + Upload version
            </button>
          </RoleGate>
        </div>
      </div>

      <div className="card">
        <div className="card-title-row">
          <h2>Lifecycle</h2>
        </div>
        {latestVersion ? (
          <LifecycleStepper status={latestVersion.status} />
        ) : (
          <EmptyState title="No versions uploaded" description="Upload the first version to begin the lifecycle." icon="🚀" />
        )}
      </div>

      <div className="grid grid-cols-2">
        <div className="card">
          <div className="card-title-row">
            <h2>Lineage</h2>
          </div>
          <dl className="lineage-list">
            <div>
              <dt>Owner</dt>
              <dd>{dataset.ownerName || dataset.owner || '—'}</dd>
            </div>
            <div>
              <dt>Source system</dt>
              <dd>{latestVersion?.sourceSystem || '—'}</dd>
            </div>
            <div>
              <dt>Checksum (latest)</dt>
              <dd style={{ fontFamily: 'monospace', fontSize: '0.78rem', wordBreak: 'break-all' }}>
                {latestVersion?.checksum || '—'}
              </dd>
            </div>
            <div>
              <dt>Registered</dt>
              <dd>{formatDateTime(dataset.createdAt)}</dd>
            </div>
          </dl>
        </div>

        <div className="card">
          <div className="card-title-row">
            <h2>Latest version summary</h2>
          </div>
          {latestVersion ? (
            <dl className="lineage-list">
              <div>
                <dt>Version</dt>
                <dd>#{latestVersion.versionNumber ?? latestVersion.id}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>
                  <StatusBadge status={latestVersion.status} />
                </dd>
              </div>
              <div>
                <dt>Uploaded</dt>
                <dd>{formatDateTime(latestVersion.createdAt)}</dd>
              </div>
            </dl>
          ) : (
            <p className="text-muted">No version data yet.</p>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-title-row">
          <h2>Version history</h2>
        </div>
        {versionsQuery.status === 'loading' && <LoadingState rows={4} />}
        {versionsQuery.status === 'error' && isForbidden(versionsQuery.error) && (
          <PermissionDenied message="You don't have permission to view version history." />
        )}
        {versionsQuery.status === 'error' && !isForbidden(versionsQuery.error) && (
          <ErrorState error={versionsQuery.error} onRetry={versionsQuery.reload} />
        )}
        {versionsQuery.status === 'success' && (!versionsQuery.data || versionsQuery.data.length === 0) && (
          <EmptyState title="No versions yet" description="Uploaded versions will appear here, newest first." icon="🕓" />
        )}
        {versionsQuery.status === 'success' && versionsQuery.data?.length > 0 && (
          <ol className="version-timeline">
            {versionsQuery.data.map((version) => (
              <li key={version.id} className="version-timeline-item">
                <div className="version-timeline-dot" aria-hidden="true" />
                <div className="version-timeline-body">
                  <div className="row-between">
                    <strong>Version #{version.versionNumber ?? version.id}</strong>
                    <StatusBadge status={version.status} />
                  </div>
                  <div className="text-muted" style={{ fontSize: '0.78rem', marginTop: 2 }}>
                    {formatDateTime(version.createdAt)} · source: {version.sourceSystem || '—'}
                  </div>
                  {version.checksum && (
                    <div className="text-muted" style={{ fontSize: '0.72rem', fontFamily: 'monospace', marginTop: 4 }}>
                      {version.checksum}
                    </div>
                  )}
                </div>
              </li>
            ))}
          </ol>
        )}
      </div>

      {uploadOpen && <UploadVersionModal datasetId={id} onClose={() => setUploadOpen(false)} onDone={() => {
        setUploadOpen(false);
        datasetQuery.reload();
        versionsQuery.reload();
      }} />}
    </div>
  );
}

function UploadVersionModal({ datasetId, onClose, onDone }) {
  const [file, setFile] = useState(null);
  const [sourceSystem, setSourceSystem] = useState('');
  const [phase, setPhase] = useState('idle'); // idle | uploading | processing | error
  const [percent, setPercent] = useState(0);
  const [error, setError] = useState(null);
  const [touched, setTouched] = useState(false);

  const fileError = !file ? 'Select a file to upload.' : null;
  const sourceError = !sourceSystem.trim() ? 'Source system is required.' : null;

  async function handleSubmit(e) {
    e.preventDefault();
    setTouched(true);
    if (fileError || sourceError) return;
    setError(null);
    setPhase('uploading');
    setPercent(0);
    try {
      await uploadVersionWithProgress(datasetId, { file, sourceSystem: sourceSystem.trim() }, (pct) => {
        setPercent(pct);
        if (pct >= 100) setPhase('processing');
      });
      onDone();
    } catch (err) {
      setError(err);
      setPhase('error');
    }
  }

  return (
    <Modal title="Upload new version" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <ValidationErrorBanner error={error} />
        <div className="field">
          <label htmlFor="version-file">File</label>
          <input
            id="version-file"
            type="file"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            disabled={phase === 'uploading' || phase === 'processing'}
            aria-invalid={touched && Boolean(fileError)}
          />
          {touched && fileError && <span className="field-error">{fileError}</span>}
        </div>
        <div className="field">
          <label htmlFor="source-system">Source system</label>
          <input
            id="source-system"
            value={sourceSystem}
            onChange={(e) => setSourceSystem(e.target.value)}
            placeholder="e.g. SAP-ERP, Salesforce, CSV-drop"
            disabled={phase === 'uploading' || phase === 'processing'}
            aria-invalid={touched && Boolean(sourceError)}
          />
          {touched && sourceError && <span className="field-error">{sourceError}</span>}
        </div>

        {(phase === 'uploading' || phase === 'processing') && (
          <UploadProgress fileName={file?.name} percent={percent} status={phase} />
        )}

        <div className="modal-footer">
          <button type="button" className="btn" onClick={onClose} disabled={phase === 'uploading' || phase === 'processing'}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={phase === 'uploading' || phase === 'processing'}>
            {phase === 'uploading' ? `Uploading ${percent}%…` : phase === 'processing' ? 'Processing…' : 'Upload'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
