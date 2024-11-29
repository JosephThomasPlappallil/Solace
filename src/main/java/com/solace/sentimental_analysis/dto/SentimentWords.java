package com.solace.sentimental_analysis.dto;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SentimentWords {

    // List of positive words
    public static final Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "love", "amazing", "excellent", "enjoyed", "great", "fantastic", "happy",
            "insights", "valuable", "thank", "useful", "awesome", "inspiring", "brilliant",
            "top-notch", "better", "structured", "well-structured"));

    // List of negative words
    public static final Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
            "terrible", "bad", "hate", "wasted", "awful", "poor", "sad", "angry",
            "disappointed", "time-wasting", "lacks", "depth", "confusing", "frustrating",
            "worse", "unclear", "incomplete", "poorly"));

}
