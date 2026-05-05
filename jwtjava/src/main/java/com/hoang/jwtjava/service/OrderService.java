package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.OrderCreateRequest;
import com.hoang.jwtjava.dto.request.OrderItemRequest;
import com.hoang.jwtjava.dto.response.OrderItemResponse;
import com.hoang.jwtjava.dto.response.OrderResponse;
import com.hoang.jwtjava.entity.Order;
import com.hoang.jwtjava.entity.OrderItem;
import com.hoang.jwtjava.entity.OrderStatus;
import com.hoang.jwtjava.entity.Product;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.repository.OrderRepository;
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
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(String userEmail, OrderCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty())
            throw new AppException(ErrorCode.ORDER_ITEM_INVALID);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<OrderItem> items = new ArrayList<>();
        int total = 0;
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
                    .build());
            total += unitPrice * itemRequest.getQuantity();

            product.setStock(product.getStock() - itemRequest.getQuantity());
        }

        Order order = Order.builder()
                .user(user)
                .items(items)
                .totalAmount(total)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();
        return toResponse(orderRepository.save(order), null);
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
    public List<OrderResponse> getSellerOrders(String sellerEmail) {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(order -> toResponse(order, sellerEmail))
                .filter(order -> !order.getItems().isEmpty())
                .toList();
    }

    @Transactional
    public Order markPaid(String userEmail, Long orderId) {
        Order order = orderRepository.findByIdAndUserEmail(orderId, userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
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
                        .build())
                .toList();

        int totalAmount = itemResponses.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
        if (sellerEmailFilter == null)
            totalAmount = order.getTotalAmount();

        return OrderResponse.builder()
                .id(order.getId())
                .userEmail(order.getUser().getEmail())
                .items(itemResponses)
                .totalAmount(totalAmount)
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
