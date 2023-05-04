package org.jlab.myquery;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.Assert.assertEquals;

public class MySamplerQueryTest {
    @Test
    public void basicUsageTest() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/myquery/mysampler?c=channel1&b=2019-08-12+23%3A59%3A00&n=5&s=15000&m=docker&f=6&v=")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String jsonString = """
                {
                            "datatype": "DBR_DOUBLE",
                                "datasize": 1,
                                "datahost": "mya",
                                "ioc": null,
                                "active": true,
                                "data": [
                            {
                                "d": "2019-08-12 23:59:00.000000",
                                    "v": 94.550102
                            },
                            {
                                "d": "2019-08-12 23:59:15.000000",
                                    "v": 94.987701
                            },
                            {
                                "d": "2019-08-12 23:59:30.000000",
                                    "v": 94.651604
                            },
                            {
                                "d": "2019-08-12 23:59:45.000000",
                                    "v": 94.292702
                            },
                            {
                                "d": "2019-08-13 00:00:00.000000",
                                    "v": 95.179703
                            }
                  ],
                            "returnCount": 5
                        }""";
        String exp;
        try(JsonReader r = Json.createReader(new StringReader(jsonString))) {
            exp = r.readObject().toString();
        }

        try(JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject json = reader.readObject();
            assertEquals(exp, json.toString());
        }
    }
}
