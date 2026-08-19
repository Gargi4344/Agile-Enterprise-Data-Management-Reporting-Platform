import { useEffect, useState } from 'react';
import { useAsync } from '../hooks/useAsync.js';
import { datasetsApi } from '../api/datasets.js';
import { validationApi } from '../api/validation.js';
import { LoadingState } from '../components/states/LoadingState.jsx';
import { ErrorState } from '../components/states/ErrorState.jsx';
import { PermissionDenied } from '../components/states/PermissionDenied.jsx';
import { EmptyState } from '../components/states/EmptyState.jsx';
import { RoleGate } from '../components/common/RoleGate.jsx';
import { StatusBadge } from '../components/common/StatusBadge.jsx';
import { QualityScoreGauge } from '../components/validation/QualityScoreGauge.jsx';
import { RuleFailureBreakdown } from '../components/validation/RuleFailureBreakdown.jsx';
import { QualityTrendChart } from '../components/validation/QualityTrendChart.jsx';
import { RuleCatalog } from '../components/validation/RuleCatalog.jsx';
import { RunResultsPanel } from '../components/validation/RunResultsPanel.jsx';
import { formatDateTime } from '../utils/format.js';
import { ApiError } from '../api/client.js';

function asList(payload) {
  if (!payload) return [];
  return Array.isArray(payload) ? payload : payload.content || [];
}

export function ValidationPage() {
  const [datasetId, setDatasetId] = useState('');
  const [versionId, setVersionId] = useState('');
  const [triggering, setTriggering] = useState(false);
  const [triggerError, setTriggerError] = useState(null);

  const datasetsQuery = useAsync(() => datasetsApi.list({ page: 0, size: 100 }), []);
  const versionsQuery = useAsync(() => (datasetId ? datasetsApi.versions(datasetId) : Promise.resolve([])), [datasetId]);
  const runsQuery = useAsync(() => (versionId ? validationApi.runHistory(versionId) : Promise.resolve(null)), [versionId]);

  useEffect(() => {
    setVersionId('');
  }, [datasetId]);

  const runs = asList(runsQuery.data);
  const latestRun = runs[0];

  const isForbidden = datasetsQuery.error instanceof ApiError && datasetsQuery.error.status === 403;

  async function handleTriggerRun() {
    if (!versionId) return;
    setTriggering(true);
    setTriggerError(null);
    try {
      await validationApi.runValidation(versionId);
      runsQuery.reload();
    } catch (err) {
      setTriggerError(err);
    } finally {
      setTriggering(false);
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Quality center</h1>
          <p className="page-subtitle">Validation runs, quality scores, and rule catalog across dataset versions.</p>
        </div>
      </div>

      <div className="card">
        <div className="card-title-row">
          <h2>Select a version</h2>
        </div>
        {datasetsQuery.status === 'loading' && <LoadingState rows={1} />}
        {datasetsQuery.status === 'error' && isForbidden && <PermissionDenied message="You don't have permission to view datasets." />}
        {datasetsQuery.status === 'error' && !isForbidden && <ErrorState error={datasetsQuery.error} onRetry={datasetsQuery.reload} />}
        {datasetsQuery.status === 'success' && (
          <div className="row" style={{ flexWrap: 'wrap' }}>
            <select value={datasetId} onChange={(e) => setDatasetId(e.target.value)} aria-label="Select dataset">
              <option value="">Choose dataset…</option>
              {datasetsQuery.data.content.map((ds) => (
                <option key={ds.id} value={ds.id}>
                  {ds.name}
                </option>
              ))}
            </select>
            <select
              value={versionId}
              onChange={(e) => setVersionId(e.target.value)}
              disabled={!datasetId || versionsQuery.status === 'loading'}
              aria-label="Select version"
            >
              <option value="">Choose version…</option>
              {(versionsQuery.data || []).map((v) => (
                <option key={v.id} value={v.id}>
                  Version #{v.versionNumber ?? v.id} — {v.status}
                </option>
              ))}
            </select>
            <RoleGate permission="VALIDATION_RUN" fallbackTitle="Requires VALIDATION_RUN permission">
              <button type="button" className="btn btn-primary" onClick={handleTriggerRun} disabled={!versionId || triggering}>
                {triggering ? 'Running…' : 'Run validation'}
              </button>
            </RoleGate>
          </div>
        )}
        {triggerError && (
          <p className="field-error" style={{ marginTop: 10 }}>
            {triggerError.message}
          </p>
        )}
      </div>

      {!versionId && (
        <div className="card">
          <EmptyState title="Pick a dataset version" description="Select a dataset and version above to see its quality data." icon="🧪" />
        </div>
      )}

      {versionId && (
        <>
          <div className="grid grid-cols-2">
            <div className="card">
              <div className="card-title-row">
                <h2>Overall quality score</h2>
              </div>
              {runsQuery.status === 'loading' && <LoadingState rows={3} />}
              {runsQuery.status === 'error' && <ErrorState error={runsQuery.error} onRetry={runsQuery.reload} />}
              {runsQuery.status === 'success' && !latestRun && (
                <EmptyState title="No validation runs yet" description="Trigger a run to compute the quality score." icon="🎯" />
              )}
              {runsQuery.status === 'success' && latestRun && (
                <QualityScoreGauge
                  score={latestRun.qualityScore}
                  acceptedRecords={latestRun.acceptedRecords}
                  totalRecords={latestRun.totalRecords}
                />
              )}
            </div>

            <div className="card">
              <div className="card-title-row">
                <h2>Failure breakdown by rule</h2>
              </div>
              {runsQuery.status === 'success' && latestRun?.ruleBreakdown?.length > 0 && (
                <RuleFailureBreakdown items={latestRun.ruleBreakdown} />
              )}
              {runsQuery.status === 'success' && (!latestRun || !latestRun.ruleBreakdown?.length) && (
                <EmptyState title="No failures recorded" description="This run has no rule breakdown to display." icon="✅" />
              )}
            </div>
          </div>

          {runsQuery.status === 'success' && latestRun?.topFailingColumns?.length > 0 && (
            <div className="card">
              <div className="card-title-row">
                <h2>Top failing columns</h2>
              </div>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Column</th>
                      <th>Failures</th>
                    </tr>
                  </thead>
                  <tbody>
                    {latestRun.topFailingColumns.map((col) => (
                      <tr key={col.column}>
                        <td>{col.column}</td>
                        <td>{col.failureCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          <div className="card">
            <div className="card-title-row">
              <h2>Quality trend</h2>
            </div>
            {runs.length > 1 ? <QualityTrendChart runs={runs} /> : <p className="text-muted">Run validation more than once to see a trend.</p>}
          </div>

          <div className="card">
            <div className="card-title-row">
              <h2>Run history</h2>
            </div>
            {runs.length === 0 && <EmptyState title="No runs yet" icon="📜" />}
            {runs.length > 0 && (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Run</th>
                      <th>Quality score</th>
                      <th>Status</th>
                      <th>Run at</th>
                    </tr>
                  </thead>
                  <tbody>
                    {runs.map((run) => (
                      <tr key={run.id}>
                        <td>#{run.id}</td>
                        <td>{run.qualityScore?.toFixed?.(1) ?? '—'}%</td>
                        <td>{run.status && <StatusBadge status={run.status} />}</td>
                        <td>{formatDateTime(run.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {latestRun && (
            <div className="card">
              <div className="card-title-row">
                <h2>Failing rows (latest run)</h2>
              </div>
              <RunResultsPanel runId={latestRun.id} />
            </div>
          )}
        </>
      )}

      <div className="card">
        <div className="card-title-row">
          <h2>Rule catalog</h2>
        </div>
        {datasetId ? (
          <RuleCatalog datasetId={datasetId} />
        ) : (
          <p className="text-muted">Select a dataset above to view and manage its rule catalog (global rules always apply).</p>
        )}
      </div>
    </div>
  );
}
