package org.jlab.myquery;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.Assert.assertEquals;

/**
 * Note: We try to force Java 8 compatibility for the web service itself, but these test require Java 11 in order to
 * take advantage of HttpClient class.  We could use Apache HttpClient dependency instead...
 *
 * Note: We were too lazy to figure out how to spin up a server on the fly so these test require YOU to ensure the
 * web service is running on localhost before running the tests!
 *
 */
public class IntervalQueryTest {
    @Test
    public void doTest() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8888/myquery/interval?c=R123GSET&b=2019-08-01&e=2019-08-11")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //System.out.println(response.body());

        assertEquals(200, response.statusCode());

        try(JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject json = reader.readObject();

            int count = json.getInt("returnCount");

            assertEquals(52, count);
        }
    }
}