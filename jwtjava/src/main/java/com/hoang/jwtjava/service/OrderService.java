package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.OrderCreateRequest;
import com.hoang.jwtjava.dto.request.OrderItemRequest;
import com.hoang.jwtjava.dto.request.SellerOrderStatusUpdateRequest;
import com.hoang.jwtjava.dto.response.OrderItemResponse;
import com.hoang.jwtjava.dto.response.OrderResponse;
import com.hoang.jwtjava.dto.response.OrderStatusHistoryResponse;
import com.hoang.jwtjava.entity.FulfillmentStatus;
import com.hoang.jwtjava.entity.Order;
import com.hoang.jwtjava.entity.OrderItem;
import com.hoang.jwtjava.entity.OrderStatus;
import com.hoang.jwtjava.entity.PaymentStatus;
import com.hoang.jwtjava.entity.Product;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.repository.OrderRepository;
import com.hoang.jwtjava.repository.OrderStatusHistoryRepository;
import com.hoang.jwtjava.repository.PaymentRepository;
import com.hoang.jwtjava.repository.ProductRepository;
import com.hoang.jwtjava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRealtimeNotifier orderRealtimeNotifier;

    @Transactional
    public OrderResponse createOrder(String userEmail, OrderCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty())
            throw new AppException(ErrorCode.ORDER_ITEM_INVALID);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<OrderItem> items = new ArrayList<>();
        int subtotal = 0;
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            if (itemRequest.getQuantity() <= 0)
                throw new AppException(ErrorCode.ORDER_ITEM_INVALID);
            if (product.getStock() < itemRequest.getQuantity())
                throw new AppException(ErrorCode.OUT_OF_STOCK);

            int unitPrice = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            items.add(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(itemRequest.getQuantity())
                    .sellerEmail(product.getSellerEmail())
                    .fulfillmentStatus(FulfillmentStatus.AWAITING_CONFIRMATION)
                    .build());
            subtotal += unitPrice * itemRequest.getQuantity();

            product.setStock(product.getStock() - itemRequest.getQuantity());
        }

        int shippingFee = ShippingFeeCalculator.calculate(subtotal);
        int totalAmount = subtotal + shippingFee;

        Order order = Order.builder()
                .user(user)
                .items(items)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .receiverName(request.getReceiverName().trim())
                .receiverPhone(request.getReceiverPhone().trim())
                .shippingAddress(request.getShippingAddress().trim())
                .status(OrderStatus.PENDING_PAYMENT)
                .build();
        Order savedOrder = orderRepository.save(order);
        orderRealtimeNotifier.publishToSellers(savedOrder, OrderRealtimeNotifier.CREATED);
        return toResponse(savedOrder, null);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(order -> toResponse(order, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String userEmail, Long orderId) {
        Order order = orderRepository.findByIdAndUserEmail(orderId, userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return toResponse(order, null);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(String userEmail, Long orderId) {
        orderRepository.findByIdAndUserEmail(orderId, userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtDesc(orderId)
                .stream()
                .map(entry -> OrderStatusHistoryResponse.builder()
                        .id(entry.getId())
                        .oldStatus(entry.getOldStatus())
                        .newStatus(entry.getNewStatus())
                        .changedAt(entry.getChangedAt())
                        .changedBy(entry.getChangedBy())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getSellerOrders(String sellerEmail) {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(order -> toResponse(order, sellerEmail))
                .filter(order -> !order.getItems().isEmpty())
                .toList();
    }

    @Transactional
    public OrderResponse updateSellerFulfillmentStatus(
            String sellerEmail,
            Long orderId,
            SellerOrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderItem> sellerItems = order.getItems().stream()
                .filter(item -> sellerEmail.equalsIgnoreCase(item.getSellerEmail()))
                .toList();
        if (sellerItems.isEmpty())
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        if (order.getStatus() != OrderStatus.PAID)
            throw new AppException(ErrorCode.ORDER_FULFILLMENT_UPDATE_NOT_ALLOWED);

        FulfillmentStatus target = request.getStatus();
        boolean changed = false;
        for (OrderItem item : sellerItems) {
            FulfillmentStatus current = effectiveFulfillmentStatus(item);
            if (!isFulfillmentTransitionAllowed(current, target))
                throw new AppException(ErrorCode.ORDER_FULFILLMENT_UPDATE_NOT_ALLOWED);
            if (current != target) {
                item.setFulfillmentStatus(target);
                changed = true;
            }
        }

        if (changed) {
            Order savedOrder = orderRepository.save(order);
            orderRealtimeNotifier.publishToSeller(
                    savedOrder,
                    sellerEmail,
                    OrderRealtimeNotifier.FULFILLMENT_STATUS_CHANGED);
            return toResponse(savedOrder, sellerEmail);
        }
        return toResponse(order, sellerEmail);
    }

    @Transactional
    public Order markPaid(String userEmail, Long orderId) {
        Order order = orderRepository.findByIdAndUserEmail(orderId, userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        order.setStatus(OrderStatus.PAID);
        Order savedOrder = orderRepository.save(order);
        orderRealtimeNotifier.publishToSellers(savedOrder, OrderRealtimeNotifier.STATUS_CHANGED);
        return savedOrder;
    }

    /**
     * Hủy đơn chưa thanh toán — hoàn tồn kho và hủy payment VNPay đang PENDING.
     */
    @Transactional
    public OrderResponse cancelOrder(String userEmail, Long orderId) {
        Order order = orderRepository.findByIdAndUserEmail(orderId, userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT)
            throw new AppException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);

        restoreStock(order);
        cancelPendingPayments(order.getId());
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        orderRealtimeNotifier.publishToSellers(savedOrder, OrderRealtimeNotifier.STATUS_CHANGED);
        return toResponse(savedOrder, null);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            product.setStock(product.getStock() + item.getQuantity());
        }
    }

    private void cancelPendingPayments(Long orderId) {
        paymentRepository.findByOrder_IdAndStatus(orderId, PaymentStatus.PENDING)
                .forEach(payment -> payment.setStatus(PaymentStatus.FAILED));
    }

    private OrderResponse toResponse(Order order, String sellerEmailFilter) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .filter(item -> sellerEmailFilter == null || sellerEmailFilter.equalsIgnoreCase(item.getSellerEmail()))
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .sellerEmail(item.getSellerEmail())
                        .fulfillmentStatus(effectiveFulfillmentStatus(item).name())
                        .build())
                .toList();

        int itemsSubtotal = itemResponses.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        Integer subtotal = order.getSubtotal();
        Integer shippingFee = order.getShippingFee();
        int totalAmount;
        if (sellerEmailFilter == null) {
            if (subtotal == null)
                subtotal = itemsSubtotal;
            if (shippingFee == null)
                shippingFee = 0;
            totalAmount = order.getTotalAmount();
        } else {
            // Seller view: item totals for their lines; shipping snapshot still from order.
            subtotal = itemsSubtotal;
            if (shippingFee == null)
                shippingFee = 0;
            totalAmount = itemsSubtotal;
        }

        return OrderResponse.builder()
                .id(order.getId())
                .userEmail(order.getUser().getEmail())
                .items(itemResponses)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddress(order.getShippingAddress())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private static FulfillmentStatus effectiveFulfillmentStatus(OrderItem item) {
        return item.getFulfillmentStatus() == null
                ? FulfillmentStatus.AWAITING_CONFIRMATION
                : item.getFulfillmentStatus();
    }

    private static boolean isFulfillmentTransitionAllowed(
            FulfillmentStatus current,
            FulfillmentStatus target) {
        if (current == target)
            return true;
        return switch (current) {
            case AWAITING_CONFIRMATION -> target == FulfillmentStatus.CONFIRMED;
            case CONFIRMED -> target == FulfillmentStatus.PROCESSING;
            case PROCESSING -> target == FulfillmentStatus.SHIPPED;
            case SHIPPED -> target == FulfillmentStatus.DELIVERED;
            case DELIVERED -> false;
        };
    }
}
