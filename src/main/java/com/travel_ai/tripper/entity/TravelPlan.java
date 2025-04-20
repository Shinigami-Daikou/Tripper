package com.travel_ai.tripper.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record Activity(String time, String description) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record Day(int day, String date, List<Activity> activities) {}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TravelPlan {
    List<Day> itinerary;
}
