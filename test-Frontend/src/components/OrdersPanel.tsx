import { useCallback, useEffect, useState } from 'react';
import { getOrderDetail, getOrdersByUser } from '../api';
import { ApiError } from '../api/client';
import { OrderTrackingMap } from './OrderTrackingMap';
import { useAuth } from '../context/AuthContext';
import type { Order, OrderDetailResponse } from '../types';
import { ORDER_STATUS_LABELS, STATION_NAMES } from '../types';

function formatPrice(value: number) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function formatDate(value: string) {
  return new Date(value).toLocaleString();
}

export function OrdersPanel() {
  const { token, user, isAuthenticated } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [selected, setSelected] = useState<OrderDetailResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadOrders = useCallback(async () => {
    if (!token || !user) return;
    setLoading(true);
    setError(null);
    try {
      const data = await getOrdersByUser(token, user.id);
      setOrders(data.sort((a, b) => b.id - a.id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  }, [token, user]);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  const viewDetail = async (orderId: number) => {
    if (!token) return;
    setError(null);
    try {
      const detail = await getOrderDetail(token, orderId);
      setSelected(detail);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load order detail');
    }
  };

  if (!isAuthenticated) {
    return (
      <section className="panel muted-panel">
        <h2>Orders</h2>
        <p>Log in to view your order history.</p>
      </section>
    );
  }

  return (
    <section className="panel">
      <div className="shop-header">
        <h2>Your orders</h2>
        <button type="button" className="btn ghost" onClick={loadOrders} disabled={loading}>
          Refresh
        </button>
      </div>

      {loading && <p className="muted">Loading orders…</p>}
      {error && <p className="error">{error}</p>}

      {orders.length === 0 && !loading && !error ? (
        <p className="muted">No orders yet. Add items in the Shop tab and checkout.</p>
      ) : orders.length > 0 ? (
        <div className="orders-layout">
          <ul className="order-list">
            {orders.map((order) => (
              <li key={order.id}>
                <button type="button" className="order-row" onClick={() => viewDetail(order.id)}>
                  <span>
                    <strong>{order.order_no}</strong>
                    <span className="muted block">
                      {STATION_NAMES[order.station_id] ?? `Station ${order.station_id}`} ·{' '}
                      {ORDER_STATUS_LABELS[order.status] ?? `Status ${order.status}`}
                    </span>
                  </span>
                  <span>
                    {formatPrice(order.total_amount)}
                    <span className="muted block">{formatDate(order.created_at)}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>

          {selected && (
            <aside className="order-detail">
              <h3>{selected.order.order_no}</h3>
              <p className="muted">Order ID: {selected.order.id}</p>
              <p>
                Status:{' '}
                {ORDER_STATUS_LABELS[selected.order.status] ?? selected.order.status}
              </p>
              <p>
                Hub:{' '}
                {STATION_NAMES[selected.order.station_id] ?? selected.order.station_id}
              </p>
              <p>Total: {formatPrice(selected.order.total_amount)}</p>
              <p className="muted">Drone ID: {selected.order.assigned_drone_id ?? '—'}</p>

              <OrderTrackingMap
                token={token!}
                orderId={selected.order.id}
                orderStatus={selected.order.status}
              />

              <h4>Items</h4>
              <ul className="cart-lines">
                {selected.items.map((item) => (
                  <li key={item.id}>
                    <span>Product #{item.product_id}</span>
                    <span>×{item.quantity}</span>
                    <span>{formatPrice(item.unit_price * item.quantity)}</span>
                  </li>
                ))}
              </ul>
            </aside>
          )}
        </div>
      ) : null}
    </section>
  );
}
