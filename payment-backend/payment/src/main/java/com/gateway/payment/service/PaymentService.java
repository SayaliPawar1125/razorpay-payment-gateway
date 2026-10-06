package com.gateway.payment.service;

import com.gateway.payment.dto.CreateOrderResponse;
import com.gateway.payment.dto.VerifyPaymentRequest;
import com.gateway.payment.entity.Payment;
import com.gateway.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service


public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final RestClient restClient;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public PaymentService(
            PaymentRepository paymentRepository,
            RestClient.Builder restClientBuilder) {

        this.paymentRepository = paymentRepository;

        this.restClient = restClientBuilder
                .baseUrl("https://api.razorpay.com")
                        .build();
    }

// ============================================================
// CREATE RAZORPAY ORDER
// ============================================================

    public CreateOrderResponse createOrder(Long amountInRupees)
    {

        if (amountInRupees == null ||
                amountInRupees <= 0) {

            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }

// Convert rupees to paise
        long amountInPaise =
                amountInRupees * 100;

        String receipt =
                "receipt_" +
                        UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 20);

        String requestBody =
                """
                {
                "amount": %d,
                "currency": "INR",
                "receipt": "%s"
                }
                """.formatted(
                        amountInPaise,
                        receipt
                );

        String response = restClient
                .post()
                .uri("/v1/orders")
                .headers(headers -> {

                    headers.setBasicAuth(
                            keyId,
                            keySecret
                    );

                    headers.setContentType(
                            MediaType.APPLICATION_JSON );
                })
                .body(requestBody)
                .retrieve()
                .body(String.class);

        String razorpayOrderId =
                extractJsonValue(
                        response,
                        "id"
                );

        Payment payment = new Payment();

        payment.setRazorpayOrderId(
                razorpayOrderId
        );

        payment.setAmount(
                amountInPaise
        );

        payment.setCurrency("INR");

        payment.setStatus("CREATED");

        payment.setCreatedAt(
                LocalDateTime.now()
        );

        paymentRepository.save(payment);

        return new CreateOrderResponse(
                keyId,
                razorpayOrderId,
                amountInPaise,
                "INR"
        );
    }

// ============================================================
// VERIFY PAYMENT
// ============================================================

    public String verifyPayment(
            VerifyPaymentRequest request) {

        Payment payment =
                paymentRepository
                        .findByRazorpayOrderId(
                                request.getRazorpayOrderId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Order not found"
                                )
                        );

        String payload =
                request.getRazorpayOrderId()
                        + "|"
                        + request.getRazorpayPaymentId();

        String generatedSignature =
                generateHmacSha256(
                        payload,
                        keySecret
                );

        if (!generatedSignature.equals(
                request.getRazorpaySignature())) {

            payment.setStatus("FAILED");

            paymentRepository.save(payment);

            throw new RuntimeException(
                    "Payment signature verification failed"
            );
        }

        payment.setRazorpayPaymentId(
                request.getRazorpayPaymentId()
        );

        payment.setRazorpaySignature(
                request.getRazorpaySignature()
        );

        payment.setStatus("SUCCESS");

        paymentRepository.save(payment);

        return "Payment verified successfully";
    }

// ============================================================
// GET PAYMENT
// ============================================================

    public Payment getPayment(Long id) {

        return paymentRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Payment not found"
                        )
                );
    }

// ============================================================
// HMAC SHA256
// ============================================================

    private String generateHmacSha256(
            String data,
            String secret) {

        try {

            Mac mac =
                    Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            secret.getBytes(
                                    StandardCharsets.UTF_8 ),
                            "HmacSHA256"
                    );

            mac.init(secretKey);

            byte[] hash =
                    mac.doFinal(
                            data.getBytes(
                                    StandardCharsets.UTF_8 )
                    );

            StringBuilder hex =
                    new StringBuilder();

            for (byte b : hash) {

                hex.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return hex.toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to generate signature",
                    e
            );
        }
    }

// ============================================================
// SIMPLE JSON ID EXTRACTION
// ============================================================

    private String extractJsonValue(
            String json,
            String key) {

        String search =
                "\"" + key + "\"";

        int keyPosition =
                json.indexOf(search);

        if (keyPosition == -1) {

            throw new RuntimeException(
                    "Razorpay response did not contain "
                            + key
            );
        }

        int colonPosition =
                json.indexOf(
                        ":",
                        keyPosition
                );

        int firstQuote =
                json.indexOf(
                        "\"",
                        colonPosition
                );

        int secondQuote =
                json.indexOf(
                        "\"",
                        firstQuote + 1
                );

        return json.substring(
                firstQuote + 1,
                secondQuote
        );
    }
}