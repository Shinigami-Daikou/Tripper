package com.travel_ai.tripper.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
