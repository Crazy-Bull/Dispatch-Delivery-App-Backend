package com.laioffer.dispatchdeliveryapp.service;

import com.laioffer.dispatchdeliveryapp.config.OrderAssignmentProperties;
import com.laioffer.dispatchdeliveryapp.dto.CreateOrderRequest;
import com.laioffer.dispatchdeliveryapp.dto.DeliveryPlanResponse;
import com.laioffer.dispatchdeliveryapp.dto.GeoPoint;
import com.laioffer.dispatchdeliveryapp.dto.OrderDetailResponse;
import com.laioffer.dispatchdeliveryapp.dto.OrderItemRequest;
import com.laioffer.dispatchdeliveryapp.dto.OrderPlansRequest;
import com.laioffer.dispatchdeliveryapp.dto.OrderTrackingResponse;
import com.laioffer.dispatchdeliveryapp.entity.Drone;
import com.laioffer.dispatchdeliveryapp.entity.Order;
import com.laioffer.dispatchdeliveryapp.entity.OrderItem;
import com.laioffer.dispatchdeliveryapp.entity.Product;
import com.laioffer.dispatchdeliveryapp.entity.Station;
import com.laioffer.dispatchdeliveryapp.model.DroneStatus;
import com.laioffer.dispatchdeliveryapp.model.OrderStatus;
import com.laioffer.dispatchdeliveryapp.repository.DroneRepository;
import com.laioffer.dispatchdeliveryapp.repository.OrderItemRepository;
import com.laioffer.dispatchdeliveryapp.repository.OrderRepository;
import com.laioffer.dispatchdeliveryapp.repository.ProductRepository;
import com.laioffer.dispatchdeliveryapp.repository.StationProductRepository;
import com.laioffer.dispatchdeliveryapp.repository.StationRepository;
import com.laioffer.dispatchdeliveryapp.repository.UserRepository;
import com.laioffer.dispatchdeliveryapp.util.GeographyUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StationProductRepository stationProductRepository;
    private final DroneRepository droneRepository;
    private final StationRepository stationRepository;
    private final OrderAssignmentProperties assignmentProperties;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            StationProductRepository stationProductRepository,
            DroneRepository droneRepository,
            StationRepository stationRepository,
            OrderAssignmentProperties assignmentProperties) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.stationProductRepository = stationProductRepository;
        this.droneRepository = droneRepository;
        this.stationRepository = stationRepository;
        this.assignmentProperties = assignmentProperties;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
    }

    public List<Order> getOrdersByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("User not found: " + userId);
        }
        return orderRepository.findByUserId(userId);
    }

    public OrderDetailResponse getOrderDetail(Long id) {
        Order order = getById(id);
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        return new OrderDetailResponse(order, items);
    }

    public OrderTrackingResponse getOrderTracking(Long orderId, Long userId) {
        Order order = getById(orderId);
        if (!order.userId().equals(userId)) {
            throw new NoSuchElementException("Order not found: " + orderId);
        }

        GeoPoint deliveryDestination = orderRepository.findDeliveryPositionWktByOrderId(orderId)
                .flatMap(GeographyUtils::parseGeoPoint)
                .orElse(null);

        GeoPoint stationPosition = stationRepository.findPositionWktById(order.stationId())
                .flatMap(GeographyUtils::parseGeoPoint)
                .orElse(null);

        boolean trackable = order.status() == OrderStatus.ASSIGNED && order.assignedDroneId() != null;
        if (!trackable) {
            return new OrderTrackingResponse(
                    orderId,
                    order.status(),
                    false,
                    null,
                    deliveryDestination,
                    stationPosition,
                    null,
                    null,
                    null,
                    null);
        }

        Drone drone = droneRepository.findById(order.assignedDroneId())
                .orElseThrow(() -> new NoSuchElementException("Drone not found: " + order.assignedDroneId()));

        GeoPoint dronePosition = droneRepository.findPositionWktById(drone.id())
                .flatMap(GeographyUtils::parseGeoPoint)
                .orElse(null);

        return new OrderTrackingResponse(
                orderId,
                order.status(),
                drone.status() == DroneStatus.DELIVERY && dronePosition != null,
                dronePosition,
                deliveryDestination,
                stationPosition,
                drone.droneCode(),
                drone.status(),
                drone.speed(),
                drone.batteryLevel());
    }

    public List<DeliveryPlanResponse> getDeliveryPlans(OrderPlansRequest request) {
        validateOrderItemsRequest(request.longitude(), request.latitude(), request.items());

        String deliveryWkt = deliveryWkt(request.longitude(), request.latitude());
        List<ResolvedItem> resolvedItems = resolveItems(request.items());
        BigDecimal totalAmount = computeTotal(resolvedItems);

        return stationRepository.findAll().stream()
                .map(station -> buildPlan(station, deliveryWkt, resolvedItems, totalAmount))
                .sorted(Comparator
                        .comparing(DeliveryPlanResponse::feasible).reversed()
                        .thenComparing(DeliveryPlanResponse::distanceKm))
                .toList();
    }

    @Transactional
    public OrderDetailResponse createOrder(Long userId, CreateOrderRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("User not found: " + userId);
        }
        validateCreateOrderRequest(request);

        if (!stationRepository.existsById(request.stationId())) {
            throw new IllegalArgumentException("Station not found: " + request.stationId());
        }

        List<ResolvedItem> resolvedItems = resolveAndValidateItemsAtStation(request.stationId(), request.items());
        BigDecimal totalAmount = computeTotal(resolvedItems);

        Drone drone = findAvailableDrone(request.stationId());

        String orderNo = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String deliveryWkt = deliveryWkt(request.longitude(), request.latitude());

        orderRepository.insertOrder(
                orderNo,
                userId,
                request.stationId(),
                drone.id(),
                deliveryWkt,
                OrderStatus.ASSIGNED,
                totalAmount);

        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalStateException("Failed to load order after insert: " + orderNo));

        List<OrderItem> savedItems = new ArrayList<>();
        for (ResolvedItem item : resolvedItems) {
            int updated = stationProductRepository.decrementStock(
                    request.stationId(), item.product().id(), item.quantity());
            if (updated == 0) {
                throw new IllegalStateException("Insufficient stock for product: " + item.product().name());
            }
            OrderItem saved = orderItemRepository.save(new OrderItem(
                    null, order.id(), item.product().id(), item.quantity(), item.product().price()));
            savedItems.add(saved);
        }

        droneRepository.assignToDelivery(
                drone.id(), DroneStatus.DELIVERY, assignmentProperties.deliverySpeed());

        return new OrderDetailResponse(order, savedItems);
    }

    private DeliveryPlanResponse buildPlan(
            Station station,
            String deliveryWkt,
            List<ResolvedItem> items,
            BigDecimal totalAmount) {
        double distanceKm = stationRepository.findDistanceKmToPoint(station.id(), deliveryWkt).orElse(0.0);
        int availableDrones = countAvailableDrones(station.id());
        String stockIssue = stockIssueAtStation(station.id(), items);
        String reason = null;
        boolean feasible = true;

        if (stockIssue != null) {
            feasible = false;
            reason = stockIssue;
        } else if (availableDrones == 0) {
            feasible = false;
            reason = "No available drone with sufficient battery";
        }

        return new DeliveryPlanResponse(
                station.id(),
                station.name(),
                station.address(),
                totalAmount,
                Math.round(distanceKm * 100.0) / 100.0,
                availableDrones,
                feasible,
                reason);
    }

    private Drone findAvailableDrone(Long stationId) {
        List<Drone> candidates = droneRepository.findByStationIdAndStatusAndMinBatteryLevel(
                stationId, DroneStatus.WAITING, assignmentProperties.minBatteryLevel());

        return candidates.stream()
                .max(Comparator.comparingInt(Drone::batteryLevel))
                .orElseThrow(() -> new IllegalStateException(
                        "No available drone at station " + stationId + " with sufficient battery"));
    }

    private int countAvailableDrones(Long stationId) {
        return droneRepository.findByStationIdAndStatusAndMinBatteryLevel(
                stationId, DroneStatus.WAITING, assignmentProperties.minBatteryLevel()).size();
    }

    private List<ResolvedItem> resolveAndValidateItemsAtStation(Long stationId, List<OrderItemRequest> items) {
        List<ResolvedItem> resolved = resolveItems(items);
        String stockIssue = stockIssueAtStation(stationId, resolved);
        if (stockIssue != null) {
            throw new IllegalArgumentException(stockIssue);
        }
        if (countAvailableDrones(stationId) == 0) {
            throw new IllegalStateException("No available drone at station " + stationId + " with sufficient battery");
        }
        return resolved;
    }

    private String stockIssueAtStation(Long stationId, List<ResolvedItem> items) {
        for (ResolvedItem item : items) {
            int stock = stationProductRepository.findStock(stationId, item.product().id()).orElse(0);
            if (stock < item.quantity()) {
                return "Insufficient stock for " + item.product().name() + " at this hub";
            }
        }
        return null;
    }

    private List<ResolvedItem> resolveItems(List<OrderItemRequest> items) {
        List<ResolvedItem> resolved = new ArrayList<>();
        for (OrderItemRequest item : items) {
            if (item.productId() == null) {
                throw new IllegalArgumentException("Product ID is required");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }

            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.productId()));

            resolved.add(new ResolvedItem(product, item.quantity()));
        }
        return resolved;
    }

    private BigDecimal computeTotal(List<ResolvedItem> items) {
        return items.stream()
                .map(item -> item.product().price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateCreateOrderRequest(CreateOrderRequest request) {
        validateOrderItemsRequest(request.longitude(), request.latitude(), request.items());
        if (request.stationId() == null) {
            throw new IllegalArgumentException("Station ID is required");
        }
    }

    private void validateOrderItemsRequest(Double longitude, Double latitude, List<OrderItemRequest> items) {
        if (longitude == null || latitude == null) {
            throw new IllegalArgumentException("Delivery coordinates are required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }
    }

    private static String deliveryWkt(double longitude, double latitude) {
        return "SRID=4326;POINT(%f %f)".formatted(longitude, latitude);
    }

    private record ResolvedItem(Product product, int quantity) {}
}
