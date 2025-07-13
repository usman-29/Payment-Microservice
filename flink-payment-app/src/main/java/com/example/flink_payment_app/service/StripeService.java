package main.java.com.example.flink_payment_app.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

public class StripeService {

    public StripeService(String secretKey) {
        Stripe.apiKey = secretKey;
    }

    public String checkPaymentStatus(String idempotencyKey) {
        try {
            // You’ll likely need a mapping between idempotency key ↔ payment intent ID
            // For test, pretend idempotencyKey is paymentIntentId directly
            PaymentIntent paymentIntent = PaymentIntent.retrieve(idempotencyKey);
            return paymentIntent.getStatus(); // e.g., "succeeded", "processing", "requires_payment_method"
        } catch (StripeException e) {
            System.err.println("Stripe error: " + e.getMessage());
            return "not_found";
        }
    }
}
