import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { LifecycleStepper } from '../components/datasets/LifecycleStepper.jsx';

function stepEl(label) {
  return screen.getByText(label).closest('li');
}

describe('LifecycleStepper', () => {
  it('marks Upload as current when status is UPLOADED', () => {
    render(<LifecycleStepper status="UPLOADED" />);
    expect(stepEl('Upload')).toHaveClass('lifecycle-step-current');
    expect(stepEl('Validate')).toHaveClass('lifecycle-step-upcoming');
    expect(stepEl('Transform')).toHaveClass('lifecycle-step-upcoming');
    expect(stepEl('Report')).toHaveClass('lifecycle-step-upcoming');
  });

  it('marks Validate as current and Upload as complete when status is VALIDATING', () => {
    render(<LifecycleStepper status="VALIDATING" />);
    expect(stepEl('Upload')).toHaveClass('lifecycle-step-complete');
    expect(stepEl('Validate')).toHaveClass('lifecycle-step-current');
  });

  it('marks Validate as an error state when status is REJECTED', () => {
    render(<LifecycleStepper status="REJECTED" />);
    expect(stepEl('Validate')).toHaveClass('lifecycle-step-error');
    expect(stepEl('Transform')).toHaveClass('lifecycle-step-upcoming');
  });

  it('marks Transform as an attention state when status is RETRY', () => {
    render(<LifecycleStepper status="RETRY" />);
    expect(stepEl('Transform')).toHaveClass('lifecycle-step-attention');
  });

  it('marks every step complete when status is COMPLETED', () => {
    render(<LifecycleStepper status="COMPLETED" />);
    expect(stepEl('Upload')).toHaveClass('lifecycle-step-complete');
    expect(stepEl('Validate')).toHaveClass('lifecycle-step-complete');
    expect(stepEl('Transform')).toHaveClass('lifecycle-step-complete');
    expect(stepEl('Report')).toHaveClass('lifecycle-step-current');
  });
});
