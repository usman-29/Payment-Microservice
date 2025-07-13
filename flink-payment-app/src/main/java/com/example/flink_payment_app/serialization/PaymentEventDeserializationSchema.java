package main.java.com.example.serialization;

import com.example.flink_payment_app.model.PaymentEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

public class PaymentEventDeserializationSchema implements DeserializationSchema<PaymentEvent> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PaymentEvent deserialize(byte[] message) throws IOException {
        JsonNode root = objectMapper.readTree(message);
        JsonNode payload = root.get("payload");

        if (payload == null || payload.get("after") == null || payload.get("after").isNull()) {
            return null; // skip delete events or tombstones
        }

        JsonNode after = payload.get("after");

        String idempotencyKey = after.get("idempotency_key").asText();
        String status = after.get("status").asText();
        long createdAt = after.get("created_at").asLong(); // assuming it is stored as a timestamp (epoch millis)

        return new PaymentEvent(idempotencyKey, status, createdAt);
    }

    @Override
    public boolean isEndOfStream(PaymentEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<PaymentEvent> getProducedType() {
        return TypeInformation.of(PaymentEvent.class);
    }
}
