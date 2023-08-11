package org.jlab.myquery;

import org.junit.Assert;
import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.Assert.assertEquals;

public class PointQueryTest {
    @Test
    public void doTest() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/myquery/point?m=docker&c=channel1&t=2019-08-13")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //System.out.println(response.body());

        assertEquals(200, response.statusCode());

        try(JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject json = reader.readObject();

            JsonObject data = json.getJsonObject("data");

            String d = data.getString("d");

            assertEquals("2019-08-12T23:59:58", d);
        }
    }

    @Test
    public void basicUsage() throws IOException, InterruptedException {
        // Test that we get the origin point as expected.
        String endPoint = "http://localhost:8080/myquery/point?m=docker&c=channel1&t=2019-08-12+00:00:00.2925281&f=6";

        String expString =  "{\"datatype\":\"DBR_DOUBLE\",\"datasize\":1,\"datahost\":\"mya\"," +
                "\"data\":{\"d\":\"2019-08-12 00:00:00.292528\",\"v\":95.180199,\"t\":\"ORIGIN_OF_CHANNELS_HISTORY\"}}";
        JsonObject exp;
        try (JsonReader reader = Json.createReader(new StringReader(expString))) {
            exp = reader.readObject();
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endPoint)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject result;
        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            result = reader.readObject();
        }

        Assert.assertEquals(exp, result);
    }


}