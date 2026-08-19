import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="page state-block-center" style={{ paddingTop: 80 }}>
      <h1>404</h1>
      <p className="text-secondary">This page doesn't exist.</p>
      <Link className="btn btn-primary" to="/overview">
        Back to overview
      </Link>
    </div>
  );
}
