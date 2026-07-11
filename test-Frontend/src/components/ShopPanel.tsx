import { useCallback, useEffect, useMemo, useState } from 'react';
import { createOrder, getDeliveryPlans, getProducts, getRecommendations } from '../api';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import type { CartLine, DeliveryPlan, Product, RecommendedItem } from '../types';
import { STATION_NAMES } from '../types';

const DEFAULT_COORDS = { longitude: -122.4194, latitude: 37.7749 };
const PRODUCT_IMAGE_FALLBACK =
  'data:image/svg+xml,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400"><rect fill="#e2e8f0" width="400" height="400"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#64748b" font-family="sans-serif" font-size="18">No photo</text></svg>',
  );

interface PendingRecommendation {
  item: RecommendedItem;
  selected: boolean;
  quantity: number;
}

function formatPrice(value: number) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

export function ShopPanel() {
  const { token, user, isAuthenticated } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [cart, setCart] = useState<CartLine[]>([]);
  const [coords, setCoords] = useState(DEFAULT_COORDS);
  const [loadingProducts, setLoadingProducts] = useState(false);
  const [loadingPlans, setLoadingPlans] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [lastOrderId, setLastOrderId] = useState<number | null>(null);
  const [plans, setPlans] = useState<DeliveryPlan[] | null>(null);
  const [selectedPlan, setSelectedPlan] = useState<DeliveryPlan | null>(null);

  const [recommendQuery, setRecommendQuery] = useState('');
  const [recommendSummary, setRecommendSummary] = useState<string | null>(null);
  const [pendingRecommendations, setPendingRecommendations] = useState<PendingRecommendation[]>([]);
  const [loadingRecommendations, setLoadingRecommendations] = useState(false);

  const loadProducts = useCallback(async () => {
    setLoadingProducts(true);
    setError(null);
    try {
      const data = await getProducts();
      setProducts(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load products');
    } finally {
      setLoadingProducts(false);
    }
  }, []);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  const cartTotal = useMemo(
    () => cart.reduce((sum, line) => sum + line.product.price * line.quantity, 0),
    [cart],
  );

  const resetCheckoutState = () => {
    setPlans(null);
    setSelectedPlan(null);
  };

  const addToCartWithQuantity = (product: Product, quantity: number) => {
    if (quantity <= 0) return;
    setMessage(null);
    setError(null);
    resetCheckoutState();
    if (product.stock <= 0) {
      setError('Out of stock at all hubs');
      return;
    }
    const qty = Math.min(quantity, product.stock);
    setCart((prev) => {
      const existing = prev.find((line) => line.product.id === product.id);
      if (existing) {
        const nextQty = Math.min(existing.quantity + qty, product.stock);
        if (nextQty === existing.quantity) {
          setError(`Only ${product.stock} available at the best-stocked hub`);
          return prev;
        }
        return prev.map((line) =>
          line.product.id === product.id ? { ...line, quantity: nextQty } : line,
        );
      }
      return [...prev, { product, quantity: qty }];
    });
  };

  const addToCart = (product: Product) => {
    addToCartWithQuantity(product, 1);
  };

  const updateQuantity = (productId: number, delta: number) => {
    resetCheckoutState();
    setCart((prev) =>
      prev
        .map((line) => {
          if (line.product.id !== productId) return line;
          const nextQty = line.quantity + delta;
          if (nextQty <= 0) return null;
          if (nextQty > line.product.stock) {
            setError(`Only ${line.product.stock} available at the best-stocked hub`);
            return line;
          }
          return { ...line, quantity: nextQty };
        })
        .filter((line): line is CartLine => line != null),
    );
  };

  const clearCart = () => {
    setCart([]);
    resetCheckoutState();
    setError(null);
    setMessage(null);
  };

  const fetchRecommendations = async () => {
    if (!token || !recommendQuery.trim()) return;
    setLoadingRecommendations(true);
    setError(null);
    setMessage(null);
    setRecommendSummary(null);
    setPendingRecommendations([]);
    try {
      const result = await getRecommendations(token, recommendQuery.trim());
      setRecommendSummary(result.summary);
      setPendingRecommendations(
        result.items.map((item) => ({
          item,
          selected: true,
          quantity: item.quantity,
        })),
      );
      if (result.items.length === 0) {
        setError('No matching items found. Try rephrasing your request.');
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to get recommendations');
    } finally {
      setLoadingRecommendations(false);
    }
  };

  const toggleRecommendation = (productId: number) => {
    setPendingRecommendations((prev) =>
      prev.map((entry) =>
        entry.item.product_id === productId ? { ...entry, selected: !entry.selected } : entry,
      ),
    );
  };

  const updateRecommendationQuantity = (productId: number, delta: number) => {
    setPendingRecommendations((prev) =>
      prev.map((entry) => {
        if (entry.item.product_id !== productId) return entry;
        const maxStock = entry.item.product.stock;
        const nextQty = Math.max(1, Math.min(entry.quantity + delta, maxStock));
        return { ...entry, quantity: nextQty };
      }),
    );
  };

  const addSelectedRecommendationsToCart = () => {
    const selected = pendingRecommendations.filter((entry) => entry.selected);
    if (selected.length === 0) {
      setError('Select at least one recommended item.');
      return;
    }
    selected.forEach((entry) => {
      addToCartWithQuantity(entry.item.product, entry.quantity);
    });
    setMessage(`Added ${selected.length} recommended item${selected.length === 1 ? '' : 's'} to cart.`);
    setPendingRecommendations([]);
    setRecommendSummary(null);
    setRecommendQuery('');
  };

  const loadPlans = async () => {
    if (!token || cart.length === 0) return;
    setLoadingPlans(true);
    setError(null);
    setMessage(null);
    setSelectedPlan(null);
    try {
      const result = await getDeliveryPlans(token, {
        longitude: coords.longitude,
        latitude: coords.latitude,
        items: cart.map((line) => ({
          product_id: line.product.id,
          quantity: line.quantity,
        })),
      });
      setPlans(result);
      const firstFeasible = result.find((plan) => plan.feasible);
      setSelectedPlan(firstFeasible ?? null);
      if (result.every((plan) => !plan.feasible)) {
        setError('No hub can fulfill this order right now. Try reducing quantities.');
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load delivery plans');
      setPlans(null);
    } finally {
      setLoadingPlans(false);
    }
  };

  const placeOrder = async () => {
    if (!token || !user || cart.length === 0 || selectedPlan == null) return;
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      const result = await createOrder(token, {
        station_id: selectedPlan.station_id,
        longitude: coords.longitude,
        latitude: coords.latitude,
        items: cart.map((line) => ({
          product_id: line.product.id,
          quantity: line.quantity,
        })),
      });
      setLastOrderId(result.order.id);
      setMessage(
        `Order ${result.order.order_no} created from ${STATION_NAMES[selectedPlan.station_id] ?? selectedPlan.station_name} — total ${formatPrice(result.order.total_amount)}`,
      );
      clearCart();
      await loadProducts();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Checkout failed');
    } finally {
      setSubmitting(false);
    }
  };

  if (!isAuthenticated) {
    return (
      <section className="panel muted-panel">
        <h2>Shop</h2>
        <p>Log in to browse products and place orders.</p>
      </section>
    );
  }

  return (
    <section className="panel shop-panel">
      <div className="shop-header">
        <div>
          <h2>Shop &amp; checkout</h2>
          <p className="muted">All hubs carry the full catalog — pick a fulfillment hub at checkout.</p>
        </div>
        <button type="button" className="btn ghost" onClick={loadProducts} disabled={loadingProducts}>
          Refresh
        </button>
      </div>

      <section className="recommend-panel">
        <h3>AI shopping assistant</h3>
        <p className="muted">
          Describe what you need in plain language — we&apos;ll suggest items you can review and add to your cart.
        </p>
        <div className="recommend-form">
          <textarea
            className="recommend-input"
            rows={3}
            placeholder="e.g. I'm hosting brunch for 4 friends and want something healthy with drinks"
            value={recommendQuery}
            onChange={(e) => setRecommendQuery(e.target.value)}
            disabled={loadingRecommendations}
          />
          <button
            type="button"
            className="btn primary"
            onClick={fetchRecommendations}
            disabled={loadingRecommendations || !recommendQuery.trim()}
          >
            {loadingRecommendations ? 'Thinking…' : 'Get recommendations'}
          </button>
        </div>

        {recommendSummary && (
          <p className="recommend-summary">{recommendSummary}</p>
        )}

        {pendingRecommendations.length > 0 && (
          <div className="recommend-results">
            <ul className="recommend-list">
              {pendingRecommendations.map((entry) => (
                <li key={entry.item.product_id} className="recommend-item">
                  <label className="recommend-check">
                    <input
                      type="checkbox"
                      checked={entry.selected}
                      onChange={() => toggleRecommendation(entry.item.product_id)}
                    />
                  </label>
                  <img
                    className="cart-thumb"
                    src={entry.item.product.image_url ?? PRODUCT_IMAGE_FALLBACK}
                    alt=""
                    onError={(e) => {
                      e.currentTarget.src = PRODUCT_IMAGE_FALLBACK;
                    }}
                  />
                  <div className="recommend-meta">
                    <strong>{entry.item.product.name}</strong>
                    <span className="muted block">{entry.item.reason}</span>
                    <span className="muted">{formatPrice(entry.item.product.price)} each</span>
                  </div>
                  <div className="qty-controls">
                    <button
                      type="button"
                      onClick={() => updateRecommendationQuantity(entry.item.product_id, -1)}
                      disabled={!entry.selected}
                    >
                      −
                    </button>
                    <span>{entry.quantity}</span>
                    <button
                      type="button"
                      onClick={() => updateRecommendationQuantity(entry.item.product_id, 1)}
                      disabled={!entry.selected}
                    >
                      +
                    </button>
                  </div>
                </li>
              ))}
            </ul>
            <button
              type="button"
              className="btn secondary recommend-confirm"
              onClick={addSelectedRecommendationsToCart}
            >
              Add selected to cart
            </button>
          </div>
        )}
      </section>

      {loadingProducts && <p className="muted">Loading products…</p>}
      {error && <p className="error">{error}</p>}
      {message && <p className="success">{message}</p>}
      {lastOrderId != null && (
        <p className="muted">Latest order ID: {lastOrderId} — see Orders tab for details.</p>
      )}

      <div className="shop-grid">
        <div className="product-list">
          {products.map((product) => (
            <article key={product.id} className="product-card">
              <img
                className="product-image"
                src={product.image_url ?? PRODUCT_IMAGE_FALLBACK}
                alt={product.name}
                loading="lazy"
                onError={(e) => {
                  e.currentTarget.src = PRODUCT_IMAGE_FALLBACK;
                }}
              />
              <div className="product-meta">
                <h3>{product.name}</h3>
                <p className="muted">{product.description}</p>
                <p>
                  <strong>{formatPrice(product.price)}</strong>
                  <span className="badge">Max hub stock: {product.stock}</span>
                </p>
              </div>
              <button
                type="button"
                className="btn primary"
                onClick={() => addToCart(product)}
                disabled={product.stock <= 0}
              >
                Add
              </button>
            </article>
          ))}
          {!loadingProducts && products.length === 0 && (
            <p className="muted">No products found. Is the backend running on port 8080?</p>
          )}
        </div>

        <aside className="cart">
          <h3>Cart</h3>
          {cart.length === 0 ? (
            <p className="muted">Cart is empty.</p>
          ) : (
            <ul className="cart-lines">
              {cart.map((line) => (
                <li key={line.product.id}>
                  <img
                    className="cart-thumb"
                    src={line.product.image_url ?? PRODUCT_IMAGE_FALLBACK}
                    alt=""
                    onError={(e) => {
                      e.currentTarget.src = PRODUCT_IMAGE_FALLBACK;
                    }}
                  />
                  <span>{line.product.name}</span>
                  <div className="qty-controls">
                    <button type="button" onClick={() => updateQuantity(line.product.id, -1)}>−</button>
                    <span>{line.quantity}</span>
                    <button type="button" onClick={() => updateQuantity(line.product.id, 1)}>+</button>
                  </div>
                  <span>{formatPrice(line.product.price * line.quantity)}</span>
                </li>
              ))}
            </ul>
          )}

          <p className="cart-total">
            <strong>Subtotal:</strong> {formatPrice(cartTotal)}
          </p>

          <fieldset className="coords">
            <legend>Delivery coordinates (SF area)</legend>
            <label>
              Longitude
              <input
                type="number"
                step="0.0001"
                value={coords.longitude}
                onChange={(e) => {
                  setCoords({ ...coords, longitude: Number(e.target.value) });
                  resetCheckoutState();
                }}
              />
            </label>
            <label>
              Latitude
              <input
                type="number"
                step="0.0001"
                value={coords.latitude}
                onChange={(e) => {
                  setCoords({ ...coords, latitude: Number(e.target.value) });
                  resetCheckoutState();
                }}
              />
            </label>
          </fieldset>

          <div className="cart-actions">
            <button type="button" className="btn secondary" onClick={clearCart} disabled={cart.length === 0}>
              Clear cart
            </button>
            <button
              type="button"
              className="btn primary"
              onClick={loadPlans}
              disabled={cart.length === 0 || loadingPlans}
            >
              {loadingPlans ? 'Loading plans…' : 'Get delivery plans'}
            </button>
          </div>

          {plans != null && plans.length > 0 && (
            <div className="plans-section">
              <h4>Choose a fulfillment hub</h4>
              <ul className="plan-list">
                {plans.map((plan) => (
                  <li key={plan.station_id}>
                    <label className={`plan-card ${!plan.feasible ? 'plan-disabled' : ''}`}>
                      <input
                        type="radio"
                        name="delivery-plan"
                        disabled={!plan.feasible}
                        checked={selectedPlan?.station_id === plan.station_id}
                        onChange={() => setSelectedPlan(plan)}
                      />
                      <div className="plan-body">
                        <strong>{plan.station_name}</strong>
                        <span className="muted block">{plan.station_address}</span>
                        <span className="block">
                          {formatPrice(plan.total_amount)} · {plan.distance_km.toFixed(2)} km ·{' '}
                          {plan.available_drones} drone{plan.available_drones === 1 ? '' : 's'}
                        </span>
                        {!plan.feasible && plan.infeasibility_reason && (
                          <span className="error block">{plan.infeasibility_reason}</span>
                        )}
                      </div>
                    </label>
                  </li>
                ))}
              </ul>
              <button
                type="button"
                className="btn primary plan-confirm"
                onClick={placeOrder}
                disabled={selectedPlan == null || submitting}
              >
                {submitting ? 'Placing order…' : 'Confirm order'}
              </button>
            </div>
          )}
        </aside>
      </div>
    </section>
  );
}
