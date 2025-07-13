package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequestDto;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public String createPendingPayment(PaymentRequestDto requestDTO) {
        Payment payment = new Payment();
        payment.setIdempotencyKey(UUID.randomUUID().toString());
        payment.setAmount(requestDTO.getAmount());
        payment.setStatus("pending");

        paymentRepository.save(payment);

        return payment.getIdempotencyKey();
    }

    public Optional<Payment> getPaymentByKey(String key) {
        return paymentRepository.findByIdempotencyKey(key);
    }

    public boolean updatePaymentStatus(String key, String status) {
        Optional<Payment> paymentOpt = paymentRepository.findByIdempotencyKey(key);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(status.toLowerCase());
            paymentRepository.save(payment);
            return true;
        }
        return false;
    }

}
