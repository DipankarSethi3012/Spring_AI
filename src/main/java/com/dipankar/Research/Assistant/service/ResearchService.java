package com.dipankar.Research.Assistant.service;

import com.dipankar.Research.Assistant.request.ResearchRequest;
import com.dipankar.Research.Assistant.response.GeminiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Objects;

@Service
public class ResearchService {

    @Value("${gemini.api.url}")
    private String geminiApiURl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ObjectMapper  objectMapper;

    public ResearchService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }


    public String processContent(ResearchRequest request) {

        //build the prompt
        String prompt = buildPrompt(request);

        //query the api model
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        System.out.println(requestBody);

        String response = webClient.post()
                .uri(geminiApiURl + geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();


        //parse the response
        //return the response
        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(String response) {

        try {
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
            if(geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                GeminiResponse.Candidate firstCandidate = geminiResponse.getCandidates().get(0);

                if(firstCandidate.getContent() != null && firstCandidate.getContent().getParts() != null
                    && !firstCandidate.getContent().getParts().isEmpty()) {
                    return firstCandidate.getContent().getParts().get(0).getText();
                }
            }
        } catch (Exception e) {
            return "Error parsing: " + e.getMessage();
        }
        return "done";

    }

    private String buildPrompt(ResearchRequest request) {
        StringBuilder prompt = new StringBuilder();

        switch (request.getOperation()){
            case "summarize":
                prompt.append("Provide the clear and concise summary of the following text in few sentences: \n\n");
                break;
            case "suggest":
                prompt.append("Based on the following content: suggest related topics and further reading. Format the response with clear headings and bullet points: \n\n");
                break;
            default:
                throw new IllegalArgumentException("Unknown Operating");
        }

        prompt.append(request.getContent());
        return prompt.toString();
    }
}
