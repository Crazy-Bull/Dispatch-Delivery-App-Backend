package com.laioffer.dispatchdeliveryapp.dto;

import com.laioffer.dispatchdeliveryapp.entity.Order;
import com.laioffer.dispatchdeliveryapp.entity.OrderItem;

import java.util.List;

public record OrderDetailResponse(Order order, List<OrderItem> items) {}
