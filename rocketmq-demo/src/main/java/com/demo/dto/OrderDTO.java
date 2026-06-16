package com.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.io.Serializable;

@Data
public class OrderDTO implements Serializable {
    private String orderId;
    private String userId;
    private String productName;
    private Integer quantity;
    private BigDecimal amount;
    private String status;  // CREATED, PAID, SHIPPED, COMPLETED
}
