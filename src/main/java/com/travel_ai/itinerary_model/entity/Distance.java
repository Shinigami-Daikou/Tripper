package com.travel_ai.itinerary_model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.travel_ai.itinerary_model.entity.routes.RouteMatrixRes;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Distance {
    String source;
    String dest;
    double dist;
    int timeTaken;
    List<String> modeTransport;
}
