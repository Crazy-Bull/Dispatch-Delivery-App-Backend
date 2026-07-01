import { useCallback, useEffect, useMemo, useState } from 'react';
import { getAllDrones } from '../api';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import type { Drone } from '../types';
import { DRONE_STATUS_LABELS, STATION_NAMES } from '../types';

const REFRESH_MS = 5000;

function parsePosition(position: string): string {
  const match = position.match(/POINT\s*\(\s*([-\d.]+)\s+([-\d.]+)\s*\)/i);
  if (!match) return position;
  const lon = Number(match[1]).toFixed(4);
  const lat = Number(match[2]).toFixed(4);
  return `${lat}, ${lon}`;
}

function statusClass(status: number): string {
  if (status === 1) return 'status-pill delivery';
  if (status === 2) return 'status-pill return';
  return 'status-pill waiting';
}

function batteryClass(level: number): string {
  if (level <= 20) return 'battery low';
  if (level <= 50) return 'battery medium';
  return 'battery high';
}

export function DronesPanel() {
  const { token, isAuthenticated } = useAuth();
  const [drones, setDrones] = useState<Drone[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [stationFilter, setStationFilter] = useState<number | 'all'>('all');
  const [statusFilter, setStatusFilter] = useState<number | 'all'>('all');
  const [autoRefresh, setAutoRefresh] = useState(true);

  const loadDrones = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const data = await getAllDrones(token);
      setDrones(data.sort((a, b) => a.station_id - b.station_id || a.drone_code.localeCompare(b.drone_code)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load drones');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadDrones();
  }, [loadDrones]);

  useEffect(() => {
    if (!autoRefresh || !isAuthenticated) return;
    const timer = window.setInterval(loadDrones, REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [autoRefresh, isAuthenticated, loadDrones]);

  const filtered = useMemo(
    () =>
      drones.filter((drone) => {
        if (stationFilter !== 'all' && drone.station_id !== stationFilter) return false;
        if (statusFilter !== 'all' && drone.status !== statusFilter) return false;
        return true;
      }),
    [drones, stationFilter, statusFilter],
  );

  const summary = useMemo(() => {
    const counts = { waiting: 0, delivery: 0, return: 0 };
    for (const drone of drones) {
      if (drone.status === 1) counts.delivery += 1;
      else if (drone.status === 2) counts.return += 1;
      else counts.waiting += 1;
    }
    return counts;
  }, [drones]);

  if (!isAuthenticated) {
    return (
      <section className="panel muted-panel">
        <h2>Drones</h2>
        <p>Log in to view drone fleet status.</p>
      </section>
    );
  }

  return (
    <section className="panel">
      <div className="shop-header">
        <div>
          <h2>Drone fleet</h2>
          <p className="muted">Live status from <code>GET /drones</code></p>
        </div>
        <div className="filter-row">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
            />
            Auto-refresh (5s)
          </label>
          <button type="button" className="btn ghost" onClick={loadDrones} disabled={loading}>
            Refresh
          </button>
        </div>
      </div>

      <div className="drone-summary">
        <div className="summary-card">
          <span className="summary-value">{drones.length}</span>
          <span className="muted">Total</span>
        </div>
        <div className="summary-card waiting-card">
          <span className="summary-value">{summary.waiting}</span>
          <span className="muted">Waiting</span>
        </div>
        <div className="summary-card delivery-card">
          <span className="summary-value">{summary.delivery}</span>
          <span className="muted">Delivery</span>
        </div>
        <div className="summary-card return-card">
          <span className="summary-value">{summary.return}</span>
          <span className="muted">Return</span>
        </div>
      </div>

      <div className="filter-row drone-filters">
        <label>
          Hub
          <select
            value={stationFilter === 'all' ? 'all' : String(stationFilter)}
            onChange={(e) =>
              setStationFilter(e.target.value === 'all' ? 'all' : Number(e.target.value))
            }
          >
            <option value="all">All hubs</option>
            {Object.entries(STATION_NAMES).map(([id, name]) => (
              <option key={id} value={id}>
                {name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Status
          <select
            value={statusFilter === 'all' ? 'all' : String(statusFilter)}
            onChange={(e) =>
              setStatusFilter(e.target.value === 'all' ? 'all' : Number(e.target.value))
            }
          >
            <option value="all">All statuses</option>
            {Object.entries(DRONE_STATUS_LABELS).map(([id, label]) => (
              <option key={id} value={id}>
                {label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {loading && drones.length === 0 && <p className="muted">Loading drones…</p>}
      {error && <p className="error">{error}</p>}

      {filtered.length === 0 && !loading && !error ? (
        <p className="muted">No drones match the current filters.</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Hub</th>
                <th>Status</th>
                <th>Battery</th>
                <th>Speed</th>
                <th>Altitude</th>
                <th>Position</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((drone) => (
                <tr key={drone.id}>
                  <td>
                    <strong>{drone.drone_code}</strong>
                    <span className="muted block">ID {drone.id}</span>
                  </td>
                  <td>{STATION_NAMES[drone.station_id] ?? `Station ${drone.station_id}`}</td>
                  <td>
                    <span className={statusClass(drone.status)}>
                      {DRONE_STATUS_LABELS[drone.status] ?? `Status ${drone.status}`}
                    </span>
                  </td>
                  <td>
                    <div className="battery-cell">
                      <div className={batteryClass(drone.battery_level)}>
                        <div
                          className="battery-fill"
                          style={{ width: `${Math.max(0, Math.min(100, drone.battery_level))}%` }}
                        />
                      </div>
                      <span>{drone.battery_level}%</span>
                    </div>
                  </td>
                  <td>{drone.speed.toFixed(1)} m/s</td>
                  <td>{drone.altitude.toFixed(0)} m</td>
                  <td className="mono">{parsePosition(drone.position)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
