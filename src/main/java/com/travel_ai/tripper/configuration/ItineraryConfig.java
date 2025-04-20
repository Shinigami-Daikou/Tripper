package com.travel_ai.tripper.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@Data
public class ItineraryConfig {
    private String genAIURL;
    private String apiKey;
    private String promptPath;
    private String geminiProURL;
    private String gcpProjectID;
    private String modelID;
    private String googleMapsAPIKey;
    private String verifyPlacesPromptPath;
    private String googlePlacesURL;
    private String tmpFolder;
    private String geminiFileUrl;
    private String googleRoutesURL;
    private String distanceMatrixPromptPath;
    private String travelItineraryPromptPath;
    private String gemini2_5URL;
}