package com.solace.sentimental_analysis.service;

import com.solacesystems.jms.SolConnectionFactory;
import com.solacesystems.jms.SolConnectionFactoryImpl;

import jakarta.annotation.PostConstruct;

import javax.jms.*;

import java.util.function.BiConsumer;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PubSubPlusBroker {
	private Connection connection;
	private Session session;

	// Flag to track whether the connection is established
	private boolean isConnected = false;

	// Hardcoded connection details
	private static final String BROKER_URL = "tcps://mr-connection-ikm5vks8m60.messaging.solace.cloud:55443";
	private static final String USERNAME = "solace-cloud-client";
	private static final String PASSWORD = "mketc7oaji4jn6306jvtfscb0c";
	private static final String MESSAGE_VPN = "sentimentalanalysis"; // Message VPN
	private static final String PUBLISH_TOPIC_LIKE = "twiiter/response/like";
	private static final String PUBLISH_TOPIC_DISLIKE = "twitter/response/dislike";
	private static final String PUBLISH_TOPIC_NEUTRAL = "twitter/response/neutral";

	// Constructor
	public PubSubPlusBroker() {
		// Constructor logic can remain empty as values are hardcoded
	}

	// Automatically call connect when the bean is initialized
	@PostConstruct
	public void connect() throws JMSException {
		log.info("Attempting to connect to Solace broker...");

		if (!isConnected) {
			try {
				// Log before creating the connection factory
				log.info("Creating SolConnectionFactory with host: {} and Message VPN: {}", BROKER_URL, MESSAGE_VPN);

				// Use SolConnectionFactoryImpl to create a connection factory
				SolConnectionFactory connectionFactory = new SolConnectionFactoryImpl();
				connectionFactory.setHost(BROKER_URL);
				connectionFactory.setUsername(USERNAME);
				connectionFactory.setPassword(PASSWORD);
				connectionFactory.setVPN(MESSAGE_VPN); // Set the Message VPN

				log.info("Creating connection...");
				connection = connectionFactory.createConnection();

				log.info("Creating session...");
				session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

				log.info("Starting connection...");
				connection.start();

				// Log the connection status
				isConnected = true; // Set connection status to true once it's established
				log.info("Connected to Solace broker at {} with Message VPN: {}", BROKER_URL, MESSAGE_VPN);
			} catch (JMSException e) {
				// Log the exception if connection setup fails
				log.error("Failed to establish connection. Error: {}", e.getMessage(), e);
				throw e; // Rethrow the exception after logging
			}
		} else {
			log.info("Connection to Solace broker is already established.");
		}
	}

	// Method to publish messages
	public void publish(String metadata, String data, String queueName) throws JMSException {
		// Ensure connection and session are initialized
		if (!isConnected || connection == null || session == null) {
			log.error("Connection or Session is not initialized.");
			throw new java.lang.IllegalStateException("Connection or Session is not initialized."); // Explicitly
																									// specify the
																									// java.lang package
		}

		Queue queue = session.createQueue(queueName);
		MessageProducer producer = session.createProducer(queue);

		// Construct the bulk payload
		String payload = metadata + "\n" + data + "\n";

		TextMessage message = session.createTextMessage(payload);

		try {
			// Log the payload being sent
			log.info("Publishing message to Solace queue: {}\nPayload:\n{}", queueName, payload);

			// Send the message
			producer.send(message);
			log.info("Message successfully published to queue: {}", queueName);
		} catch (JMSException e) {
			log.error("Failed to publish message to queue: {}. Error: {}", queueName, e.getMessage(), e);
			throw e;
		} finally {
			producer.close();
		}
	}

	// Method to determine the correct topic based on sentiment type
	private String getTopicNameBasedOnSentiment(String sentimentType) {
		log.info("Determining topic for sentiment: {}", sentimentType);
		if ("positive".equalsIgnoreCase(sentimentType)) {
			log.info("Topic selected: {}", PUBLISH_TOPIC_LIKE);
			return PUBLISH_TOPIC_LIKE;
		} else if ("negative".equalsIgnoreCase(sentimentType)) {
			log.info("Topic selected: {}", PUBLISH_TOPIC_DISLIKE);
			return PUBLISH_TOPIC_DISLIKE;
		} else if ("neutral".equalsIgnoreCase(sentimentType)) {
			log.info("Topic selected: {}", PUBLISH_TOPIC_NEUTRAL);
			return PUBLISH_TOPIC_NEUTRAL;
		}
		log.warn("Invalid sentiment type received: {}", sentimentType);
		return null;
	}

	// Method to subscribe to a topic
	public void subscribe(String topicName) throws JMSException {
		// Ensure the session is initialized
		if (session == null) {
			log.error("Session is not initialized. Please connect first.");
			return;
		}

		Topic topic = session.createTopic(topicName);
		MessageConsumer consumer = session.createConsumer(topic);
		consumer.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				try {
					TextMessage textMessage = (TextMessage) message;
					log.info("Received message: {}", textMessage.getText());
				} catch (JMSException e) {
					log.error("Error processing message", e);
				}
			}
		});
	}

}