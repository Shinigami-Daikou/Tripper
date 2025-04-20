package com.travel_ai.tripper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel_ai.tripper.configuration.ItineraryConfig;
import com.travel_ai.tripper.entity.AIOutput;
import com.travel_ai.tripper.entity.Distance;
import com.travel_ai.tripper.entity.Place;
import com.travel_ai.tripper.util.GeminiFileUtil;
import com.travel_ai.tripper.util.GoogleMapsUtil;
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
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class GeminiProService {
    @Autowired
    ItineraryConfig itineraryConfig;

    @Autowired
    GoogleMapsUtil gmapsUtil;

    @Autowired
    GeminiFileUtil geminiFileUtil;

    private final OkHttpClient client;
    private static final Logger logger = Logger.getLogger(GeminiProService.class);

    public GeminiProService() {
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

    public List<Place> verifyPlaces(List<Place> places) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String placesJsonString = null, verifyPlacesJsonString = null;
        try {
            placesJsonString = mapper.writeValueAsString(places);
            writeToTmpFile(placesJsonString, "places.txt");
            List<JsonNode> googleMapFetchPlaces = gmapsUtil.fetchPlacesDetailsByName(places);
            verifyPlacesJsonString = mapper.writeValueAsString(googleMapFetchPlaces);
            writeToTmpFile(verifyPlacesJsonString, "verifyPlaces.txt");
        } catch (JsonProcessingException e) {
            logger.error("Error during transforming input places list to string.", e);
        }

        String placesFileUrl = geminiFileUtil.uploadFile("places.txt");
        String verifyPlacesFileURL = geminiFileUtil.uploadFile("verifyPlaces.txt");

        System.out.println(placesFileUrl);
        System.out.println(verifyPlacesFileURL);

        JsonNode prompt = null;
        String formattedQueryParam = null;
        try {
            Path promptFilePath = Paths.get(itineraryConfig.getVerifyPlacesPromptPath());
            String promptString = Files.readString(promptFilePath, StandardCharsets.UTF_8);
            formattedQueryParam = String.format(promptString, placesFileUrl, verifyPlacesFileURL);
            prompt = mapper.readValue(formattedQueryParam, JsonNode.class);
        } catch (IOException e) {
            logger.error("Unable to read prompt", e);
        }

        Request request = new Request.Builder().
                url(itineraryConfig.getGeminiProURL() + itineraryConfig.getModelID() + ":generateContent?key=" +
                        itineraryConfig.getApiKey()).
                header("Content-Type", "application/json").
                post(RequestBody.create(prompt.toString().getBytes(StandardCharsets.UTF_8))).build();

        List<Place> verifiedPlace = null;
        try {
            Response response = client.newCall(request).execute();
            try (InputStream stream = response.body().byteStream()) {
                AIOutput aiOut = mapper.readValue(stream, AIOutput.class);
                String responseStr = aiOut.getText();
                responseStr = responseStr.substring(responseStr.indexOf('['));
                responseStr = responseStr.substring(0, responseStr.lastIndexOf(']') + 1);
                System.out.println(responseStr);
                try {
                    verifiedPlace = mapper.readValue(responseStr, new TypeReference<List<Place>>() {});
                } catch (JsonProcessingException e) {
                    logger.error("Mapping to CityData failed", e.getStackTrace());
                }
            }
        } catch (IOException e) {
            logger.error("Error while reading response from Gemini", e.getStackTrace());
        }

        return verifiedPlace;
    }

    public List<Distance> generateDistanceMatrix(List<Place> places) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        HashMap<String, String> placeIDMap = new HashMap<>();
        List<String> placeIDList = new ArrayList<>();

        for(Place place: places) {
            placeIDMap.put(place.getGooglePlaceId(), place.getName());
            placeIDList.add(place.getGooglePlaceId());
        }

        String placesString =placeIDMap.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(", "));


        try {
            List<Distance> distancesMatrix = gmapsUtil.getRoutesMatrix(placeIDMap, placeIDList);
            String distanceMatrix = mapper.writeValueAsString(distancesMatrix);
            writeToTmpFile(distanceMatrix, "distance_matrix.txt");
        } catch (JsonProcessingException e) {
            logger.error("Error: Distance matrix cannot be converted to String", e);
        }

        String distanceMatrixFileURL = geminiFileUtil.uploadFile("distance_matrix.txt");
        System.out.println(distanceMatrixFileURL);

        JsonNode prompt = null;
        String formattedQueryParam = null;
        try {
            Path promptFilePath = Paths.get(itineraryConfig.getDistanceMatrixPromptPath());
            String promptString = Files.readString(promptFilePath, StandardCharsets.UTF_8);
            formattedQueryParam = String.format(promptString, distanceMatrixFileURL, placesString);
            prompt = mapper.readValue(formattedQueryParam, JsonNode.class);
        } catch (IOException e) {
            logger.error("Unable to read prompt", e);
        }

        Request request = new Request.Builder().
                url(itineraryConfig.getGeminiProURL() + itineraryConfig.getModelID() + ":generateContent?key=" +
                        itineraryConfig.getApiKey()).
                header("Content-Type", "application/json").
                post(RequestBody.create(prompt.toString().getBytes(StandardCharsets.UTF_8))).build();

        List<Distance> distanceMatrix = null;
        try {
            Response response = client.newCall(request).execute();
            try (InputStream stream = response.body().byteStream()) {
                AIOutput aiOut = mapper.readValue(stream, AIOutput.class);
                String responseStr = aiOut.getText();
                responseStr = responseStr.substring(responseStr.indexOf('['));
                responseStr = responseStr.substring(0, responseStr.lastIndexOf(']') + 1);
                System.out.println(responseStr);
                try {
                    distanceMatrix = mapper.readValue(responseStr, new TypeReference<List<Distance>>() {});
                } catch (JsonProcessingException e) {
                    logger.error("Mapping to Distance Matrix failed", e.getStackTrace());
                }
            }
        } catch (IOException e) {
            logger.error("Error while reading response from Gemini", e.getStackTrace());
        }

        return distanceMatrix;
    }
}
