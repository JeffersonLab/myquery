package org.jlab.myquery;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jlab.mya.EventCode;
import org.jlab.mya.EventStream;
import org.jlab.mya.Metadata;
import org.jlab.mya.event.FloatEvent;
import org.jlab.mya.event.MultiStringEvent;
import org.jlab.mya.stream.FloatEventStream;
import org.jlab.mya.stream.MultiStringEventStream;

/**
 *
 * @author ryans
 */
@WebServlet(name = "IntervalController", urlPatterns = {"/interval"})
public class IntervalController extends HttpServlet {

    private final static Logger LOGGER = Logger.getLogger(IntervalController.class.getName());

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String jsonp = request.getParameter("jsonp");

        if (jsonp != null) {
            response.setContentType("application/javascript");
        } else {
            response.setContentType("application/json");
        }

        String errorReason = null;
        EventStream stream = null;
        Long count = null;
        Metadata metadata = null;
        boolean sample = false;

        String c = request.getParameter("c");
        String b = request.getParameter("b");
        String e = request.getParameter("e");
        String l = request.getParameter("l");
        String p = request.getParameter("p");
        String m = request.getParameter("m");
        String M = request.getParameter("M");
        String d = request.getParameter("d");
        String f = request.getParameter("f");
        String s = request.getParameter("s");
        String t = request.getParameter("t");
        String v = request.getParameter("v");

        IntervalWebService service = new IntervalWebService();

        try {
            if (c == null || c.trim().isEmpty()) {
                throw new Exception("Channel (c) is required");
            }
            if (b == null || b.trim().isEmpty()) {
                throw new Exception("Begin Date (b) is required");
            }
            if (e == null || e.trim().isEmpty()) {
                throw new Exception("End Date (e) is required");
            }
            // Repace ' ' with 'T' if present
            b = b.replace(' ', 'T');
            e = e.replace(' ', 'T');

            Instant begin = LocalDateTime.parse(b).atZone(
                    ZoneId.systemDefault()).toInstant();
            Instant end = LocalDateTime.parse(e).atZone(
                    ZoneId.systemDefault()).toInstant();

            metadata = service.findMetadata(c);

            long limit = -1;

            if (l != null && !l.trim().isEmpty()) {
                limit = Long.parseLong(l);
                // We were given a limit so we must count
                count = service.count(metadata, begin, end, p, m, M, d);
                // This query seems to take about 0.1 second, so we only do it if necessary

                if (count > limit) {
                    sample = true;
                }
            }

            if (sample) {
                stream = service.openSampleEventStream(metadata, begin, end, limit, p, m, M, d, count);
            } else {
                stream = service.openEventStream(metadata, begin, end, p, m, M, d);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to service request", ex);
            errorReason = ex.getMessage();

            try {
                stream.close();
                stream = null;
            } catch (Exception closeIssue) {
                System.err.println("Unable to close stream");
            }
        }

        DateTimeFormatter timestampFormatter = service.getInstantFormatter(f);
        DecimalFormat decimalFormatter = service.getDecimalFormat(v);

        try {
            OutputStream out = response.getOutputStream();

            if (jsonp != null) {
                out.write((jsonp + "(").getBytes("UTF-8"));
            }

            try (JsonGenerator gen = Json.createGenerator(out)) {
                gen.writeStartObject();

                if (errorReason != null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    gen.write("error", errorReason);
                } else {
                    if (metadata != null) {
                        gen.write("datatype", metadata.getType().name());
                        gen.write("datasize", metadata.getSize());
                        gen.write("datahost", metadata.getHost());
                    }

                    gen.write("sampled", sample);

                    if (count != null) {
                        gen.write("count", count);
                    }

                    gen.writeStartArray("data");
                    if (stream == null) {
                        // Didn't get a stream so presumably there is an errorReason
                    } else if (stream instanceof FloatEventStream) {
                        generateFloatStream(gen, (FloatEventStream) stream, (t != null),
                                timestampFormatter, decimalFormatter);
                    } else if (stream instanceof MultiStringEventStream) {
                        generateMultiStringStream(gen, (MultiStringEventStream) stream, (t != null),
                                timestampFormatter);
                    } else {
                        errorReason = "Unsupported data type: " + stream.getClass();
                    }
                    gen.writeEnd();
                }
                gen.writeEnd();

                gen.flush();
            }
            if (jsonp != null) {
                out.write((");").getBytes("UTF-8"));
            }
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception closeIssue) {
                System.err.println("Unable to close stream");
            }
        }
    }

    private void writeTimestamp(JsonGenerator gen, java.time.Instant timestamp,
            boolean formatAsMillisSinceEpoch, DateTimeFormatter formatter) {
        if (formatAsMillisSinceEpoch) {
            gen.write("d", (timestamp.getEpochSecond() * 1000)
                    + (timestamp.getLong(ChronoField.MILLI_OF_SECOND)));
        } else {
            gen.write("d",
                    timestamp.atZone(QueryWebService.DEFAULT_ZONE).format(
                            formatter));
        }
    }

    private void generateFloatStream(JsonGenerator gen, FloatEventStream stream,
            boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter,
            DecimalFormat decimalFormatter) throws IOException {
        FloatEvent event = null;
        while ((event = stream.read()) != null) {
            gen.writeStartObject();
            writeTimestamp(gen, event.getTimestamp(), formatAsMillisSinceEpoch, timestampFormatter);

            if (event.getCode() == EventCode.UPDATE) {
                // Round number (banker's rounding) and create String then create new BigDecimal to ensure no quotes are used in JSON
                gen.write("v", new BigDecimal(decimalFormatter.format(event.getValue())));

                // This is an alternative to the above
                /*BigDecimal v = new BigDecimal(event.getValue()); // passing string would be safter than float
            v = v.setScale(decimalFormatter.getMaximumFractionDigits(), BigDecimal.ROUND_HALF_EVEN); // Create new BigDecimal
            v = v.stripTrailingZeros(); // Create new BigDecimal
            gen.write("v", v);*/
                // This would always show maximum precision / scale
                //gen.write("v", event.getValue());
            } else {
                gen.write("v", event.getCode().name());
            }

            gen.writeEnd();
        }
    }

    private void generateMultiStringStream(JsonGenerator gen,
            MultiStringEventStream stream, boolean formatAsMillisSinceEpoch,
            DateTimeFormatter timestampFormatter) throws IOException {
        MultiStringEvent event = null;
        while ((event = stream.read()) != null) {
            gen.writeStartObject();
            writeTimestamp(gen, event.getTimestamp(), formatAsMillisSinceEpoch, timestampFormatter);
            gen.write("v", event.getValue()[0]); // Just grab first value
            gen.writeEnd();
        }
    }
}
