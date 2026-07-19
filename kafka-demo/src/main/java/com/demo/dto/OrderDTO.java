package com.demo.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderDTO implements Serializable {
    private String orderId;
    private String userId;
    private String productName;
    private Integer quantity;
    private BigDecimal amount;
    private String status;  // CREATED, PAID, SHIPPED, COMPLETED
}
