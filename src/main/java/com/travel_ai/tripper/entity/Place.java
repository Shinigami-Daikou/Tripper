package com.travel_ai.tripper.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Place {
    String googlePlaceId;
    String name;
    String address;
    int timeToExplore;
    String openingTime;
    String closingTime;
    String isSun;
    String latitude;
    String longitude;
    List<List<String>> bestVisitTime;
    List<List<String>> bestTimeYear;
    double userRating;
    boolean isRecommended;
    String metadata;
}
