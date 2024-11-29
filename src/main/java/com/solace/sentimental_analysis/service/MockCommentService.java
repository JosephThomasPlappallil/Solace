package com.solace.sentimental_analysis.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MockCommentService {
    private static final String MOCK_COMMENTS_FILE = "mock-comments.json";

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchComments() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> data = objectMapper.readValue(new File(MOCK_COMMENTS_FILE), Map.class);
            return (List<Map<String, Object>>) data.get("data");
        } catch (IOException e) {
            throw new RuntimeException("Error reading mock comments file", e);
        }
    }
}
