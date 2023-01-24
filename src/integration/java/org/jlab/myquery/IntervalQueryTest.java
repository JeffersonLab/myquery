package org.jlab.myquery;

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

public class IntervalQueryTest {
    @Test
    public void doBasicTest() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/myquery/interval?m=docker&c=channel1&b=2019-08-12&e=2019-08-13")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //System.out.println(response.body());

        assertEquals(200, response.statusCode());

        try(JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject json = reader.readObject();

            int count = json.getInt("returnCount");

            assertEquals(32990, count);
        }
    }

    @Test
    public void doWithTimeTest() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/myquery/interval?m=docker&c=channel4&b=2023-01-17+00%3A00%3A00&e=2023-01-17+02%3A10%3A00")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //System.out.println(response.body());

        assertEquals(200, response.statusCode());

        try(JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject json = reader.readObject();

            int count = json.getInt("returnCount");

            assertEquals(6116, count);
        }
    }

    @Test
    public void doIntegratedAndSampledTest() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/myquery/interval?m=docker&c=channel1&b=2019-08-12&e=2019-08-13&l=10&t=graphical&p=on&i=on")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //System.out.println(response.body());

        assertEquals(200, response.statusCode());

        try(JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject json = reader.readObject();

            int count = json.getInt("returnCount");

            assertEquals(23, count);
        }
    }
}