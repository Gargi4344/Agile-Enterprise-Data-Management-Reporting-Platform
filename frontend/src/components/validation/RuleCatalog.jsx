import { useState } from 'react';
import { useAsync } from '../../hooks/useAsync.js';
import { validationApi } from '../../api/validation.js';
import { LoadingState } from '../states/LoadingState.jsx';
import { ErrorState, ValidationErrorBanner } from '../states/ErrorState.jsx';
import { EmptyState } from '../states/EmptyState.jsx';
import { StatusBadge } from '../common/StatusBadge.jsx';
import { RoleGate } from '../common/RoleGate.jsx';
import { Modal } from '../common/Modal.jsx';
import { useAuth } from '../../context/AuthContext.jsx';

export function RuleCatalog({ datasetId }) {
  const { can } = useAuth();
  const [createOpen, setCreateOpen] = useState(false);
  const { status, data, error, reload } = useAsync(() => validationApi.rules(datasetId), [datasetId]);
  const canManage = can('RULE_MANAGE_ALL') || can('RULE_MANAGE_OWN');

  return (
    <div>
      <div className="row-between" style={{ marginBottom: 12 }}>
        <p className="text-secondary" style={{ margin: 0, fontSize: '0.85rem' }}>
          Global rules apply to every dataset; scoped rules apply only to this one.
        </p>
        <RoleGate permission={can('RULE_MANAGE_ALL') ? 'RULE_MANAGE_ALL' : 'RULE_MANAGE_OWN'} fallbackTitle="Requires rule management permission">
          <button type="button" className="btn btn-sm btn-primary" onClick={() => setCreateOpen(true)}>
            + New rule
          </button>
        </RoleGate>
      </div>

      {status === 'loading' && <LoadingState rows={3} />}
      {status === 'error' && <ErrorState error={error} onRetry={reload} />}
      {status === 'success' && (!data || data.length === 0) && (
        <EmptyState title="No rules defined" description="Create a rule to start validating this dataset." icon="📏" />
      )}
      {status === 'success' && data?.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Rule</th>
                <th>Scope</th>
                <th>Severity</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {data.map((rule) => (
                <tr key={rule.id}>
                  <td>{rule.name}</td>
                  <td>{rule.datasetId ? 'Dataset' : 'Global'}</td>
                  <td>
                    <StatusBadge status={rule.severity} />
                  </td>
                  <td>{rule.active !== false ? <StatusBadge status="ACTIVE" label="Active" /> : <StatusBadge status="INACTIVE" label="Inactive" />}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {createOpen && canManage && (
        <CreateRuleModal datasetId={datasetId} onClose={() => setCreateOpen(false)} onCreated={() => { setCreateOpen(false); reload(); }} />
      )}
    </div>
  );
}

function CreateRuleModal({ datasetId, onClose, onCreated }) {
  const [name, setName] = useState('');
  const [severity, setSeverity] = useState('BLOCKER');
  const [scopeToDataset, setScopeToDataset] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [touched, setTouched] = useState(false);

  const nameError = !name.trim() ? 'Rule name is required.' : null;

  async function handleSubmit(e) {
    e.preventDefault();
    setTouched(true);
    if (nameError) return;
    setSubmitting(true);
    setError(null);
    try {
      await validationApi.createRule({
        name: name.trim(),
        severity,
        datasetId: scopeToDataset ? datasetId : null,
      });
      onCreated();
    } catch (err) {
      setError(err);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal title="New validation rule" onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <ValidationErrorBanner error={error} />
        <div className="field">
          <label htmlFor="rule-name">Name</label>
          <input id="rule-name" value={name} onChange={(e) => setName(e.target.value)} aria-invalid={touched && Boolean(nameError)} />
          {touched && nameError && <span className="field-error">{nameError}</span>}
        </div>
        <div className="field">
          <label htmlFor="rule-severity">Severity</label>
          <select id="rule-severity" value={severity} onChange={(e) => setSeverity(e.target.value)}>
            <option value="BLOCKER">Blocker</option>
            <option value="WARNING">Warning</option>
          </select>
        </div>
        <div className="field">
          <label>
            <input type="checkbox" checked={scopeToDataset} onChange={(e) => setScopeToDataset(e.target.checked)} style={{ marginRight: 6 }} />
            Scope to this dataset only
          </label>
        </div>
        <div className="modal-footer">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create rule'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
