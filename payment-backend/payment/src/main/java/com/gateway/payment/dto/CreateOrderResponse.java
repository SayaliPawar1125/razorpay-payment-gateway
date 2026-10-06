package com.gateway.payment.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResponse {
    private String keyId;

    private  String orderId;

    private  Long amount;

    private String currency;
}
