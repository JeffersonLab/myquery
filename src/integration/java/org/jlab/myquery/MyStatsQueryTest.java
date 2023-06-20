package org.jlab.myquery;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.junit.Test;

import java.io.BufferedReader;
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
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/myquery/mystats?c=channel1,channel4&b=2019-08-12&e=2019-08-12+01%3A00%3A00&n=5&m=docker&f=3&v=2")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String jsonString = """
          {
            "channels": {
              "channel1": {
                "metadata": {
                  "name": "channel1",
                  "datatype": "DBR_DOUBLE",
                  "datasize": 1,
                  "datahost": "mya",
                  "ioc": null,
                  "active": true
                },
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
              },
              "channel4": {
                "metadata": {
                  "name": "channel4",
                  "datatype": "DBR_DOUBLE",
                  "datasize": 1,
                  "datahost": "mya",
                  "ioc": null,
                  "active": true
                },
                "data": [
                    {
                      "begin": "2019-08-12 00:00:00.000",
                      "eventCount": 0,
                      "updateCount": 0,
                      "duration": null,
                      "integration": null,
                      "max": null,
                      "mean": null,
                      "min": null,
                      "rms": null,
                      "stdev": null
                    },
                    {
                      "begin": "2019-08-12 00:12:00.000",
                      "eventCount": 0,
                      "updateCount": 0,
                      "duration": null,
                      "integration": null,
                      "max": null,
                      "mean": null,
                      "min": null,
                      "rms": null,
                      "stdev": null
                    },
                    {
                      "begin": "2019-08-12 00:24:00.000",
                      "eventCount": 0,
                      "updateCount": 0,
                      "duration": null,
                      "integration": null,
                      "max": null,
                      "mean": null,
                      "min": null,
                      "rms": null,
                      "stdev": null
                    },
                    {
                      "begin": "2019-08-12 00:36:00.000",
                      "eventCount": 0,
                      "updateCount": 0,
                      "duration": null,
                      "integration": null,
                      "max": null,
                      "mean": null,
                      "min": null,
                      "rms": null,
                      "stdev": null
                    },
                    {
                      "begin": "2019-08-12 00:48:00.000",
                      "eventCount": 0,
                      "updateCount": 0,
                      "duration": null,
                      "integration": null,
                      "max": null,
                      "mean": null,
                      "min": null,
                      "rms": null,
                      "stdev": null
                    }
                ],
                "returnCount": 5
              }
            }
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

    @Test
    public void unsupportedTypeTest() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/myquery/mystats?c=channel1,channel2&b=2019-08-12+00%3A01%3A00&e=2019-08-19+02%3A00%3A00&n=2&m=docker&f=&v=")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String jsonString = """
           {
             "channels": {
               "channel1": {
                 "metadata": {
                   "name": "channel1",
                   "datatype": "DBR_DOUBLE",
                   "datasize": 1,
                   "datahost": "mya",
                   "ioc": null,
                   "active": true
                 },
                 "data": [
                   {
                     "begin": "2019-08-12T00:01:00",
                     "eventCount": 32963,
                     "updateCount": 32962,
                     "duration": 305970,
                     "integration": 27214184.885566,
                     "max": 103.997002,
                     "mean": 88.943965,
                     "min": 0,
                     "rms": 91.141455,
                     "stdev": 19.893113
                   },
                   {
                     "begin": "2019-08-15T13:00:30",
                     "eventCount": 2,
                     "updateCount": 1,
                     "duration": 305970,
                     "integration": 29282429.886932,
                     "max": 95.703598,
                     "mean": 95.703598,
                     "min": 95.703598,
                     "rms": 95.703598,
                     "stdev": 0
                   }
                 ],
                 "returnCount": 2
               },
               "channel2": {
                 "error": "This myStats only supports FloatEvents - not 'org.jlab.mya.event.IntEvent'."
               }
             }
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

    @Test
    public void jsonpTest() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/myquery/mystats?c=channel1,channel4&b=2019-08-12&e=2019-08-12+01%3A00%3A00&n=1&m=docker&f=3&v=2&jsonp=1234test")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String exp = "1234test({\"channels\":{\"channel1\":{\"metadata\":{\"name\":\"channel1\",\"datatype\":\"DBR_DOUBLE\",\"datasize\":1,\"datahost\":\"mya\",\"ioc\":null,\"active\":true},\"data\":[{\"begin\":\"2019-08-12 00:00:00.000\",\"eventCount\":1716,\"updateCount\":1715,\"duration\":3594.42,\"integration\":341342.2,\"max\":96.95,\"mean\":94.96,\"min\":0,\"rms\":95.27,\"stdev\":7.59}],\"returnCount\":1},\"channel4\":{\"metadata\":{\"name\":\"channel4\",\"datatype\":\"DBR_DOUBLE\",\"datasize\":1,\"datahost\":\"mya\",\"ioc\":null,\"active\":true},\"data\":[{\"begin\":\"2019-08-12 00:00:00.000\",\"eventCount\":0,\"updateCount\":0,\"duration\":null,\"integration\":null,\"max\":null,\"mean\":null,\"min\":null,\"rms\":null,\"stdev\":null}],\"returnCount\":1}}});";

        String result;
        try (BufferedReader reader = new BufferedReader(new StringReader(response.body()))) {
            result = reader.readLine();
            assertEquals(exp, result);
        }
    }
}
