package com.travel_ai.itinerary_model.controller;

import com.travel_ai.itinerary_model.configuration.ItineraryConfig;
import com.travel_ai.itinerary_model.entity.City;
import com.travel_ai.itinerary_model.entity.Distance;
import com.travel_ai.itinerary_model.entity.Place;
import com.travel_ai.itinerary_model.service.GeminiFlashService;
import com.travel_ai.itinerary_model.service.GeminiProService;
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

}
