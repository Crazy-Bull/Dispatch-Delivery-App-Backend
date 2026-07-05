package com.laioffer.dispatchdeliveryapp.service;

import com.laioffer.dispatchdeliveryapp.config.OrderAssignmentProperties;
import com.laioffer.dispatchdeliveryapp.dto.CreateOrderRequest;
import com.laioffer.dispatchdeliveryapp.dto.DeliveryPlanResponse;
import com.laioffer.dispatchdeliveryapp.dto.GeoPoint;
import com.laioffer.dispatchdeliveryapp.dto.OrderDetailResponse;
import com.laioffer.dispatchdeliveryapp.dto.OrderItemRequest;
import com.laioffer.dispatchdeliveryapp.dto.OrderItemWithProduct;
import com.laioffer.dispatchdeliveryapp.dto.OrderPlansRequest;
import com.laioffer.dispatchdeliveryapp.dto.OrderTrackingResponse;
import com.laioffer.dispatchdeliveryapp.entity.Drone;
import com.laioffer.dispatchdeliveryapp.entity.Order;
import com.laioffer.dispatchdeliveryapp.entity.OrderItem;
import com.laioffer.dispatchdeliveryapp.entity.Product;
import com.laioffer.dispatchdeliveryapp.entity.Station;
import com.laioffer.dispatchdeliveryapp.model.DeliveryMode;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class OrderService {

    private static final BigDecimal DRONE_BASE_FEE = new BigDecimal("2.99");
    private static final BigDecimal ROBOT_BASE_FEE = new BigDecimal("1.99");

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
        List<OrderItemWithProduct> items = orderItemRepository.findWithProductByOrderId(id);
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
        Drone drone = order.assignedDroneId() != null
                ? droneRepository.findById(order.assignedDroneId()).orElse(null)
                : null;
        GeoPoint dronePosition = drone != null
                ? droneRepository.findPositionWktById(drone.id())
                        .flatMap(GeographyUtils::parseGeoPoint).orElse(null)
                : null;
        if (!trackable) {
            // Even for delivered/completed trips, surface the last-known
            // vehicle position + battery so the timeline still has a
            // meaningful end state.
            return new OrderTrackingResponse(
                    orderId,
                    order.status(),
                    false,
                    dronePosition,
                    deliveryDestination,
                    stationPosition,
                    drone != null ? drone.droneCode() : null,
                    drone != null ? drone.status() : null,
                    drone != null ? drone.speed() : null,
                    drone != null ? drone.batteryLevel() : null,
                    drone != null && drone.vehicleMode() != null
                            ? drone.vehicleMode()
                            : DeliveryMode.fromStringOrDefault(order.deliveryMode(), DeliveryMode.DRONE).name());
        }

        if (drone == null) {
            throw new NoSuchElementException("Drone not found: " + order.assignedDroneId());
        }

        boolean inDelivery = drone.status() == DroneStatus.DELIVERY && dronePosition != null;
        String vehicleMode = drone.vehicleMode() != null ? drone.vehicleMode()
                : DeliveryMode.fromStringOrDefault(order.deliveryMode(), DeliveryMode.DRONE).name();

        return new OrderTrackingResponse(
                orderId,
                order.status(),
                inDelivery,
                dronePosition,
                deliveryDestination,
                stationPosition,
                drone.droneCode(),
                drone.status(),
                drone.speed(),
                drone.batteryLevel(),
                vehicleMode);
    }

    public List<DeliveryPlanResponse> getDeliveryPlans(OrderPlansRequest request) {
        validateOrderItemsRequest(request.longitude(), request.latitude(), request.items());
        DeliveryMode mode = DeliveryMode.fromStringOrDefault(request.deliveryMode(), DeliveryMode.DRONE);

        String deliveryWkt = deliveryWkt(request.longitude(), request.latitude());
        List<ResolvedItem> resolvedItems = resolveItems(request.items());
        BigDecimal totalAmount = computeTotal(resolvedItems);

        return stationRepository.findAll().stream()
                .map(station -> buildPlan(station, deliveryWkt, resolvedItems, totalAmount, mode))
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

        DeliveryMode mode = DeliveryMode.fromStringOrDefault(request.deliveryMode(), DeliveryMode.DRONE);

        List<ResolvedItem> resolvedItems = resolveAndValidateItemsAtStation(request.stationId(), request.items());
        BigDecimal totalAmount = computeTotal(resolvedItems);

        Drone drone = findAvailableVehicle(request.stationId(), mode);

        String orderNo = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String deliveryWkt = deliveryWkt(request.longitude(), request.latitude());

        orderRepository.insertOrder(
                orderNo,
                userId,
                request.stationId(),
                drone.id(),
                deliveryWkt,
                request.deliveryAddress(),
                mode.name(),
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

        double assignedSpeed = mode == DeliveryMode.DRONE
                ? assignmentProperties.deliverySpeed()
                : assignmentProperties.robotDeliverySpeed();
        droneRepository.assignToDelivery(
                drone.id(), DroneStatus.DELIVERY, assignedSpeed);

        // Re-fetch items with joined product info so the response payload
        // carries product_name / image_url (mirrors getOrderDetail shape).
        List<OrderItemWithProduct> itemsWithProduct = orderItemRepository.findWithProductByOrderId(order.id());
        return new OrderDetailResponse(order, itemsWithProduct);
    }

    private DeliveryPlanResponse buildPlan(
            Station station,
            String deliveryWkt,
            List<ResolvedItem> items,
            BigDecimal totalAmount,
            DeliveryMode mode) {
        double distanceKm = stationRepository.findDistanceKmToPoint(station.id(), deliveryWkt).orElse(0.0);
        int availableVehicles = countAvailableVehicles(station.id(), mode);
        String stockIssue = stockIssueAtStation(station.id(), items);
        String reason = null;
        boolean feasible = true;

        if (stockIssue != null) {
            feasible = false;
            reason = stockIssue;
        } else if (availableVehicles == 0) {
            feasible = false;
            reason = mode == DeliveryMode.ROBOT
                    ? "No available ground robot with sufficient battery"
                    : "No available drone with sufficient battery";
        }

        int etaMin = computeEtaMinutes(distanceKm, mode);
        BigDecimal deliveryFee = mode == DeliveryMode.ROBOT ? ROBOT_BASE_FEE : DRONE_BASE_FEE;

        return new DeliveryPlanResponse(
                station.id(),
                station.name(),
                station.address(),
                totalAmount,
                Math.round(distanceKm * 100.0) / 100.0,
                availableVehicles,
                feasible,
                reason,
                mode.name(),
                etaMin,
                deliveryFee.setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    private Drone findAvailableVehicle(Long stationId, DeliveryMode mode) {
        List<Drone> candidates = droneRepository.findByStationIdAndVehicleModeAndStatusAndMinBatteryLevel(
                stationId, mode.name(), DroneStatus.WAITING, assignmentProperties.minBatteryLevel());

        if (candidates.isEmpty() && mode == DeliveryMode.ROBOT) {
            throw new IllegalStateException(
                    "No available ground robot at station " + stationId + " with sufficient battery");
        }

        return candidates.stream()
                .max(Comparator.comparingInt(Drone::batteryLevel))
                .orElseThrow(() -> new IllegalStateException(
                        "No available " + (mode == DeliveryMode.ROBOT ? "ground robot" : "drone")
                                + " at station " + stationId + " with sufficient battery"));
    }

    private int countAvailableVehicles(Long stationId, DeliveryMode mode) {
        return droneRepository.findByStationIdAndVehicleModeAndStatusAndMinBatteryLevel(
                stationId, mode.name(), DroneStatus.WAITING, assignmentProperties.minBatteryLevel()).size();
    }

    private List<ResolvedItem> resolveAndValidateItemsAtStation(Long stationId, List<OrderItemRequest> items) {
        List<ResolvedItem> resolved = resolveItems(items);
        String stockIssue = stockIssueAtStation(stationId, resolved);
        if (stockIssue != null) {
            throw new IllegalArgumentException(stockIssue);
        }
        if (countAvailableVehicles(stationId, DeliveryMode.DRONE) == 0
                && countAvailableVehicles(stationId, DeliveryMode.ROBOT) == 0) {
            throw new IllegalStateException("No available vehicle at station " + stationId + " with sufficient battery");
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

    private int computeEtaMinutes(double distanceKm, DeliveryMode mode) {
        double speedMps = mode == DeliveryMode.ROBOT
                ? assignmentProperties.robotDeliverySpeed()
                : assignmentProperties.deliverySpeed();
        if (speedMps <= 0) {
            return 30;
        }
        double etaSec = (distanceKm * 1000.0) / speedMps + 180; // + 3 min handling overhead
        return Math.max(8, (int) Math.round(etaSec / 60.0));
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
