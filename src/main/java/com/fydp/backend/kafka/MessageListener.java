package com.fydp.backend.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);

    private CountDownLatch latch;

    public Map<String, String> messages;

    public void setMessages(int numMessages){
        latch = new CountDownLatch(numMessages);
        messages = new HashMap<>();
    }

    @KafkaListener(topics = "brevity_responses", groupId = KafkaConsumerConfig.groupId)
    public void listen(ConsumerRecord<String, String> record){

        logger.info("Received message : \n"  + record.key() + " : " + record.value());
        messages.put(record.key(), record.value());
        latch.countDown();
    }

    public Map<String, String> getMessages() {
        return messages;
    }

    public CountDownLatch getLatch(){
        return latch;
    }

}
