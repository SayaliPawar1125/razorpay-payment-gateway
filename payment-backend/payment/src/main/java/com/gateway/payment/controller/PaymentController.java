package com.gateway.payment.controller;

import com.gateway.payment.dto.CreateOrderRequest;
import com.gateway.payment.dto.CreateOrderResponse;
import com.gateway.payment.dto.VerifyPaymentRequest;
import com.gateway.payment.entity.Payment;
import com.gateway.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

// ============================================================
// CREATE ORDER
// ============================================================

    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        CreateOrderResponse response = paymentService.createOrder(
                request.getAmount()
        );
        return ResponseEntity.ok(response);
    }

// ============================================================
// VERIFY PAYMENT
// ============================================================

    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {

        String result = paymentService.verifyPayment(request);
        return ResponseEntity.ok(result);
    }

// ============================================================
// GET PAYMENT
// ============================================================

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                paymentService.getPayment(id)
        );
    }
}