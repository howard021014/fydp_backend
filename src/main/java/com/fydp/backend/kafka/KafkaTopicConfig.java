package com.fydp.backend.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value(value = "${brevity.kafka.bootstrapAddress}")
    private String bootstrapAddress;

    @Value(value = "${brevity.kafka.egress.topic}")
    private String egressTopicName;

    @Value(value="${brevity.kafka.ingress.topic}")
    private String ingressTopicName;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic requestTopic() {
        return new NewTopic(egressTopicName, 1, (short) 1);
    }

    @Bean
    public NewTopic responseTopic() {
        return new NewTopic(ingressTopicName, 1, (short) 1);
    }


}
