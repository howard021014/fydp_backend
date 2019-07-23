package com.fydp.backend.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);

    private CountDownLatch latch;

    public List<String> messages;

    public void setMessages(int numMessages){
        latch = new CountDownLatch(numMessages);
        messages = new ArrayList<>();
    }

    @KafkaListener(topics = "brevity_responses", groupId = KafkaConsumerConfig.groupId)
    public void listen(String message){
        logger.info("Received message : \n"  + message);
        messages.add(message);
        latch.countDown();
    }

    public List<String> getMessages() {
        return messages;
    }

    public CountDownLatch getLatch(){
        return latch;
    }

}
