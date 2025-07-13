package com.example.flink_payment_app;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import com.example.flink_payment_app.model.PaymentEvent;
import com.example.flink_payment_app.serialization.PaymentEventDeserializationSchema;
import com.example.flink_payment_app.processor.PendingPaymentProcessor;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.Properties;

public class FlinkPaymentAppApplication {

	public static void main(String[] args) throws Exception {
		// Flink environment setup
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		// Kafka connection properties
		Properties kafkaProps = new Properties();
		kafkaProps.setProperty("bootstrap.servers", "localhost:9092"); // Internal Docker name
		kafkaProps.setProperty("group.id", "flink-cdc-consumer");

		// Set up Kafka consumer for Debezium topic
		FlinkKafkaConsumer<PaymentEvent> kafkaConsumer = new FlinkKafkaConsumer<>(
				"dbserver1.public.payments", // CDC topic from Debezium
				new PaymentEventDeserializationSchema(),
				kafkaProps);

		kafkaConsumer.assignTimestampsAndWatermarks(WatermarkStrategy.noWatermarks());

		FlinkKafkaProducer<String> producer = new FlinkKafkaProducer<>(
				"payment-status-update",
				new SimpleStringSchema(),
				kafkaProps);

		// Stream raw messages from Kafka
		env.addSource(kafkaConsumer)
				.keyBy(PaymentEvent::getIdempotencyKey)
				.process(new PendingPaymentProcessor())
				.print();

		env.execute("Flink Payment CDC Job");

		DataStream<String> updates = env
				.addSource(kafkaConsumer)
				.map(new PaymentEventDeserializer())
				.keyBy(PaymentEvent::getIdempotencyKey)
				.process(new PendingPaymentProcessor());

		updates.addSink(producer)
				.name("Send Payment Status to Kafka");
	}
}
