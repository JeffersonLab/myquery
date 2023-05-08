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

public class MyStatsQueryTest {
    @Test
    public void basicUsageTest() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/myquery/mystats?c=channel1&b=2019-08-12&e=2019-08-12+01%3A00%3A00&n=5&m=docker&f=3&v=2")).build();
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
                                               "begin": "2019-08-12 00:00:00.000",
                                               "eventCount": 335,
                                               "updateCount": 334,
                                               "duration": 719.13,
                                               "integration": 68755.14,
                                               "max": 96.81,
                                               "mean": 95.61,
                                               "min": 94.43,
                                               "rms": 95.61,
                                               "stdev": 0.44
                                             },
                                             {
                                               "begin": "2019-08-12 00:12:00.000",
                                               "eventCount": 369,
                                               "updateCount": 368,
                                               "duration": 719.12,
                                               "integration": 68944.14,
                                               "max": 96.85,
                                               "mean": 95.87,
                                               "min": 94.94,
                                               "rms": 95.87,
                                               "stdev": 0.39
                                             },
                                             {
                                               "begin": "2019-08-12 00:24:00.000",
                                               "eventCount": 343,
                                               "updateCount": 342,
                                               "duration": 718.11,
                                               "integration": 68751.67,
                                               "max": 96.89,
                                               "mean": 95.74,
                                               "min": 95.01,
                                               "rms": 95.74,
                                               "stdev": 0.35
                                             },
                                             {
                                               "begin": "2019-08-12 00:36:00.000",
                                               "eventCount": 317,
                                               "updateCount": 316,
                                               "duration": 718.07,
                                               "integration": 65907.67,
                                               "max": 96.95,
                                               "mean": 91.78,
                                               "min": 0,
                                               "rms": 93.27,
                                               "stdev": 16.59
                                             },
                                             {
                                               "begin": "2019-08-12 00:48:00.000",
                                               "eventCount": 352,
                                               "updateCount": 351,
                                               "duration": 714.12,
                                               "integration": 68422.19,
                                               "max": 96.9,
                                               "mean": 95.81,
                                               "min": 94.85,
                                               "rms": 95.81,
                                               "stdev": 0.45
                                             }
                                           ],
                                           "returnCount": 5
                                         }""";
        String exp;
        try (JsonReader r = Json.createReader(new StringReader(jsonString))) {
            exp = r.readObject().toString();
        }

        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject json = reader.readObject();
            assertEquals(exp, json.toString());
        }
    }
}
