package com.travel_ai.tripper.controller;

import com.travel_ai.tripper.configuration.ItineraryConfig;
import com.travel_ai.tripper.entity.*;
import com.travel_ai.tripper.service.Gemini2_5Service;
import com.travel_ai.tripper.service.GeminiFlashService;
import com.travel_ai.tripper.service.GeminiProService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ItineraryController {

    @Autowired
    GeminiFlashService geminiService;

    @Autowired
    GeminiProService geminiProService;

    @Autowired
    Gemini2_5Service gemini2_5Service;

    @Autowired
    ItineraryConfig itineraryConfig;

    @PostMapping("/travel-plan")
    public City travelPlan(@RequestBody Map<String, Object> body){
        return geminiService.generateTravelPlan((String) body.get("destination"), (Integer) body.get("nLocations"));
    }

    @PatchMapping("/travel-plan")
    public List<Place> verifyPlaces(@RequestBody List<Place> places){
        return geminiProService.verifyPlaces(places);
    }

    @PostMapping("/distance-matrix")
    public List<Distance> distanceMatrix(@RequestBody List<Place> places){
        return geminiProService.generateDistanceMatrix(places);
    }

    @PostMapping("/itinerary")
    public TravelPlan createItinerary(@RequestBody Query query){
        return gemini2_5Service.generateItinerary(query);
    }

}
