import { useAsync } from '../hooks/useAsync.js';
import { reportsApi } from '../api/reports.js';
import { auditApi } from '../api/audit.js';
import { LoadingState } from '../components/states/LoadingState.jsx';
import { ErrorState } from '../components/states/ErrorState.jsx';
import { PermissionDenied } from '../components/states/PermissionDenied.jsx';
import { EmptyState } from '../components/states/EmptyState.jsx';
import { KpiTile } from '../components/common/KpiTile.jsx';
import { ApiError } from '../api/client.js';
import { formatRelative } from '../utils/format.js';
import { useAuth } from '../context/AuthContext.jsx';

const KPI_LABELS = {
  totalDatasets: 'Datasets registered',
  activeDatasets: 'Active datasets',
  pendingValidation: 'Pending validation',
  datasetsAwaitingValidation: 'Awaiting validation',
  runningJobs: 'Jobs running',
  activeJobs: 'Jobs running',
  failedJobs: 'Jobs failed',
  averageQualityScore: 'Avg. quality score',
  avgQualityScore: 'Avg. quality score',
  reportsGenerated: 'Reports generated',
  totalReports: 'Reports generated',
  totalUsers: 'Active users',
};

const PERCENT_KEYS = new Set(['averageQualityScore', 'avgQualityScore']);

function humanize(key) {
  return key
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/^./, (c) => c.toUpperCase());
}

export function OverviewPage() {
  const { can, user } = useAuth();
  const kpiQuery = useAsync(() => reportsApi.kpiOverview(), []);
  const canViewAudit = can('AUDIT_VIEW_ALL') || can('AUDIT_VIEW_LIMITED');
  const activityQuery = useAsync(
    () => (canViewAudit ? auditApi.list({ page: 0, size: 8 }) : Promise.resolve({ content: [] })),
    [canViewAudit]
  );

  const kpiEntries = kpiQuery.data
    ? Object.entries(kpiQuery.data).filter(([, v]) => typeof v === 'number' || typeof v === 'string')
    : [];

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Operations overview</h1>
          <p className="page-subtitle">Welcome back, {user?.fullName?.split(' ')[0] || 'there'}. Here's the current state of the platform.</p>
        </div>
      </div>

      <div className="card">
        <div className="card-title-row">
          <h2>Key metrics</h2>
        </div>
        {kpiQuery.status === 'loading' && <LoadingState rows={2} />}
        {kpiQuery.status === 'error' && kpiQuery.error instanceof ApiError && kpiQuery.error.status === 403 && (
          <PermissionDenied message="You don't have permission to view platform KPIs." />
        )}
        {kpiQuery.status === 'error' && !(kpiQuery.error instanceof ApiError && kpiQuery.error.status === 403) && (
          <ErrorState error={kpiQuery.error} onRetry={kpiQuery.reload} />
        )}
        {kpiQuery.status === 'success' && kpiEntries.length === 0 && (
          <EmptyState title="No KPI data yet" description="KPIs will appear once datasets have been processed." icon="📊" />
        )}
        {kpiQuery.status === 'success' && kpiEntries.length > 0 && (
          <div className="grid grid-cols-4">
            {kpiEntries.map(([key, value]) => (
              <KpiTile
                key={key}
                label={KPI_LABELS[key] || humanize(key)}
                value={typeof value === 'number' ? Math.round(value * 10) / 10 : value}
                unit={PERCENT_KEYS.has(key) ? '%' : undefined}
              />
            ))}
          </div>
        )}
      </div>

      <div className="card">
        <div className="card-title-row">
          <h2>Recent activity</h2>
        </div>
        {!canViewAudit && <PermissionDenied message="You don't have permission to view the audit trail." />}
        {canViewAudit && activityQuery.status === 'loading' && <LoadingState rows={4} />}
        {canViewAudit && activityQuery.status === 'error' && <ErrorState error={activityQuery.error} onRetry={activityQuery.reload} />}
        {canViewAudit && activityQuery.status === 'success' && activityQuery.data.content.length === 0 && (
          <EmptyState title="No recent activity" icon="🕒" />
        )}
        {canViewAudit && activityQuery.status === 'success' && activityQuery.data.content.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Action</th>
                  <th>Actor</th>
                  <th>Target</th>
                  <th>When</th>
                </tr>
              </thead>
              <tbody>
                {activityQuery.data.content.map((entry) => (
                  <tr key={entry.id}>
                    <td>{entry.action}</td>
                    <td>{entry.actorName || entry.actorId}</td>
                    <td>
                      {entry.targetType} #{entry.targetId}
                    </td>
                    <td>{formatRelative(entry.createdAt || entry.timestamp)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
