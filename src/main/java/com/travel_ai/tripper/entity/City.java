package com.travel_ai.tripper.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class City {
    List<Place> places;
    List<Distance> distanceMatrix;
}
