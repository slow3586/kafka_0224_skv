package com.slow3586;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class Main {
    private static final String TOPIC0 = "Topic0";
    private static final String TOPIC1 = "Topic1";
    private static final String HOST = "kafka";
    private static final String PORT0 = HOST + ":9091";
    private static final String PORT1 = HOST + ":9092";

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Creating topic!");
        try (final Admin admin = Admin.create(Map.of(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, PORT0
        ))) {
            admin.createTopics(List.of(
                new NewTopic(TOPIC0, 1, (short) 1),
                new NewTopic(TOPIC1, 1, (short) 1)));
        }
        System.out.println("Topic created!");

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(Main::consumer);
        Thread.sleep(2000);
        executorService.execute(Main::producer);
    }

    public static void consumer() {
        System.out.println("Starting consumer!");
        try (
            final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, PORT0,
                ConsumerConfig.GROUP_ID_CONFIG, "GROUP_ID",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED.toString().toLowerCase(Locale.ROOT)
            ))
        ) {
            consumer.subscribe(List.of(TOPIC0, TOPIC1));
            System.out.println("Consumer subscribed!");
            while (true) {
                consumer.poll(Duration.ofSeconds(1))
                    .forEach(r -> System.out.println("Consumer received: " + r.key() + ":" + r.value()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void producer() {
        System.out.println("Starting producer");
        try (
            final KafkaProducer<String, String> producer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, PORT1,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.TRANSACTIONAL_ID_CONFIG, "TRANSACTIONAL_ID_CONFIG"
            ))
        ) {
            System.out.println("Initializing transactions!");
            producer.initTransactions();
            System.out.println("Starting transactions!");
            producer.beginTransaction();
            System.out.println("Transaction #1 started!");

            IntStream.range(0, 5).forEach(i -> {
                producer.send(new ProducerRecord<>(TOPIC0, "Key0", TOPIC0 + ", Transaction #1 (Committed): " + i));
                producer.send(new ProducerRecord<>(TOPIC1, "Key1", TOPIC1 + ", Transaction #1 (Committed): " + i));
            });

            producer.commitTransaction();
            System.out.println("Transaction #1 committed!");

            producer.beginTransaction();
            System.out.println("Transaction #2 started!");

            IntStream.range(0, 2).forEach(i -> {
                producer.send(new ProducerRecord<>(TOPIC0, "Key0", TOPIC0 + ", Transaction #2 (Aborted): " + i));
                producer.send(new ProducerRecord<>(TOPIC1, "Key1", TOPIC1 + ", Transaction #2 (Aborted): " + i));
            });
            producer.flush();
            System.out.println("Transaction #2 flushed!");

            producer.abortTransaction();
            producer.flush();
            System.out.println("Transaction #2 aborted!");
        }
    }

}