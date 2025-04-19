package com.travel_ai.itinerary_model.entity.routes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteMatrixRes {
    private int originIndex;
    private int destinationIndex;
    private int distanceMeters;
    private String duration;
    private String condition;
}
