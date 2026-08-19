import { useState } from 'react';
import { useAsync } from '../hooks/useAsync.js';
import { jobsApi } from '../api/jobs.js';
import { LoadingState } from '../components/states/LoadingState.jsx';
import { ErrorState, ValidationErrorBanner } from '../components/states/ErrorState.jsx';
import { PermissionDenied } from '../components/states/PermissionDenied.jsx';
import { EmptyState } from '../components/states/EmptyState.jsx';
import { RoleGate } from '../components/common/RoleGate.jsx';
import { Modal } from '../components/common/Modal.jsx';
import { JobTimeline } from '../components/jobs/JobTimeline.jsx';
import { ApiError } from '../api/client.js';

const STATUS_OPTIONS = ['PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'RETRYING'];

function asList(payload) {
  if (!payload) return [];
  return Array.isArray(payload) ? payload : payload.content || [];
}

export function JobsPage() {
  const [datasetVersionId, setDatasetVersionId] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [retryingId, setRetryingId] = useState(null);
  const [retryError, setRetryError] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);

  const jobsQuery = useAsync(
    () => jobsApi.list({ datasetVersionId: datasetVersionId || undefined, status: statusFilter || undefined }),
    [datasetVersionId, statusFilter]
  );
  const jobs = asList(jobsQuery.data);
  const isForbidden = jobsQuery.error instanceof ApiError && jobsQuery.error.status === 403;

  async function handleRetry(job) {
    setRetryingId(job.id);
    setRetryError(null);
    try {
      await jobsApi.retry(job.id);
      jobsQuery.reload();
    } catch (err) {
      setRetryError(err);
    } finally {
      setRetryingId(null);
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Processing jobs</h1>
          <p className="page-subtitle">Validation, transformation, and load jobs with retry and timing detail.</p>
        </div>
        <RoleGate permission="PROCESS_APPROVE" fallbackTitle="Requires PROCESS_APPROVE permission">
          <button type="button" className="btn btn-primary" onClick={() => setCreateOpen(true)}>
            + Submit job
          </button>
        </RoleGate>
      </div>

      <div className="card">
        <div className="row" style={{ marginBottom: 16, flexWrap: 'wrap' }}>
          <input
            placeholder="Filter by dataset version ID"
            value={datasetVersionId}
            onChange={(e) => setDatasetVersionId(e.target.value)}
            style={{ padding: '8px 10px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-strong)', background: 'var(--surface-2)', color: 'var(--text-primary)' }}
          />
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} aria-label="Filter by status">
            <option value="">All statuses</option>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>

        {retryError && (
          <p className="field-error" style={{ marginBottom: 10 }}>
            {retryError.message}
          </p>
        )}

        {jobsQuery.status === 'loading' && <LoadingState rows={5} />}
        {jobsQuery.status === 'error' && isForbidden && <PermissionDenied message="You don't have permission to view processing jobs." />}
        {jobsQuery.status === 'error' && !isForbidden && <ErrorState error={jobsQuery.error} onRetry={jobsQuery.reload} />}
        {jobsQuery.status === 'success' && jobs.length === 0 && (
          <EmptyState title="No jobs found" description="Jobs will appear here once processing starts." icon="⚙️" />
        )}
        {jobsQuery.status === 'success' && jobs.length > 0 && (
          <JobTimeline jobs={jobs} onRetry={handleRetry} retryingId={retryingId} />
        )}
      </div>

      {createOpen && (
        <CreateJobModal
          onClose={() => setCreateOpen(false)}
          onCreated={() => {
            setCreateOpen(false);
            jobsQuery.reload();
          }}
        />
      )}
    </div>
  );
}

function CreateJobModal({ onClose, onCreated }) {
  const [datasetVersionId, setDatasetVersionId] = useState('');
  const [jobType, setJobType] = useState('VALIDATION');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [touched, setTouched] = useState(false);

  const idError = !datasetVersionId ? 'Dataset version ID is required.' : null;

  async function handleSubmit(e) {
    e.preventDefault();
    setTouched(true);
    if (idError) return;
    setSubmitting(true);
    setError(null);
    try {
      await jobsApi.create({ datasetVersionId, jobType });
      onCreated();
    } catch (err) {
      setError(err);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="Submit processing job" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <ValidationErrorBanner error={error} />
        <div className="field">
          <label htmlFor="job-version-id">Dataset version ID</label>
          <input
            id="job-version-id"
            value={datasetVersionId}
            onChange={(e) => setDatasetVersionId(e.target.value)}
            aria-invalid={touched && Boolean(idError)}
          />
          {touched && idError && <span className="field-error">{idError}</span>}
        </div>
        <div className="field">
          <label htmlFor="job-type">Job type</label>
          <select id="job-type" value={jobType} onChange={(e) => setJobType(e.target.value)}>
            <option value="VALIDATION">Validation</option>
            <option value="TRANSFORMATION">Transformation</option>
            <option value="LOAD">Load</option>
          </select>
        </div>
        <div className="modal-footer">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Submitting…' : 'Submit job'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
