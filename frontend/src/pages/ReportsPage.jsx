import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAsync } from '../hooks/useAsync.js';
import { reportsApi } from '../api/reports.js';
import { LoadingState } from '../components/states/LoadingState.jsx';
import { ErrorState, ValidationErrorBanner } from '../components/states/ErrorState.jsx';
import { PermissionDenied } from '../components/states/PermissionDenied.jsx';
import { EmptyState } from '../components/states/EmptyState.jsx';
import { RoleGate } from '../components/common/RoleGate.jsx';
import { Modal } from '../components/common/Modal.jsx';
import { formatDateTime } from '../utils/format.js';
import { ApiError } from '../api/client.js';

function asList(payload) {
  if (!payload) return [];
  return Array.isArray(payload) ? payload : payload.content || [];
}

export function ReportsPage() {
  const [createOpen, setCreateOpen] = useState(false);
  const reportsQuery = useAsync(() => reportsApi.list(), []);
  const reports = asList(reportsQuery.data);
  const isForbidden = reportsQuery.error instanceof ApiError && reportsQuery.error.status === 403;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Reports</h1>
          <p className="page-subtitle">Generated reports summarizing dataset version quality and outcomes.</p>
        </div>
        <RoleGate permission="REPORT_VIEW" fallbackTitle="Requires REPORT_VIEW permission">
          <button type="button" className="btn btn-primary" onClick={() => setCreateOpen(true)}>
            + Generate report
          </button>
        </RoleGate>
      </div>

      <div className="card">
        {reportsQuery.status === 'loading' && <LoadingState rows={4} />}
        {reportsQuery.status === 'error' && isForbidden && <PermissionDenied message="You don't have permission to view reports." />}
        {reportsQuery.status === 'error' && !isForbidden && <ErrorState error={reportsQuery.error} onRetry={reportsQuery.reload} />}
        {reportsQuery.status === 'success' && reports.length === 0 && (
          <EmptyState title="No reports generated yet" description="Generate a report from a completed dataset version." icon="📄" />
        )}
        {reportsQuery.status === 'success' && reports.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Report</th>
                  <th>Type</th>
                  <th>Dataset version</th>
                  <th>Generated</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {reports.map((report) => (
                  <tr key={report.id}>
                    <td>#{report.id}</td>
                    <td>{report.reportType}</td>
                    <td>{report.datasetVersionId}</td>
                    <td>{formatDateTime(report.createdAt)}</td>
                    <td>
                      <Link to={`/reports/${report.id}`} className="btn btn-sm">
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {createOpen && (
        <GenerateReportModal
          onClose={() => setCreateOpen(false)}
          onCreated={() => {
            setCreateOpen(false);
            reportsQuery.reload();
          }}
        />
      )}
    </div>
  );
}

function GenerateReportModal({ onClose, onCreated }) {
  const [datasetVersionId, setDatasetVersionId] = useState('');
  const [reportType, setReportType] = useState('QUALITY_SUMMARY');
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
      await reportsApi.create({ datasetVersionId, reportType });
      onCreated();
    } catch (err) {
      setError(err);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="Generate report" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <ValidationErrorBanner error={error} />
        <div className="field">
          <label htmlFor="report-version-id">Dataset version ID</label>
          <input
            id="report-version-id"
            value={datasetVersionId}
            onChange={(e) => setDatasetVersionId(e.target.value)}
            aria-invalid={touched && Boolean(idError)}
          />
          {touched && idError && <span className="field-error">{idError}</span>}
        </div>
        <div className="field">
          <label htmlFor="report-type">Report type</label>
          <select id="report-type" value={reportType} onChange={(e) => setReportType(e.target.value)}>
            <option value="QUALITY_SUMMARY">Quality summary</option>
            <option value="PROCESSING_SUMMARY">Processing summary</option>
            <option value="AUDIT_SUMMARY">Audit summary</option>
          </select>
        </div>
        <div className="modal-footer">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Generating…' : 'Generate'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
