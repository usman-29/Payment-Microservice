package main.java.com.example.flink_payment_app.model;

public class PaymentEvent {
    private String idempotencyKey;
    private String status;
    private long createdAt; // timestamp in millis (or use Instant/LocalDateTime if preferred)

    public PaymentEvent() {
    }

    public PaymentEvent(String idempotencyKey, String status, long createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "idempotencyKey='" + idempotencyKey + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
