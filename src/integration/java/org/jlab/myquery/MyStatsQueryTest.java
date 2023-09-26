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
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/myquery/mystats?c=channel1,channel4&b=2019-08-12&e=2019-08-12+01%3A00%3A00&n=5&m=docker&f=3&v=6")).build();
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
                      "duration": 719.1279857158660888671875,
                      "integration": 68755.144563262627343647181987762451171875,
                      "max": 96.8135,
                      "mean": 95.6091,
                      "min": 94.4322,
                      "rms": 95.6100,
                      "stdev": 0.436171
                    },
                    {
                      "begin": "2019-08-12 00:12:00.000",
                      "eventCount": 369,
                      "updateCount": 368,
                      "duration": 719.1194341182708740234375,
                      "integration": 68944.138235634469310753047466278076171875,
                      "max": 96.8513,
                      "mean": 95.8730,
                      "min": 94.9427,
                      "rms": 95.8738,
                      "stdev": 0.390679
                    },
                    {
                      "begin": "2019-08-12 00:24:00.000",
                      "eventCount": 343,
                      "updateCount": 342,
                      "duration": 718.11330127716064453125,
                      "integration": 68751.66884353742352686822414398193359375,
                      "max": 96.8915,
                      "mean": 95.7393,
                      "min": 95.0116,
                      "rms": 95.7400,
                      "stdev": 0.352170
                    },
                    {
                      "begin": "2019-08-12 00:36:00.000",
                      "eventCount": 317,
                      "updateCount": 316,
                      "duration": 718.073926448822021484375,
                      "integration": 65907.665449329026159830391407012939453125,
                      "max": 96.9524,
                      "mean": 91.7840,
                      "min": 0,
                      "rms": 93.2710,
                      "stdev": 16.5887
                    },
                    {
                      "begin": "2019-08-12 00:48:00.000",
                      "eventCount": 352,
                      "updateCount": 351,
                      "duration": 714.1164848804473876953125,
                      "integration": 68422.188102417494519613683223724365234375,
                      "max": 96.9037,
                      "mean": 95.8138,
                      "min": 94.8486,
                      "rms": 95.8148,
                      "stdev": 0.453055
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
                     "integration": 27214184.8855658471584320068359375,
                     "max": 103.997,
                     "mean": 88.9440,
                     "min": 0,
                     "rms": 91.1415,
                     "stdev": 19.8931
                   },
                   {
                     "begin": "2019-08-15T13:00:30",
                     "eventCount": 2,
                     "updateCount": 1,
                     "duration": 305970,
                     "integration": 29282429.886932373046875,
                     "max": 95.7036,
                     "mean": 95.7036,
                     "min": 95.7036,
                     "rms": 95.7036,
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
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/myquery/mystats?c=channel1,channel4&b=2019-08-12&e=2019-08-12+01%3A00%3A00&n=1&m=docker&f=3&v=6&jsonp=1234test")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        String exp = "1234test({\"channels\":{\"channel1\":{\"metadata\":{\"name\":\"channel1\",\"datatype\":\"DBR_DOUBLE\",\"datasize\":1,\"datahost\":\"mya\",\"ioc\":null,\"active\":true},\"data\":[{\"begin\":\"2019-08-12 00:00:00.000\",\"eventCount\":1716,\"updateCount\":1715,\"duration\":3594.42103290557861328125,\"integration\":341342.2010727164451964199542999267578125,\"max\":96.9524,\"mean\":94.9644,\"min\":0,\"rms\":95.2675,\"stdev\":7.59226}],\"returnCount\":1},\"channel4\":{\"metadata\":{\"name\":\"channel4\",\"datatype\":\"DBR_DOUBLE\",\"datasize\":1,\"datahost\":\"mya\",\"ioc\":null,\"active\":true},\"data\":[{\"begin\":\"2019-08-12 00:00:00.000\",\"eventCount\":0,\"updateCount\":0,\"duration\":null,\"integration\":null,\"max\":null,\"mean\":null,\"min\":null,\"rms\":null,\"stdev\":null}],\"returnCount\":1}}});";

        String result;
        try (BufferedReader reader = new BufferedReader(new StringReader(response.body()))) {
            result = reader.readLine();
            assertEquals(exp, result);
        }
    }
}
