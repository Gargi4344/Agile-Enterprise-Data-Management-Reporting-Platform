import { Link, useParams } from 'react-router-dom';
import { useAsync } from '../hooks/useAsync.js';
import { reportsApi } from '../api/reports.js';
import { LoadingState } from '../components/states/LoadingState.jsx';
import { ErrorState } from '../components/states/ErrorState.jsx';
import { PermissionDenied } from '../components/states/PermissionDenied.jsx';
import { KpiTile } from '../components/common/KpiTile.jsx';
import { formatDateTime } from '../utils/format.js';
import { ApiError } from '../api/client.js';

function humanize(key) {
  return key.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/^./, (c) => c.toUpperCase());
}

export function ReportDetailPage() {
  const { id } = useParams();
  const { status, data, error, reload } = useAsync(() => reportsApi.get(id), [id]);
  const isForbidden = error instanceof ApiError && error.status === 403;

  if (status === 'loading') {
    return (
      <div className="page">
        <LoadingState rows={5} />
      </div>
    );
  }
  if (status === 'error') {
    return (
      <div className="page">
        {isForbidden ? <PermissionDenied message="You don't have permission to view this report." /> : <ErrorState error={error} onRetry={reload} />}
      </div>
    );
  }

  const summaryEntries = data.summaryMetrics
    ? Object.entries(data.summaryMetrics).filter(([, v]) => typeof v === 'number' || typeof v === 'string')
    : [];

  return (
    <div className="page">
      <Link to="/reports" className="text-muted" style={{ fontSize: '0.8rem', textDecoration: 'none' }}>
        ← Reports
      </Link>
      <div className="page-header" style={{ marginTop: 4 }}>
        <div>
          <h1>Report #{data.id}</h1>
          <p className="page-subtitle">
            {data.reportType} · dataset version #{data.datasetVersionId} · generated {formatDateTime(data.createdAt)}
          </p>
        </div>
      </div>

      <div className="banner banner-info" role="note">
        This report is generated from synthetic demo data for portfolio purposes only — figures do not reflect a real
        production dataset.
      </div>

      <div className="card">
        <div className="card-title-row">
          <h2>Summary metrics</h2>
        </div>
        {summaryEntries.length === 0 && <p className="text-muted">No summary metrics available for this report.</p>}
        {summaryEntries.length > 0 && (
          <div className="grid grid-cols-4">
            {summaryEntries.map(([key, value]) => (
              <KpiTile key={key} label={humanize(key)} value={typeof value === 'number' ? Math.round(value * 100) / 100 : value} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
