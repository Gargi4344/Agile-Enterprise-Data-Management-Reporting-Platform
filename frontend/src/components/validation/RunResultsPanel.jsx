import { Fragment, useState } from 'react';
import { usePaginatedFetch } from '../../hooks/usePaginatedFetch.js';
import { validationApi } from '../../api/validation.js';
import { LoadingState } from '../states/LoadingState.jsx';
import { ErrorState } from '../states/ErrorState.jsx';
import { EmptyState } from '../states/EmptyState.jsx';
import { Pagination } from '../common/Pagination.jsx';
import { StatusBadge } from '../common/StatusBadge.jsx';

export function RunResultsPanel({ runId }) {
  const [passedFilter, setPassedFilter] = useState('');
  const [severityFilter, setSeverityFilter] = useState('');
  const [expandedId, setExpandedId] = useState(null);

  const { content, status, error, page, setPage, totalPages, reload } = usePaginatedFetch(
    ({ page: p, size }) => validationApi.runResults(runId, { page: p, size, passed: passedFilter, severity: severityFilter }),
    { size: 10, extraDeps: [runId, passedFilter, severityFilter] }
  );

  return (
    <div>
      <div className="row" style={{ marginBottom: 12, flexWrap: 'wrap' }}>
        <select value={passedFilter} onChange={(e) => setPassedFilter(e.target.value)} aria-label="Filter by outcome">
          <option value="">All outcomes</option>
          <option value="false">Failed only</option>
          <option value="true">Passed only</option>
        </select>
        <select value={severityFilter} onChange={(e) => setSeverityFilter(e.target.value)} aria-label="Filter by severity">
          <option value="">All severities</option>
          <option value="BLOCKER">Blocker</option>
          <option value="WARNING">Warning</option>
        </select>
      </div>

      {status === 'loading' && <LoadingState rows={4} />}
      {status === 'error' && <ErrorState error={error} onRetry={reload} />}
      {status === 'success' && content.length === 0 && (
        <EmptyState title="No matching rows" description="Adjust the filters to see more results." icon="🧾" />
      )}
      {status === 'success' && content.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th></th>
                <th>Row</th>
                <th>Rule</th>
                <th>Severity</th>
                <th>Outcome</th>
              </tr>
            </thead>
            <tbody>
              {content.map((row) => {
                const isOpen = expandedId === row.id;
                return (
                  <Fragment key={row.id}>
                    <tr onClick={() => setExpandedId(isOpen ? null : row.id)} style={{ cursor: 'pointer' }}>
                      <td>{isOpen ? '▾' : '▸'}</td>
                      <td>{row.rowNumber ?? row.recordId ?? row.id}</td>
                      <td>{row.ruleName || row.ruleId}</td>
                      <td>{row.severity && <StatusBadge status={row.severity} />}</td>
                      <td>{row.passed ? <StatusBadge status="PASSED" label="Passed" /> : <StatusBadge status="FAILED" label="Failed" />}</td>
                    </tr>
                    {isOpen && (
                      <tr>
                        <td colSpan={5}>
                          <pre className="row-detail-pre">{JSON.stringify(row.details ?? row, null, 2)}</pre>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      {status === 'success' && <Pagination page={page} totalPages={totalPages} onChange={setPage} />}
    </div>
  );
}
