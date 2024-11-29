package com.solace.sentimental_analysis.service;

import org.springframework.stereotype.Service;

import com.solace.sentimental_analysis.dto.SentimentWords;

@Service
public class CustomSentimentAnalyzer {

    public String analyzeSentiment(String message) {
        String[] words = message.toLowerCase().split("\\W+"); // Split into words, ignoring punctuation

        int score = 0;

        for (String word : words) {
            if (SentimentWords.POSITIVE_WORDS.contains(word)) {
                score++; // Increment score for positive words
            } else if (SentimentWords.NEGATIVE_WORDS.contains(word)) {
                score--; // Decrement score for negative words
            }
        }

        // Determine sentiment based on score
        if (score > 0) {
            return "Positive";
        } else if (score < 0) {
            return "Negative";
        } else {
            return "Neutral";
        }
    }
}
