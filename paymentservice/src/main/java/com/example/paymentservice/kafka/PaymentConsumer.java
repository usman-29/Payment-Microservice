package com.example.paymentservice.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {

    @KafkaListener(topics = "payment.public.payments", groupId = "payment-group")
    public void consume(String message) {
        System.out.println("🟢 CDC Event received:");
        System.out.println(message);
    }
}
