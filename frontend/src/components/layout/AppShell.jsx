import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { TopNav } from './TopNav.jsx';
import { ActivityDrawer } from './ActivityDrawer.jsx';
import { DemoBanner } from './DemoBanner.jsx';

export function AppShell() {
  const [activityOpen, setActivityOpen] = useState(false);

  return (
    <div className="app-shell">
      <DemoBanner />
      <TopNav onToggleActivity={() => setActivityOpen((v) => !v)} />
      <main>
        <Outlet />
      </main>
      <ActivityDrawer open={activityOpen} onClose={() => setActivityOpen(false)} />
    </div>
  );
}
