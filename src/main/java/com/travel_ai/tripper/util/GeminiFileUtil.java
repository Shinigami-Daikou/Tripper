package com.travel_ai.tripper.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel_ai.tripper.configuration.ItineraryConfig;
import okhttp3.*;
import org.jboss.logging.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

@Component
public class GeminiFileUtil {
    @Autowired
    ItineraryConfig itineraryConfig;

    private final OkHttpClient client;
    private static final Logger logger = Logger.getLogger(GeminiFileUtil.class);

    public GeminiFileUtil() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .callTimeout(120, TimeUnit.SECONDS);
        this.client = builder.build();
    }

    public String uploadFile(String fileName) {
        File file = new File(itineraryConfig.getTmpFolder() + fileName);
        if (!file.exists()) {
            logger.error("Error: File not found at " + fileName);
            return null;
        }

        String mimeType = URLConnection.guessContentTypeFromName(fileName);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        long numBytes = file.length();

        JSONObject jsonPayload = new JSONObject();
        JSONObject fileInfo = new JSONObject();
        fileInfo.put("display_name", fileName);
        jsonPayload.put("file", fileInfo);

        MediaType jsonMediaType = MediaType.parse("application/json");

        Request startRequest = new Request.Builder()
                .url(itineraryConfig.getGeminiFileUrl() + "?key=" + itineraryConfig.getApiKey())
                .addHeader("X-Goog-Upload-Protocol", "resumable")
                .addHeader("X-Goog-Upload-Command", "start")
                .addHeader("X-Goog-Upload-Header-Content-Length", String.valueOf(numBytes))
                .addHeader("X-Goog-Upload-Header-Content-Type", mimeType)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonPayload.toString(), jsonMediaType))
                .build();

        try (Response response = client.newCall(startRequest).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Error starting upload: " + response.code() + " - " + response.body().string());
                return null;
            }
            String uploadUrl = response.header("X-Goog-Upload-URL");
            if (uploadUrl == null || uploadUrl.isEmpty()) {
                System.err.println("Error: X-Goog-Upload-URL not found in the response headers.");
                return null;
            }

            MediaType mediaType = MediaType.parse(mimeType);

            Request uploadRequest = new Request.Builder()
                    .url(uploadUrl)
                    .addHeader("Content-Length", String.valueOf(numBytes))
                    .addHeader("X-Goog-Upload-Offset", "0")
                    .addHeader("X-Goog-Upload-Command", "upload, finalize")
                    .put(RequestBody.create(file, mediaType))
                    .build();

            try (Response uploadResponse = client.newCall(uploadRequest).execute()) {
                if (!uploadResponse.isSuccessful()) {
                    logger.error("Error uploading file: " + uploadResponse.code() + " - " + uploadResponse.body().string());
                    return null;
                }
                ResponseBody uploadResponseBody = uploadResponse.body();
                if (uploadResponseBody != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonResponse = mapper.readTree(uploadResponseBody.byteStream());
                    System.out.println(jsonResponse.asText());
                    return jsonResponse.get("file").get("uri").asText();
                } else {
                    logger.error("Error: Null response body after upload.");
                    return null;
                }
            }
        } catch (IOException e) {
            logger.error("Error: Response Body stream error.", e);
            return null;
        }
    }
}
