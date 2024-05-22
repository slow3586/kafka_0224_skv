package com.slow3586.highload_0224_skv.hw_05_kafka_streams;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

@Component
public class KafkaProducer {
    @Value("${app.topic}")
    String topicName;
    @Value("${app.producer}")
    String producerServer;

    @PostConstruct
    public void start() {
        final org.apache.kafka.clients.producer.KafkaProducer<String, String> producer =
            new org.apache.kafka.clients.producer.KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerServer,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
            ));

        final Random random = new Random();

        IntStream.range(0, 10 + random.nextInt(10)).forEach(i ->
            IntStream.range(0, 10 + random.nextInt(10)).forEach(j ->
                producer.send(new ProducerRecord<>(
                    topicName,
                    "Key" + i,
                    "Val" + j))));

        producer.flush();
        producer.close();
    }
}
