import { useEffect, useMemo, useState } from 'react';
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer, useMap } from 'react-leaflet';
import type { LatLngTuple } from 'leaflet';
import { getOrderTracking } from '../api';
import { ApiError } from '../api/client';
import type { GeoPoint, OrderTracking } from '../types';
import { DRONE_STATUS_LABELS } from '../types';

const POLL_MS = 2000;
const IN_DELIVERY_STATUS = 1;

function toLatLng(point: GeoPoint): LatLngTuple {
  return [point.latitude, point.longitude];
}

function MapBounds({ points }: { points: LatLngTuple[] }) {
  const map = useMap();

  useEffect(() => {
    if (points.length === 0) return;
    if (points.length === 1) {
      map.setView(points[0], 14);
      return;
    }
    map.fitBounds(points, { padding: [36, 36], maxZoom: 14 });
  }, [map, points]);

  return null;
}

interface OrderTrackingMapProps {
  token: string;
  orderId: number;
  orderStatus: number;
}

export function OrderTrackingMap({ token, orderId, orderStatus }: OrderTrackingMapProps) {
  const [tracking, setTracking] = useState<OrderTracking | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (orderStatus !== IN_DELIVERY_STATUS) {
      setTracking(null);
      return;
    }

    let cancelled = false;

    async function poll() {
      try {
        const data = await getOrderTracking(token, orderId);
        if (!cancelled) {
          setTracking(data);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : 'Failed to load tracking');
        }
      }
    }

    poll();
    const timer = window.setInterval(poll, POLL_MS);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [token, orderId, orderStatus]);

  const mapPoints = useMemo(() => {
    if (!tracking) return [] as LatLngTuple[];
    const points: LatLngTuple[] = [];
    if (tracking.station_position) points.push(toLatLng(tracking.station_position));
    if (tracking.drone_position) points.push(toLatLng(tracking.drone_position));
    if (tracking.delivery_destination) points.push(toLatLng(tracking.delivery_destination));
    return points;
  }, [tracking]);

  if (orderStatus !== IN_DELIVERY_STATUS) {
    return null;
  }

  if (error) {
    return <p className="error">{error}</p>;
  }

  if (!tracking) {
    return <p className="muted">Loading live tracking…</p>;
  }

  if (!tracking.trackable || !tracking.drone_position || !tracking.delivery_destination) {
    return (
      <p className="muted">
        Live map will appear when the drone is en route to your address.
      </p>
    );
  }

  const droneLatLng = toLatLng(tracking.drone_position);
  const destinationLatLng = toLatLng(tracking.delivery_destination);
  const stationLatLng = tracking.station_position
    ? toLatLng(tracking.station_position)
    : null;

  return (
    <div className="tracking-map-wrap">
      <div className="tracking-meta">
        <span className="badge delivery-live">Live</span>
        <span>
          {tracking.drone_code ?? 'Drone'} ·{' '}
          {DRONE_STATUS_LABELS[tracking.drone_status ?? 1] ?? 'Delivery'} ·{' '}
          {tracking.drone_speed?.toFixed(1) ?? '0.0'} m/s · {tracking.drone_battery ?? '—'}% battery
        </span>
      </div>
      <MapContainer
        className="tracking-map"
        center={droneLatLng}
        zoom={13}
        scrollWheelZoom={false}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapBounds points={mapPoints} />
        {stationLatLng && (
          <CircleMarker center={stationLatLng} radius={9} pathOptions={{ color: '#2563eb', fillColor: '#2563eb', fillOpacity: 0.85 }}>
            <Popup>Hub (origin)</Popup>
          </CircleMarker>
        )}
        <CircleMarker center={droneLatLng} radius={11} pathOptions={{ color: '#d97706', fillColor: '#f59e0b', fillOpacity: 1 }}>
          <Popup>{tracking.drone_code ?? 'Drone'}</Popup>
        </CircleMarker>
        <CircleMarker center={destinationLatLng} radius={9} pathOptions={{ color: '#dc2626', fillColor: '#ef4444', fillOpacity: 0.9 }}>
          <Popup>Delivery address</Popup>
        </CircleMarker>
        <Polyline
          positions={[droneLatLng, destinationLatLng]}
          pathOptions={{ color: '#d97706', dashArray: '6 8', weight: 3 }}
        />
      </MapContainer>
      <div className="map-legend">
        <span><i className="legend-dot hub" /> Hub</span>
        <span><i className="legend-dot drone" /> Drone</span>
        <span><i className="legend-dot dest" /> Destination</span>
      </div>
    </div>
  );
}
