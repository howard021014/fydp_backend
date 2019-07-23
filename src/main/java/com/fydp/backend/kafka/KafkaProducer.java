package com.fydp.backend.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducer {

    @Value(value = "${brevity.kafka.egress.topic}")
    private String topicName;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String msg) {
        kafkaTemplate.send(topicName, msg);
    }

    public void sendMessageWithKey(String msg, String key) {
        kafkaTemplate.send(topicName, key, msg);
    }

    public KafkaProducer(){

    }

}
