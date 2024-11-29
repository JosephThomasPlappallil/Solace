package com.solace.sentimental_analysis.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solace.sentimental_analysis.service.CustomSentimentAnalyzer;
import com.solace.sentimental_analysis.service.MockCommentService;
import com.solace.sentimental_analysis.service.PubSubPlusBroker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class SentimentAnalysisController {

    private final MockCommentService commentService;
    private final CustomSentimentAnalyzer sentimentAnalyzer;
    private final PubSubPlusBroker pubSubPlusBroker;

    public SentimentAnalysisController(MockCommentService commentService, CustomSentimentAnalyzer sentimentAnalyzer,
                                        PubSubPlusBroker pubSubPlusBroker) {
        this.commentService = commentService;
        this.sentimentAnalyzer = sentimentAnalyzer;
        this.pubSubPlusBroker = pubSubPlusBroker;
    }

    @GetMapping("/analyze-comments")
    public String analyzeComments() {
        List<Map<String, Object>> comments = commentService.fetchComments();
        StringBuilder bulkRequestBuilder = new StringBuilder();

        for (Map<String, Object> comment : comments) {
            String message = (String) comment.get("message");
            String sentiment = sentimentAnalyzer.analyzeSentiment(message);

            // Add sentiment type to the original comment data
            Map<String, Object> enrichedComment = new HashMap<>(comment);
            enrichedComment.put("sentiment_type", sentiment.toLowerCase());

            // Publish the enriched comment to Solace topic
            publishMessageToTopic(enrichedComment, sentiment.toLowerCase());

            // Add index metadata and document data to the bulk request
            try {
                bulkRequestBuilder.append("{\"index\":{\"_index\":\"demo1\"}}\n")
                        .append(new ObjectMapper().writeValueAsString(enrichedComment))
                        .append("\n");
            } catch (JsonProcessingException e) {
                log.error("Error serializing enriched comment", e);
            }
        }

        bulkRequestBuilder.append("\n");

        return bulkRequestBuilder.toString();
    }

    /**
     * Publishes messages to the appropriate Solace topic based on sentiment type.
     *
     * @param enrichedComment The message to be published.
     * @param sentimentType   The sentiment type (e.g., "positive", "negative", "neutral").
     */
    private void publishMessageToTopic(Map<String, Object> enrichedComment, String sentimentType) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Metadata JSON for Elasticsearch bulk API
            String metadata = "{ \"index\": { \"_index\": \"demo1\" } }";

            // Serialize the document (data) as JSON
            String data = objectMapper.writeValueAsString(enrichedComment);

            // Combine metadata and document, ensuring newline after each
            String payload = metadata + "\n" + data + "\n";

            // Determine the topic name based on sentiment type
            String topicName;
            if ("positive".equalsIgnoreCase(sentimentType)) {
                topicName = "twitter/response/like";
            } else if ("negative".equalsIgnoreCase(sentimentType)) {
                topicName = "twitter/response/dislike";
            } else {
                topicName = "twitter/response/neutral";
            }

            // Log the bulk payload format before publishing
            log.info("Constructed bulk payload for sentiment '{}':\n{}", sentimentType, payload);

            // Publish the payload to Solace
            pubSubPlusBroker.publishToTopic(payload, topicName);
            log.info("Message successfully published to topic {}:\nPayload:\n{}", topicName, payload);
        } catch (Exception e) {
            log.error("Error while publishing message to Solace topic. Data: {}, SentimentType: {}, Error: {}",
                    enrichedComment, sentimentType, e.getMessage());
        }
    }
}