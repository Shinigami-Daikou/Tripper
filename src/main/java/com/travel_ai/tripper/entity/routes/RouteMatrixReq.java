package com.travel_ai.tripper.entity.routes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteMatrixReq {
    private List<Origin> origins;
    private List<Destination> destinations;
}
