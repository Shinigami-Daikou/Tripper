package com.travel_ai.itinerary_model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record Part(String text) {}
@JsonIgnoreProperties(ignoreUnknown = true)
record Content(List<Part> parts) {}
@JsonIgnoreProperties(ignoreUnknown = true)
record Candidate(Content content) {}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIOutput {
    List<Candidate> candidates;

    public String getText(){
        return candidates.get(0).content().parts().get(0).text();
    }
}

