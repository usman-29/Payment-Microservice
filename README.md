# Payment Microservice

This project is a complete **event-driven payment microservice architecture** integrating:

- **Spring Boot** for handling payment requests
- **PostgreSQL** with master-slave replication
- **Stripe** for real payment processing
- **Debezium + Kafka** for Change Data Capture (CDC)
- **Apache Flink** for real-time stream processing and polling of pending payments

---

## ⚙️ Architecture (High-Level Overview)

![Screenshot 2025-07-13 235247](https://github.com/user-attachments/assets/40ceca14-20bc-4af3-8bae-361ce6d1f130)

---

## 🧠 Key Concepts

### ✅ Idempotency
- Each payment request is tracked using an `idempotency_key` to avoid duplicates.

### 🔁 Webhooks
- Stripe sends status updates to a `/webhook/stripe` endpoint.

### 🔄 CDC + Kafka
- PostgreSQL changes are streamed into Kafka via Debezium.
- Kafka topics include:
  - `dbserver1.public.payments`
  - `payment-status-update`

### ⚡ Flink
- Maintains an in-memory state of `pending` payments.
- Every 10 minutes, it polls Stripe to check status.
- It updates the DB or triggers another Kafka topic when status is resolved.

---

## 🧪 How It Works (Step by Step)

### Phase 1: Payment Initiation
- Client sends a POST request to Spring Boot to initiate a payment.
- Spring Boot:
  - Generates `idempotency_key`
  - Creates a `pending` payment in PostgreSQL
  - Returns the key to client

### Phase 2: Client Sends to Stripe
- Client sends the actual payment to **Stripe** using the key.
- Stripe processes it and calls back webhook when complete.

### Phase 3: Debezium CDC
- Any `INSERT/UPDATE` in PostgreSQL is captured by Debezium.
- Streams events into Kafka (`dbserver1.public.payments` topic).

### Phase 4: Flink Processing
- Flink consumes Kafka topic and caches `pending` payments.
- Every 10 minutes:
  - Calls Stripe API using `idempotency_key`
  - If success/failure → updates DB or publishes to `payment-status-update` Kafka topic

### Phase 5: Final Update
- Webhook or Flink triggers DB update → this goes back into Kafka → Flink removes from cache

---

## 🐘 PostgreSQL Master-Slave Replication

- Master: `payment-postgres`
- Replicas: `postgres-replica-1`, `postgres-replica-2`
- Data is replicated using streaming replication.
- NGINX used to load balance reads across replicas.

---

## 📦 Tech Stack

| Layer            | Technology                |
|------------------|---------------------------|
| API Layer        | Spring Boot               |
| Database         | PostgreSQL + Replication  |
| Messaging        | Apache Kafka              |
| CDC              | Debezium                  |
| Stream Processor | Apache Flink              |
| Payments         | Stripe                    |
| Monitoring       | Kafka UI                  |
| Proxy/Load Balancer | NGINX                  |

---

## 🛠️ Setup Instructions

### 1. Clone and Configure

```bash
git clone https://github.com/usman-29/Payment-Microservice.git
```

Update `.env` files in:
- `/` (root)
- `/paymentservice`
- `/flink-payment-app`

### 2. Start Docker Services

```bash
docker-compose up --build
```

### 3. Build Spring Boot App

```bash
cd paymentservice
./mvnw clean package
```

### 4. Create Debezium User 

```bash
 # Inside postgres container
 CREATE ROLE debezium_user WITH LOGIN PASSWORD 'dbz123';
 GRANT CONNECT ON DATABASE paymentdb TO debezium_user;
 GRANT USAGE ON SCHEMA public TO debezium_user;
 GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium_user;
 ALTER ROLE debezium_user REPLICATION;
 # After the payments table was created
 CREATE PUBLICATION payment_publication FOR TABLE payments;
 # Registerer Debezium Connector
 curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "payment-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "debezium_user",
      "database.password": "dbz123",
      "database.dbname": "paymentdb",
      "database.server.name": "payment",
      "topic.prefix": "payment",
      "table.include.list": "public.payments",
      "plugin.name": "pgoutput",
      "slot.name": "payment_slot",
      "publication.name": "payment_publication",
      "database.history.kafka.bootstrap.servers": "kafka:29092",
      "database.history.kafka.topic": "schema-changes.payment"
    }
  }'
```

### 5. Run Flink Job

```bash
cd flink-payment-app
./mvnw clean package
java -jar target/flink-payment-app-fat.jar
```

---

## 📌 Kafka Topics

| Topic Name                     | Purpose                              |
|--------------------------------|--------------------------------------|
| dbserver1.public.payments      | CDC stream of payments table         |
| payment-status-update          | Flink update to status after polling |
| __consumer_offsets             | Kafka internal                       |

---

## 🔐 Secrets

Use `.env` files to store:
- `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `STRIPE_SECRET_KEY`
- `KAFKA_BOOTSTRAP_SERVERS`

Make sure to add `.env` to `.gitignore`.

---

## 📄 License

This project is licensed under the MIT License.

---

## 🙋 Author

**Usman Saeed**  
GitHub: [usman-29](https://github.com/usman-29)

