import { useEffect } from 'react';
import { createPortal } from 'react-dom';

export function Modal({ title, onClose, children, footer }) {
  useEffect(() => {
    function onKey(e) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  return createPortal(
    <div className="modal-scrim" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal-panel" role="dialog" aria-modal="true" aria-label={title}>
        <div className="row-between" style={{ marginBottom: 14 }}>
          <h2 style={{ fontSize: '1.1rem' }}>{title}</h2>
          <button type="button" className="btn btn-sm" onClick={onClose} aria-label="Close dialog">
            ✕
          </button>
        </div>
        <div>{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>,
    document.body
  );
}
