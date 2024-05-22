package com.slow3586.highload_0224_skv.hw_05_kafka_streams;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.SessionWindows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;

@Component
@Slf4j
public class KafkaConsumer {
    @Value("${app.topic}")
    String topicName;
    @Value("${app.consumer}")
    String consumerServer;

    @PostConstruct
    public void start() {
        final Properties properties = new Properties();
        properties.putAll(Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerServer,
            ConsumerConfig.GROUP_ID_CONFIG, "GROUP_ID",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            StreamsConfig.APPLICATION_ID_CONFIG, "APP"
        ));

        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(topicName, Consumed.with(Serdes.String(), Serdes.String()))
            .mapValues(v -> v.toUpperCase())
            .groupByKey()
            .windowedBy(SessionWindows.ofInactivityGapWithNoGrace(Duration.ofMinutes(5)))
            .count()
            .toStream()
            .peek((key, value) -> log.info(
                "Session finished: time:{}-{}, key:{}, received:{} times",
                DateTimeFormatter.ISO_ZONED_DATE_TIME
                    .withZone(ZoneId.systemDefault())
                    .format(key.window().startTime()),
                DateTimeFormatter.ISO_ZONED_DATE_TIME
                    .withZone(ZoneId.systemDefault())
                    .format(key.window().endTime()),
                key.key(),
                value));

        new KafkaStreams(builder.build(), properties).start();
    }
}
