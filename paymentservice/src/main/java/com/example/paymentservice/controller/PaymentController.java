package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentRequestDto;
import com.example.paymentservice.dto.StripeWebhookDto;
import com.example.paymentservice.service.PaymentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<String> initiatePayment(@RequestBody PaymentRequestDto requestDTO) {
        try {
            String idempotencyKey = paymentService.createPendingPayment(requestDTO);
            return ResponseEntity.ok(idempotencyKey); // Return the key for client-side storage
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error initiating payment: " + e.getMessage());
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> getPaymentByKey(@PathVariable String key) {
        return paymentService.getPaymentByKey(key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<?> stripeWebhook(@RequestBody StripeWebhookDto payload) {
        boolean updated = paymentService.updatePaymentStatus(payload.getIdempotencyKey(), payload.getStatus());
        return updated ? ResponseEntity.ok("Payment status updated") : ResponseEntity.notFound().build();
    }

}
