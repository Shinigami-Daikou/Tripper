package com.travel_ai.tripper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel_ai.tripper.configuration.ItineraryConfig;
import com.travel_ai.tripper.entity.AIOutput;
import com.travel_ai.tripper.entity.Query;
import com.travel_ai.tripper.entity.TravelPlan;
import com.travel_ai.tripper.util.GeminiFileUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class Gemini2_5Service {
    @Autowired
    ItineraryConfig itineraryConfig;

    @Autowired
    GeminiFileUtil geminiFileUtil;

    private final OkHttpClient client;
    private static final Logger logger = Logger.getLogger(Gemini2_5Service.class);

    public Gemini2_5Service() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .callTimeout(180, TimeUnit.SECONDS);
        this.client = builder.build();
    }

    private void writeToTmpFile(String verifyPlacesJsonString, String fileName) {
        Path tmpFolder = Paths.get(itineraryConfig.getTmpFolder());
        if(Files.notExists(tmpFolder)) {
            try {
                Files.createDirectories(tmpFolder);
            } catch (IOException e) {
                logger.error("Error while creating tmp directory.", e);
            }
        }

        Path verifyPlacesFile = Paths.get(itineraryConfig.getTmpFolder() + fileName);
        try {
            Files.writeString(verifyPlacesFile, verifyPlacesJsonString, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            logger.error("Error while writing Google API output to tmp file.", e);
        }
    }


    public TravelPlan generateItinerary(Query query) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String placeToVisit = query.getPlaces().stream()
                .collect(Collectors.joining(", "));

        try {
            String cityData = mapper.writeValueAsString(query.getCity());
            writeToTmpFile(cityData, "city_data.txt");
        } catch (JsonProcessingException e) {
            logger.error("Error: Unable to write City data to file.", e);
        }

        String cityDataFileURL = geminiFileUtil.uploadFile("city_data.txt");
        System.out.println(cityDataFileURL);

        JsonNode prompt = null;
        String formattedQueryParam = null;
        try {
            Path promptFilePath = Paths.get(itineraryConfig.getTravelItineraryPromptPath());
            String promptString = Files.readString(promptFilePath, StandardCharsets.UTF_8);
            formattedQueryParam = String.format(promptString, cityDataFileURL,
                    query.getCityName(), query.getStartTime(), query.getEndTime(), query.getPlaces());
            prompt = mapper.readValue(formattedQueryParam, JsonNode.class);
        } catch (IOException e) {
            logger.error("Unable to read prompt", e);
        }

        Request request = new Request.Builder().
                url(itineraryConfig.getGemini2_5URL() + itineraryConfig.getApiKey()).
                header("Content-Type", "application/json").
                post(RequestBody.create(prompt.toString().getBytes(StandardCharsets.UTF_8))).build();

        TravelPlan travelPlan = null;
        try {
            Response response = client.newCall(request).execute();
            try (InputStream stream = response.body().byteStream()) {
//                System.out.println(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                AIOutput aiOut = mapper.readValue(stream, AIOutput.class);
                String responseStr = aiOut.getText();
//                System.out.println(responseStr);
                responseStr = responseStr.substring(responseStr.indexOf('{'));
                responseStr = responseStr.substring(0, responseStr.lastIndexOf('}') + 1);
                try {
                    travelPlan = mapper.readValue(responseStr, TravelPlan.class);
                } catch (JsonProcessingException e) {
                    logger.error("Mapping to Travel Plan failed", e.getStackTrace());
                }
            }
        } catch (IOException e) {
            logger.error("Error while reading response from Gemini", e.getStackTrace());
        }


        return travelPlan;
    }
}
