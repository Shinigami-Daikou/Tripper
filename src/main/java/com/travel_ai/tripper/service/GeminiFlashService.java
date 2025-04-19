package com.travel_ai.tripper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel_ai.tripper.configuration.ItineraryConfig;
import com.travel_ai.tripper.entity.AIOutput;
import com.travel_ai.tripper.entity.City;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Request.Builder;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class GeminiFlashService {
    private static final Logger logger = Logger.getLogger(GeminiFlashService.class);
    private final OkHttpClient client;

    @Autowired
    ItineraryConfig itineraryConfig;

    public GeminiFlashService() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .callTimeout(90, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS) // Add connect timeout
                .readTimeout(90, TimeUnit.SECONDS)    // Add read timeout
                .writeTimeout(90, TimeUnit.SECONDS);
        this.client = builder.build();
    }

    public City generateTravelPlan(String destination, int nLocations) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode prompt = null;
        try {
            Path promptFilePath = Paths.get(itineraryConfig.getPromptPath());
            String promptString = Files.readString(promptFilePath, StandardCharsets.UTF_8);
            String formattedQueryParam = String.format(promptString, destination, nLocations, nLocations/2);
            prompt = objectMapper.readValue(formattedQueryParam, JsonNode.class);
        } catch (IOException e) {
            logger.error("Unable to read prompt", e.getStackTrace());
        }

        Request request = new Builder().
                url(itineraryConfig.getGenAIURL() + itineraryConfig.getApiKey()).
                header("Content-Type", "application/json").
                post(RequestBody.create(prompt.toString().getBytes(StandardCharsets.UTF_8))).build();

        City cityData = null;
        try {
            Response response = client.newCall(request).execute();
            try (InputStream stream = response.body().byteStream()) {
                AIOutput aiOut = objectMapper.readValue(stream, AIOutput.class);
                String responseStr = aiOut.getText();
                responseStr = responseStr.substring(responseStr.indexOf('{'));
                responseStr = responseStr.substring(0, responseStr.lastIndexOf('}') + 1);

                try {
                    cityData = objectMapper.readValue(responseStr, City.class);
                } catch (JsonProcessingException e) {
                    logger.error("Mapping to CityData failed", e.getStackTrace());
                }
            }
        } catch (IOException e) {
            logger.error("Error while reading response from Gemini", e.getStackTrace());
        }
        return cityData;
    }
}
