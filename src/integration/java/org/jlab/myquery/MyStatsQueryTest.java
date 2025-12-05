package org.jlab.myquery;

import static org.junit.Assert.assertEquals;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.Test;

public class MyStatsQueryTest {
  @Test
  public void basicUsageTest() throws IOException, InterruptedException {
    /*
    This test runs a basic myStats query across multiple channels and bins.  Only channel1 bin
    "2019-08-12T00:24:00", was fully checked for accuracy.  This same bin is then queried again in basicUsageTest2
    as the only bin to ensure that the binning logic is correct for both single and multiple bins.  This is OK as
    the FloatAnalysisStream handles should be tested more thoroughly as part of jmyapi and that handles all the
    analysis logic outside bin creation.
     */
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "http://localhost:8080/myquery/mystats?c=channel1,channel4&b=2019-08-12+00%3A23%3A50&e=2019-08-12+00%3A24%3A20&n=3&m=docker&f=&v="))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());

    String jsonString =
        """
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
                         "begin": "2019-08-12T00:23:50",
                         "eventCount": 4,
                         "updateCount": 3,
                         "duration": 10,
                         "integration": 958.25815691380921634845435619354248046875,
                         "max": 95.9059,
                         "mean": 95.8258,
                         "min": 95.3710,
                         "rms": 95.8259,
                         "stdev": 0.131686
                       },
                       {
                         "begin": "2019-08-12T00:24:00",
                         "eventCount": 6,
                         "updateCount": 5,
                         "duration": 10,
                         "integration": 955.6611437809115159325301647186279296875,
                         "max": 95.6961,
                         "mean": 95.5661,
                         "min": 95.3710,
                         "rms": 95.5662,
                         "stdev": 0.110596
                       },
                       {
                         "begin": "2019-08-12T00:24:10",
                         "eventCount": 7,
                         "updateCount": 6,
                         "duration": 10,
                         "integration": 959.089333145524506107904016971588134765625,
                         "max": 96.2163,
                         "mean": 95.9089,
                         "min": 95.6568,
                         "rms": 95.9090,
                         "stdev": 0.144037
                       }
                     ],
                     "returnCount": 3
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
                         "begin": "2019-08-12T00:23:50",
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
                         "begin": "2019-08-12T00:24:00",
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
                         "begin": "2019-08-12T00:24:10",
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
                     "returnCount": 3
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
  public void basicUsageTest2() throws IOException, InterruptedException {
    // This test should match the corresponding bin in basicUsageTest.  This helps ensure that the
    // binning logic is
    // correct.  See basicUsageTest comments for more details.
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "http://localhost:8080/myquery/mystats?c=channel1&b=2019-08-12+00%3A24%3A00&e=2019-08-12+00%3A24%3A10&n=1&m=docker&f=3&v=6"))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());

    String jsonString =
        """
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
                      "begin": "2019-08-12 00:24:00.000",
                      "eventCount": 6,
                      "updateCount": 5,
                      "duration": 10,
                      "integration": 955.6611437809115159325301647186279296875,
                      "max": 95.6961,
                      "mean": 95.5661,
                      "min": 95.3710,
                      "rms": 95.5662,
                      "stdev": 0.110596
                    }
                ],
                "returnCount": 1
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
  public void basicUsageTest3() throws IOException, InterruptedException {
    // This test checks that the binning logic is correct the binning moves past the last event in
    // the time range.
    // At one point bins after the last event were producing null values even though the previous
    // event had a value.
    // See basicUsageTest comments for more details.
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "http://localhost:8080/myquery/mystats?c=channel1&b=2019-08-12+23%3A59%3A50&e=2019-08-13+00%3A00%3A20&n=3&m=docker&f=&v="))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());

    String jsonString =
        """
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
                     "begin": "2019-08-12T23:59:50",
                     "eventCount": 7,
                     "updateCount": 6,
                     "duration": 10,
                     "integration": 949.006091671968533773906528949737548828125,
                     "max": 95.1860,
                     "mean": 94.9006,
                     "min": 94.5131,
                     "rms": 94.9009,
                     "stdev": 0.244360
                   },
                   {
                     "begin": "2019-08-13T00:00:00",
                     "eventCount": 2,
                     "updateCount": 1,
                     "duration": 10,
                     "integration": 951.797027587890625,
                     "max": 95.1797,
                     "mean": 95.1797,
                     "min": 95.1797,
                     "rms": 95.1797,
                     "stdev": 0
                   },
                   {
                     "begin": "2019-08-13T00:00:10",
                     "eventCount": 2,
                     "updateCount": 1,
                     "duration": 10,
                     "integration": 951.797027587890625,
                     "max": 95.1797,
                     "mean": 95.1797,
                     "min": 95.1797,
                     "rms": 95.1797,
                     "stdev": 0
                   }
                 ],
                 "returnCount": 3
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
    // Check that we can handle a channel with an unsupported event type without losing data for
    // other channels.
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "http://localhost:8080/myquery/mystats?c=channel1,channel2&b=2019-08-12+00%3A01%3A00&e=2019-08-19+02%3A00%3A00&n=2&m=docker&f=&v="))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());

    String jsonString =
        """
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
                      "integration": 29122133.653106689453125,
                      "max": 95.1797,
                      "mean": 95.1797,
                      "min": 95.1797,
                      "rms": 95.1797,
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
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "http://localhost:8080/myquery/mystats?c=channel1,channel4&b=2019-08-12&e=2019-08-12+01%3A00%3A00&n=1&m=docker&f=3&v=6&jsonp=1234test"))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());

    String exp =
        "1234test({\"channels\":{\"channel1\":{\"metadata\":{\"name\":\"channel1\",\"datatype\":\"DBR_DOUBLE\",\"datasize\":1,\"datahost\":\"mya\",\"ioc\":null,\"active\":true},\"data\":[{\"begin\":\"2019-08-12 00:00:00.000\",\"eventCount\":1716,\"updateCount\":1715,\"duration\":3594.42103290557861328125,\"integration\":341342.2010727164451964199542999267578125,\"max\":96.9524,\"mean\":94.9644,\"min\":0,\"rms\":95.2675,\"stdev\":7.59226}],\"returnCount\":1},\"channel4\":{\"metadata\":{\"name\":\"channel4\",\"datatype\":\"DBR_DOUBLE\",\"datasize\":1,\"datahost\":\"mya\",\"ioc\":null,\"active\":true},\"data\":[{\"begin\":\"2019-08-12 00:00:00.000\",\"eventCount\":0,\"updateCount\":0,\"duration\":null,\"integration\":null,\"max\":null,\"mean\":null,\"min\":null,\"rms\":null,\"stdev\":null}],\"returnCount\":1}}});";

    String result;
    try (BufferedReader reader = new BufferedReader(new StringReader(response.body()))) {
      result = reader.readLine();
      assertEquals(exp, result);
    }
  }
}
