import './LifecycleStepper.css';

const STEPS = [
  { key: 'upload', label: 'Upload', statuses: ['UPLOADED'] },
  { key: 'validate', label: 'Validate', statuses: ['VALIDATING', 'READY', 'REJECTED'] },
  { key: 'transform', label: 'Transform', statuses: ['PROCESSING', 'FAILED', 'RETRY', 'REVIEW'] },
  { key: 'report', label: 'Report', statuses: ['COMPLETED'] },
];

const ERROR_STATUSES = new Set(['REJECTED', 'FAILED']);
const ATTENTION_STATUSES = new Set(['RETRY', 'REVIEW']);

function stepIndexForStatus(status) {
  const idx = STEPS.findIndex((step) => step.statuses.includes(status));
  return idx === -1 ? 0 : idx;
}

/**
 * Renders the dataset version lifecycle as UPLOADED → VALIDATING → READY|REJECTED →
 * PROCESSING → COMPLETED|FAILED (see docs/state-machine.md), collapsed to the four
 * user-facing stages the spec calls for: Upload, Validate, Transform, Report.
 */
export function LifecycleStepper({ status }) {
  const activeIndex = stepIndexForStatus(status);
  const isError = ERROR_STATUSES.has(status);
  const isAttention = ATTENTION_STATUSES.has(status);

  return (
    <ol className="lifecycle-stepper" aria-label="Dataset version lifecycle">
      {STEPS.map((step, index) => {
        let state = 'upcoming';
        if (index < activeIndex) state = 'complete';
        if (index === activeIndex) state = isError ? 'error' : isAttention ? 'attention' : 'current';

        return (
          <li key={step.key} className={`lifecycle-step lifecycle-step-${state}`}>
            <div className="lifecycle-step-marker" aria-hidden="true">
              {state === 'complete' && '✓'}
              {state === 'error' && '!'}
              {state === 'attention' && '↻'}
              {(state === 'current' || state === 'upcoming') && index + 1}
            </div>
            <div className="lifecycle-step-label">
              <span>{step.label}</span>
              {index === activeIndex && <span className="lifecycle-step-status">{status}</span>}
            </div>
            {index < STEPS.length - 1 && <div className="lifecycle-step-connector" aria-hidden="true" />}
          </li>
        );
      })}
    </ol>
  );
}
