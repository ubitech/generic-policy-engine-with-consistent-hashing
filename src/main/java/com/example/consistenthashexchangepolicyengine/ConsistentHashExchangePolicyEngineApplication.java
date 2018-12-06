package com.example.consistenthashexchangepolicyengine;

import com.example.consistenthashexchangepolicyengine.messaging.PolicyListener;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ConsistentHashExchangePolicyEngineApplication {

    public static final String EXCHANGE = "e3";
    private static final String EXCHANGE_TYPE = "x-consistent-hash";


    public static void main(String[] args) {
        SpringApplication.run(ConsistentHashExchangePolicyEngineApplication.class, args);
    }

    @Bean
    public Queue autoDeleteQueue() {
        return new AnonymousQueue();
    }

    @Qualifier("policylistenerAdapter")
    @Bean
    MessageListenerAdapter policylistenerAdapter(PolicyListener receiver) {
        MessageListenerAdapter msgadapter = new MessageListenerAdapter(receiver, "policyMessageReceived");
        return msgadapter;
    }

    @Qualifier("policycontainer")
    @Bean
    SimpleMessageListenerContainer policycontainer(ConnectionFactory connectionFactory,
            @Qualifier("policylistenerAdapter") MessageListenerAdapter listenerAdapter) throws IOException {

        Connection conn = connectionFactory.createConnection();
        Channel ch = conn.createChannel(true);

        ch.queueDeclare(autoDeleteQueue().getName(), true, true, true, null);
        ch.queuePurge(autoDeleteQueue().getName());

        Map<String, Object> args = new HashMap<>();
        args.put("hash-property", "message_id");
        ch.exchangeDeclare(EXCHANGE, EXCHANGE_TYPE, false, false, args);

        ch.queueBind(autoDeleteQueue().getName(), EXCHANGE, "20");

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(autoDeleteQueue().getName());
        container.setMessageListener(listenerAdapter);
        return container;
    }
    
    
    //DONT TOUCH IT WORKS
//    @Bean
//    public Queue autoDeleteQueue1() {
//        return new AnonymousQueue();
//    }
//
//    @Qualifier("testlistenerAdapter")
//    @Bean
//    MessageListenerAdapter testlistenerAdapter(TestListener receiver) {
//        MessageListenerAdapter msgadapter = new MessageListenerAdapter(receiver, "testMessageReceived");
//        return msgadapter;
//    }
//
//    @Qualifier("testcontainer")
//    @Bean
//    SimpleMessageListenerContainer testcontainer(ConnectionFactory connectionFactory,
//            @Qualifier("testlistenerAdapter") MessageListenerAdapter listenerAdapter) throws IOException {
//
//        Connection conn = connectionFactory.createConnection();
//        Channel ch = conn.createChannel(true);
//
//        //ch.queueDeclare(autoDeleteQueue1().getName(), true, false, false, null);
//        ch.queueDeclare(autoDeleteQueue1().getName(), true, true, true, null);
//        ch.queuePurge(autoDeleteQueue1().getName());
//
//        Map<String, Object> args = new HashMap<>();
//        args.put("hash-property", "message_id");
//        //ch.exchangeDeclare(EXCHANGE, EXCHANGE_TYPE, true, false, args);
//        ch.exchangeDeclare(EXCHANGE, EXCHANGE_TYPE, false, false, args);
//
//        //ch.queueBind(autoDeleteQueue1().getName(), EXCHANGE, "1");
//        ch.queueBind(autoDeleteQueue1().getName(), EXCHANGE, "20");
//
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//        container.setQueueNames(autoDeleteQueue1().getName());
//        container.setMessageListener(listenerAdapter);
//        return container;
//    }
    

}
