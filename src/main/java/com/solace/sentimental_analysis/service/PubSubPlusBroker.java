package com.solace.sentimental_analysis.service;

import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolConnectionFactoryImpl;

import jakarta.annotation.PostConstruct;

import javax.jms.*;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PubSubPlusBroker {
    private Connection connection;
    private Session session;

    private boolean isConnected = false;

    private static final String BROKER_URL = "tcps://mr-connection-ikm5vks8m60.messaging.solace.cloud:55443";
    private static final String USERNAME = "solace-cloud-client";
    private static final String PASSWORD = "mketc7oaji4jn6306jvtfscb0c";
    private static final String MESSAGE_VPN = "sentimentalanalysis";

    @PostConstruct
    public void connect() throws JMSException {
        log.info("Attempting to connect to Solace broker...");

        if (!isConnected) {
            try {
                log.info("Creating SolConnectionFactory with host: {} and Message VPN: {}", BROKER_URL, MESSAGE_VPN);

                SolConnectionFactory connectionFactory = new SolConnectionFactoryImpl();
                connectionFactory.setHost(BROKER_URL);
                connectionFactory.setUsername(USERNAME);
                connectionFactory.setPassword(PASSWORD);
                connectionFactory.setVPN(MESSAGE_VPN);

                log.info("Creating connection...");
                connection = connectionFactory.createConnection();

                log.info("Creating session...");
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                log.info("Starting connection...");
                connection.start();

                isConnected = true;
                log.info("Connected to Solace broker at {} with Message VPN: {}", BROKER_URL, MESSAGE_VPN);
            } catch (JMSException e) {
                log.error("Failed to establish connection. Error: {}", e.getMessage(), e);
                throw e;
            }
        } else {
            log.info("Connection to Solace broker is already established.");
        }
    }

    public void publishToTopic(String payload, String topicName) throws JMSException {
        if (!isConnected || connection == null || session == null) {
            log.error("Connection or Session is not initialized.");
            throw new java.lang.IllegalStateException("Connection or Session is not initialized.");
        }

        Topic topic = session.createTopic(topicName);
        MessageProducer producer = session.createProducer(topic);

        TextMessage message = session.createTextMessage(payload);

        try {
            log.info("Publishing message to Solace topic: {}\nPayload:\n{}", topicName, payload);

            producer.send(message);
            log.info("Message successfully published to topic: {}", topicName);
        } catch (JMSException e) {
            log.error("Failed to publish message to topic: {}. Error: {}", topicName, e.getMessage(), e);
            throw e;
        } finally {
            producer.close();
        }
    }
}