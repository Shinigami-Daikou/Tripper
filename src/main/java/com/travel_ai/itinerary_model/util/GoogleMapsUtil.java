package com.travel_ai.itinerary_model.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel_ai.itinerary_model.configuration.ItineraryConfig;
import com.travel_ai.itinerary_model.entity.Distance;
import com.travel_ai.itinerary_model.entity.Place;
import com.travel_ai.itinerary_model.entity.routes.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

record TextQuery(String textQuery) {}

@Component
public class GoogleMapsUtil {
    @Autowired
    ItineraryConfig itineraryConfig;

    private static final Logger logger = Logger.getLogger(GoogleMapsUtil.class);
    private final OkHttpClient client;

    public GoogleMapsUtil() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        this.client = builder.build();
    }

    public List<JsonNode> fetchPlacesDetailsByName(List<Place> places){
        List<JsonNode> googleMapFetchPlaces = new ArrayList<>();
        for(Place place: places){
            String textQuery = new StringBuilder(place.getName()).append(" ").append(place.getAddress()).toString();
            TextQuery textQueryObj = new TextQuery(textQuery);
            try {
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(textQueryObj);

                Request request = new Request.Builder().
                        url(itineraryConfig.getGooglePlacesURL() + ":searchText").
                        header("Content-Type", "application/json").
                        header("X-Goog-Api-Key", itineraryConfig.getGoogleMapsAPIKey()).
                        header("X-Goog-FieldMask", "*").
                        post(RequestBody.create(json.getBytes(StandardCharsets.UTF_8))).build();

                Response response = client.newCall(request).execute();
                try(InputStream stream = response.body().byteStream()){
                    googleMapFetchPlaces.add(mapper.readTree(stream));
                }
            } catch (JsonProcessingException e) {
                logger.error("Unable to create Query text.", e);
            }
            catch (IOException e) {
                logger.error("Google Map Request failed.", e);
            }
        }
        return googleMapFetchPlaces;
    }

    public List<Distance> getRoutesMatrix(HashMap<String, String> placeIDMap, List<String> placeIDList){
        List<Origin> origins = new ArrayList<>();
        List<Destination> destinations = new ArrayList<>();

        for(int i = 0; i < placeIDMap.size(); i++){
            origins.add(new Origin(new Waypoint(placeIDList.get(i))));
            destinations.add(new Destination(new Waypoint(placeIDList.get(i))));
        }

        RouteMatrixReq routeMatrixReq = new RouteMatrixReq(origins, destinations);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(routeMatrixReq);
            Request request = new Request.Builder()
                    .url(itineraryConfig.getGoogleRoutesURL())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Goog-Api-Key", itineraryConfig.getGoogleMapsAPIKey())
                    .addHeader("X-Goog-FieldMask", "*")
                    .post(RequestBody.create(jsonPayload.getBytes(StandardCharsets.UTF_8)))
                    .build();

            try(Response response = client.newCall(request).execute()){
                List<RouteMatrixRes> resList = mapper.readValue(
                        response.body().byteStream(), new TypeReference<List<RouteMatrixRes>>() {});

                List<Distance> distances = new ArrayList<>();
                for(RouteMatrixRes res: resList){
                    if(res.getCondition() != null &&
                            res.getCondition().equalsIgnoreCase("ROUTE_EXISTS")) {
                        String source = placeIDMap.get(placeIDList.get(res.getOriginIndex()));
                        String destination = placeIDMap.get(placeIDList.get(res.getDestinationIndex()));
                        double distance = (double) res.getDistanceMeters() / 1000;

                        String time;
                        if(res.getDuration().indexOf('.') != -1){
                            time = res.getDuration().substring(0, res.getDuration().lastIndexOf('.'));
                        } else
                            time = res.getDuration().substring(0, res.getDuration().lastIndexOf('s'));
                        int timeTaken = Integer.parseInt(time) / 60;

                        distances.add(new Distance(source, destination, distance, timeTaken,
                                new ArrayList<>(List.of("Drive"))));
                    }
                }
                return distances;
            } catch (IOException e) {
                logger.error("Error: Routes API request unsuccessful.", e);
                return null;
            }


        } catch (JsonProcessingException e) {
            logger.error("Error: Cannot create Routes API RequestBody.", e);
            return null;
        }
    }
}
