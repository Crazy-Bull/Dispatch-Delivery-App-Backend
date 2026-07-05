export interface User {
  id: number;
  name: string;
  address: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Product {
  id: number;
  name: string;
  description: string | null;
  price: number;
  stock: number;
  image_url: string | null;
}

export interface DeliveryPlan {
  station_id: number;
  station_name: string;
  station_address: string;
  total_amount: number;
  distance_km: number;
  available_drones: number;
  feasible: boolean;
  infeasibility_reason: string | null;
}

export interface OrderPlansRequest {
  longitude: number;
  latitude: number;
  items: OrderItemRequest[];
}

export interface OrderItemRequest {
  product_id: number;
  quantity: number;
}

export interface CreateOrderRequest {
  station_id: number;
  longitude: number;
  latitude: number;
  items: OrderItemRequest[];
}

export interface Order {
  id: number;
  order_no: string;
  user_id: number;
  station_id: number;
  assigned_drone_id: number | null;
  delivery_position: string;
  status: number;
  total_amount: number;
  created_at: string;
}

export interface OrderItem {
  id: number;
  order_id: number;
  product_id: number;
  quantity: number;
  unit_price: number;
}

export interface OrderDetailResponse {
  order: Order;
  items: OrderItem[];
}

export interface CartLine {
  product: Product;
  quantity: number;
}

export const STATION_NAMES: Record<number, string> = {
  1: 'Mission Hub',
  2: 'Marina Hub',
  3: 'Sunset Hub',
};

export const ORDER_STATUS_LABELS: Record<number, string> = {
  0: 'Pending',
  1: 'In delivery',
  2: 'Delivered',
  3: 'Completed',
};

export interface GeoPoint {
  longitude: number;
  latitude: number;
}

export interface OrderTracking {
  order_id: number;
  order_status: number;
  trackable: boolean;
  drone_position: GeoPoint | null;
  delivery_destination: GeoPoint | null;
  station_position: GeoPoint | null;
  drone_code: string | null;
  drone_status: number | null;
  drone_speed: number | null;
  drone_battery: number | null;
}

export interface Drone {
  id: number;
  drone_code: string;
  station_id: number;
  battery_level: number;
  position: string;
  altitude: number;
  speed: number;
  status: number;
}

export const DRONE_STATUS_LABELS: Record<number, string> = {
  0: 'Waiting',
  1: 'Delivery',
  2: 'Return',
};

export const SEED_USERS = [
  { email: 'alice.chen@example.com', password: 'password123', label: 'Alice (seed)' },
  { email: 'bob.martinez@example.com', password: 'password123', label: 'Bob (seed)' },
];
