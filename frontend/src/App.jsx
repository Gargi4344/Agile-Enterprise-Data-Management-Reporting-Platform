import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext.jsx';
import { ProtectedRoute } from './components/common/ProtectedRoute.jsx';
import { AppShell } from './components/layout/AppShell.jsx';
import { LoginPage } from './pages/LoginPage.jsx';
import { OverviewPage } from './pages/OverviewPage.jsx';
import { DatasetsPage } from './pages/DatasetsPage.jsx';
import { DatasetDetailPage } from './pages/DatasetDetailPage.jsx';
import { ValidationPage } from './pages/ValidationPage.jsx';
import { JobsPage } from './pages/JobsPage.jsx';
import { ReportsPage } from './pages/ReportsPage.jsx';
import { ReportDetailPage } from './pages/ReportDetailPage.jsx';
import { AdminPage } from './pages/AdminPage.jsx';
import { AuditPage } from './pages/AuditPage.jsx';
import { NotFoundPage } from './pages/NotFoundPage.jsx';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={
              <ProtectedRoute>
                <AppShell />
              </ProtectedRoute>
            }
          >
            <Route path="/overview" element={<OverviewPage />} />
            <Route path="/datasets" element={<DatasetsPage />} />
            <Route path="/datasets/:id" element={<DatasetDetailPage />} />
            <Route path="/validation" element={<ValidationPage />} />
            <Route path="/jobs" element={<JobsPage />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/reports/:id" element={<ReportDetailPage />} />
            <Route path="/admin" element={<AdminPage />} />
            <Route path="/audit" element={<AuditPage />} />
          </Route>
          <Route path="/" element={<Navigate to="/overview" replace />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
