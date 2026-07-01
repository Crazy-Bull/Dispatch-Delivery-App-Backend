import { apiFetch } from './client';
import type {
  AuthResponse,
  CreateOrderRequest,
  DeliveryPlan,
  Drone,
  Order,
  OrderDetailResponse,
  OrderPlansRequest,
  OrderTracking,
  Product,
} from '../types';

export function signUp(payload: {
  name: string;
  address: string;
  email: string;
  password: string;
}) {
  return apiFetch<AuthResponse>('/auth/signup', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function login(payload: { email: string; password: string }) {
  return apiFetch<AuthResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getProducts() {
  return apiFetch<Product[]>('/products');
}

export function getDeliveryPlans(token: string, payload: OrderPlansRequest) {
  return apiFetch<DeliveryPlan[]>(
    '/orders/plans',
    { method: 'POST', body: JSON.stringify(payload) },
    token,
  );
}

export function createOrder(token: string, payload: CreateOrderRequest) {
  return apiFetch<OrderDetailResponse>(
    '/orders',
    { method: 'POST', body: JSON.stringify(payload) },
    token,
  );
}

export function getOrdersByUser(token: string, userId: number) {
  return apiFetch<Order[]>(`/orders/search?user_id=${userId}`, {}, token);
}

export function getOrderDetail(token: string, orderId: number) {
  return apiFetch<OrderDetailResponse>(`/orders/${orderId}`, {}, token);
}

export function getOrderTracking(token: string, orderId: number) {
  return apiFetch<OrderTracking>(`/orders/${orderId}/tracking`, {}, token);
}

export function getAllDrones(token: string) {
  return apiFetch<Drone[]>('/drones', {}, token);
}
