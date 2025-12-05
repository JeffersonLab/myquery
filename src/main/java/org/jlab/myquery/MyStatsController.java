package org.jlab.myquery;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.jlab.mya.Metadata;
import org.jlab.mya.RunningStatistics;
import org.jlab.mya.event.*;
import org.jlab.mya.stream.EventStream;
import org.jlab.mya.stream.FloatAnalysisStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

/**
 * This class provides functionality similar to the command line application myStats.
 * @author adamc
 */
@WebServlet(name = "MyStatsController", value = "/mystats")
public class MyStatsController extends QueryController {
  private static final Logger LOGGER = Logger.getLogger(MyStatsController.class.getName());

  /**
   * Handles the HTTP <code>GET</code> method.
   *
   * @param request  servlet request
   * @param response servlet response
   * @throws IOException if an I/O error occurs
   */
  @SuppressWarnings({"unchecked"})
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String jsonp = request.getParameter("jsonp");

    if (jsonp != null) {
      response.setContentType("application/javascript");
    } else {
      response.setContentType("application/json");
    }

    String errorReason = null;
    List<Metadata> metadatas = null;
    MyStatsResults results = new MyStatsResults();

    String c = request.getParameter("c"); // channels
    String b = request.getParameter("b"); // begin
    String e = request.getParameter("e"); // end
    String n = request.getParameter("n"); // number of bins
    String m = request.getParameter("m"); // deployment
    String d = request.getParameter("d"); // updatesOnly
    String f = request.getParameter("f"); // TimestampFormatter
    String u = request.getParameter("u"); // formatAsMillisSinceEpoch
    String a = request.getParameter("a"); // adjustMillisWithServerOffset
    String v = request.getParameter("v"); // decimalFormatter (value precision)

    try {
      if (c == null || c.trim().isEmpty()) {
        throw new Exception("Channel list (c) is required");
      }
      if (b == null || b.trim().isEmpty()) {
        throw new Exception("Begin Date (b) is required");
      }
      if (e == null || e.trim().isEmpty()) {
        throw new Exception("End Date (e) is required");
      }

      // Replace ' ' with 'T' if present
      b = b.replace(' ', 'T');
      e = e.replace(' ', 'T');

      // If only date and no time then add explicit zero time
      if (b.length() == 10) {
        b = b + "T00:00:00";
      }
      if (e.length() == 10) {
        e = e + "T00:00:00";
      }

      if (n == null || n.isEmpty()) {
        n = "1";
      }

      Instant begin = LocalDateTime.parse(b).atZone(ZoneId.systemDefault()).toInstant();
      Instant end = LocalDateTime.parse(e).atZone(ZoneId.systemDefault()).toInstant();

      String deployment = "ops";
      if (m != null && !m.trim().isEmpty()) {
        deployment = m;
      }

      IntervalWebService service = new IntervalWebService(deployment);

      metadatas = new ArrayList<>();
      List<String> channels;
      try {
        channels = Arrays.asList(c.split(","));
      } catch (PatternSyntaxException ex) {
        throw new Exception("Error parsing comma separated channel list: " + c);
      }

      for (String channel : channels) {
        Metadata metadata = service.findMetadata(channel);
        if (metadata == null) {
          throw new Exception(
              "Unable to find channel: '" + channel + "' in deployment: '" + deployment + "'");
        }
        metadatas.add(metadata);
      }

      long numBins;
      try {
        numBins = Long.parseLong(n);
      } catch (NumberFormatException ex) {
        throw new Exception("Error parsing number of bins (n): '" + n + "'");
      }
      if (numBins < 1) {
        throw new Exception("Number of bins must be >= 1.");
      }

      boolean updatesOnly = (d != null);

      // Don't tell client to cache response if contains future bounds!
      if (end.isAfter(Instant.now())) {
        CacheAndEncodingFilter.disableCaching(response);
      } else {
        response.setHeader("Cache-Control", "private");
      }

      PointWebService pws = new PointWebService(deployment);
      for (Metadata metadata : metadatas) {

        if (metadata.getType() != FloatEvent.class) {
          continue;
        }

        double interval = ((end.getEpochSecond() + end.getNano() / 1_000_000_000d) - (begin.getEpochSecond() + begin.getNano() / 1_000_000_000d)) / numBins;
        Instant binBegin, binEnd = begin;
        for (int i = 1; i <= numBins; i++) {
          binBegin = binEnd;
          if (i == numBins) {
            binEnd = end;
          } else {
            binEnd = binBegin.plusSeconds((long) interval);
          }
          results.add(metadata.getName(), binBegin, getBinStats(binBegin, binEnd, metadata, service, pws, updatesOnly));
        }
      }

    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Unable to service request", ex);
      errorReason = ex.getMessage();
    }

    DateTimeFormatter timestampFormatter = FormatUtil.getInstantFormatter(f);
    short sigFigs = FormatUtil.getSignificantFigures(v);
    boolean formatAsMillisSinceEpoch = (u != null);
    boolean adjustMillisWithServerOffset = (a != null);

    OutputStream out = response.getOutputStream();

    if (jsonp != null) {
      out.write((jsonp + "(").getBytes(StandardCharsets.UTF_8));
    }

    try (JsonGenerator gen = Json.createGenerator(out)) {
      gen.writeStartObject();

      if (errorReason != null) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        gen.write("error", errorReason);
      } else {
        if (metadatas != null) {
          gen.writeStartObject("channels");
          for (Metadata metadata : metadatas) {
            gen.writeStartObject(metadata.getName());

            if (metadata.getType() != FloatEvent.class) {
              gen.write(
                  "error",
                  "This myStats only supports FloatEvents - not '"
                      + metadata.getType().getName()
                      + "'.");
              gen.writeEnd();
              continue;
            }

            writeMetadata("metadata", gen, metadata);
            long dataLength =
                generateStatisticsStream(
                    "data",
                    gen,
                    results.get(metadata.getName()),
                    timestampFormatter,
                    sigFigs,
                    formatAsMillisSinceEpoch,
                    adjustMillisWithServerOffset);
            gen.write("returnCount", dataLength);
            gen.writeEnd(); // metadata.getName()
          }
          gen.writeEnd(); // channels
        }
      }
      gen.writeEnd();
      gen.flush();

      if (jsonp != null) {
        out.write((");").getBytes(StandardCharsets.UTF_8));
      }
    }
  }

  /**
   * Gets the statistics for a bin.  Handle this as a separate method to ensure proper handling of prior events.
   * @implNote There was also an intention to make testing easier, but mocking the web services was not straightforward.
   * @param binBegin The start of the bin.
   * @param binEnd The end of the bin.
   * @param metadata The metadata for the channel.
   * @param intervalService The interval web service.
   * @param pointService The point web service.
   * @param updatesOnly Whether to only include updates.
   * @return The statistics for the bin.
   * @throws Exception If an error occurs.
   */
  private static RunningStatistics getBinStats(Instant binBegin, Instant binEnd, Metadata metadata,
                                              IntervalWebService intervalService, PointWebService pointService,
                                              boolean updatesOnly) throws Exception {
      Event priorEvent = pointService.findEvent(metadata, updatesOnly, binBegin, true, true, false);
      RunningStatistics stats = null;

      // Since we provide a priorPoint, the underlying stream should be a BoundaryAwareStream.
      try (FloatAnalysisStream fas = new FloatAnalysisStream(intervalService.openEventStream(metadata,
                                      updatesOnly, binBegin, binEnd, priorEvent, metadata.getType()))){
          while (fas.read() != null) {
              // Read through the entire stream.  We only want statistics from it
          };
          stats = fas.getLatestStats();
      }
      return stats;
  }
}
