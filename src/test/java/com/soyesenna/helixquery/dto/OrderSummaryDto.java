package com.soyesenna.helixquery.dto;

import java.math.BigDecimal;

/**
 * DTO for Order summary projection tests.
 */
public class OrderSummaryDto {

    private final String orderNumber;
    private final BigDecimal totalAmount;
    private final String userName;

    public OrderSummaryDto(String orderNumber, BigDecimal totalAmount, String userName) {
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.userName = userName;
    }

    public String getOrderNumber() { return orderNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getUserName() { return userName; }

    @Override
    public String toString() {
        return "OrderSummaryDto{orderNumber='" + orderNumber + "', totalAmount=" + totalAmount +
               ", userName='" + userName + "'}";
    }
}
