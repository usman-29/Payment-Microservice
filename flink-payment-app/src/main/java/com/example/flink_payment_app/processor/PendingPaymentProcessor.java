package main.java.com.example.flink_payment_app.processor;

import com.example.flink_payment_app.model.PaymentEvent;
import main.java.com.example.flink_payment_app.service.StripeService;

import javax.naming.Context;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.configuration.Configuration;
import io.github.cdimascio.dotenv.Dotenv;

public class PendingPaymentProcessor extends KeyedProcessFunction<String, PaymentEvent, String> {

    private transient ValueState<PaymentEvent> pendingState;
    private transient StripeService stripeService;

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) {
        ValueStateDescriptor<PaymentEvent> descriptor = new ValueStateDescriptor<>(
                "pendingPaymentState",
                PaymentEvent.class);
        pendingState = getRuntimeContext().getState(descriptor);
        Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                .directory("./") // root of your project
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        String stripeKey = dotenv.get("STRIPE_SECRET_KEY");
        stripeService = new StripeService(stripeKey);
    }

    @Override
    public void processElement(
            PaymentEvent event,
            Context ctx,
            Collector<String> out) throws Exception {

        if ("pending".equalsIgnoreCase(event.getStatus())) {
            // Save in state
            pendingState.update(event);

            // Register a timer 10 minutes later
            long timerTime = ctx.timestamp() + 10 * 60 * 1000L; // 10 minutes
            ctx.timerService().registerProcessingTimeTimer(timerTime);
        } else {
            // If not pending (e.g., webhook updated to success/fail), clear it
            pendingState.clear();
        }
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
        PaymentEvent pending = pendingState.value();

        if (pending != null) {
            // Call Stripe API (mock this for now)
            String stripeStatus = stripeService.checkPaymentStatus(pending.getIdempotencyKey());
            out.collect("Stripe status for " + pending.getIdempotencyKey() + ": " + stripeStatus);

            // If still pending, you may choose to retry later
            if (!"pending".equalsIgnoreCase(stripeStatus)) {
                // Build JSON string to send to Kafka
                String updateEvent = String.format(
                        "{\"idempotency_key\":\"%s\", \"payment_intent_id\":\"%s\", \"status\":\"%s\"}",
                        pending.getIdempotencyKey(),
                        pending.getPaymentIntentId(),
                        stripeStatus);

                out.collect(updateEvent); // This sends to Kafka
                pendingState.clear(); // Clean cache
            } else {
                out.collect("Stripe still shows pending for: " + pending.getIdempotencyKey());
            }
        }
    }
}
