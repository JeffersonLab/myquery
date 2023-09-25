package org.jlab.myquery;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jlab.mya.*;
import org.jlab.mya.event.*;
import org.jlab.mya.stream.EventStream;
import org.jlab.mya.stream.FloatAnalysisStream;
import org.jlab.mya.stream.LabeledEnumStream;

/**
 * @author ryans
 */
@WebServlet(name = "IntervalController", urlPatterns = {"/interval"})
public class IntervalController extends QueryController {

    private final static Logger LOGGER = Logger.getLogger(IntervalController.class.getName());

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @SuppressWarnings({"unchecked"})
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
        Event priorEvent = null;
        Long count = null;
        Metadata metadata = null;
        List<ExtraInfo> enumLabels = null;
        boolean sample = false;

        String c = request.getParameter("c");
        String b = request.getParameter("b");
        String e = request.getParameter("e");
        String l = request.getParameter("l");
        String p = request.getParameter("p");
        String m = request.getParameter("m");
        String d = request.getParameter("d");
        String f = request.getParameter("f");
        String s = request.getParameter("s");
        String u = request.getParameter("u");
        String a = request.getParameter("a");
        String v = request.getParameter("v");
        String t = request.getParameter("t");
        String i = request.getParameter("i");

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

            Instant begin = LocalDateTime.parse(b).atZone(
                    ZoneId.systemDefault()).toInstant();
            Instant end = LocalDateTime.parse(e).atZone(
                    ZoneId.systemDefault()).toInstant();

            String deployment = "ops";

            if (m != null && !m.trim().isEmpty()) {
                deployment = m;
            }

            IntervalWebService service = new IntervalWebService(deployment);

            metadata = service.findMetadata(c);

            if (metadata == null) {
                throw new Exception("Unable to find channel: '" + c + "' in deployment: '" + deployment + "'");
            }

            boolean updatesOnly = (d != null);
            boolean enumsAsStrings = (s != null);

            if (p != null || (t != null && t.equals("mysampler"))) { // Include prior point
                PointWebService pointService = new PointWebService(deployment);
                priorEvent = pointService.findEvent(metadata, updatesOnly, begin, true, false, enumsAsStrings);
            }

            long limit = -1;

            if (l != null && !l.trim().isEmpty()) {
                limit = Long.parseLong(l);
                // We were given a limit so we must count
                count = service.count(metadata, updatesOnly, begin, end);
                // This query seems to take about 0.1 second, so we only do it if necessary

                if (count > limit) {
                    sample = true;

                    // Default to graphical sampling if nothing is specified.
                    if (t == null || t.isEmpty()) {
                        t = "graphical";
                    }
                }
            }

            if(end.isAfter(Instant.now())) { // Don't tell client to cache response if contains future bounds!
                CacheAndEncodingFilter.disableCaching(response);
            } else { // Let's cache, but only share value (proxy servers) if sampled
                if(!sample) {
                    response.setHeader("Cache-Control", "private");
                }
            }

            boolean integrate = i != null && (t != null && !t.trim().isEmpty());

            Class type = metadata.getType();

            if (sample) {
                if(type != FloatEvent.class) {
                    throw new IllegalArgumentException("Only float events can be sampled");
                }

                stream = service.openSampleEventStream(t, metadata, begin, end, limit, count, updatesOnly, integrate, (FloatEvent)priorEvent, type);
            } else {
                stream = service.openEventStream(metadata, updatesOnly, begin, end, priorEvent, type);

                if(integrate) {
                    stream = new FloatAnalysisStream(stream, new short[]{RunningStatistics.INTEGRATION});
                }
            }

            if(metadata.getMyaType() == MyaDataType.DBR_ENUM) {
                enumLabels = service.findExtraInfo(metadata, "enum_strings", begin, end);

                if (enumsAsStrings) {
                    stream = new LabeledEnumStream((EventStream<IntEvent>) stream, enumLabels);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to service request", ex);
            errorReason = ex.getMessage();

            try {
                if (stream != null) {
                    stream.close();
                    stream = null;
                }
            } catch (Exception closeIssue) {
                System.err.println("Unable to close stream");
            }
        }

        DateTimeFormatter timestampFormatter = FormatUtil.getInstantFormatter(f);
        short sigFigs = FormatUtil.getSignificantFigures(v);
        boolean formatAsMillisSinceEpoch = (u != null);
        boolean adjustMillisWithServerOffset = (a != null);

        try {
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
                    if (metadata != null) {
                        gen.write("datatype", metadata.getMyaType().name());
                        gen.write("datasize", metadata.getSize());
                        gen.write("datahost", metadata.getHost());
                        if(metadata.getIoc() == null) {
                            gen.writeNull("ioc");
                        } else {
                            gen.write("ioc", metadata.getIoc());
                        }
                        gen.write("active", metadata.isActive());
                    }

                    if(enumLabels != null && enumLabels.size() > 0) {
                        gen.writeStartArray("labels");
                        for(ExtraInfo info: enumLabels) {
                            gen.writeStartObject();
                            FormatUtil.writeTimestampJSON(gen, "d", info.getTimestamp(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
                            gen.writeStartArray("value");
                            for(String token: info.getValueAsArray()) {
                                if(token != null && !token.isEmpty()) {
                                    gen.write(token);
                                }
                            }
                            gen.writeEnd();
                            gen.writeEnd();
                        }
                        gen.writeEnd();
                    }

                    gen.write("sampled", sample);

                    if (count != null) {
                        gen.write("count", count);
                    }
                    if (sample) {
                        gen.write("sampleType", t);
                    }

                    gen.writeStartArray("data");

                    long dataLength = 0;
                    if (stream == null) {
                        // Didn't get a stream so presumably there is an errorReason
                    } else if (stream.getType() == IntEvent.class) {
                        dataLength = generateIntStream(gen, (EventStream<IntEvent>) stream, formatAsMillisSinceEpoch, adjustMillisWithServerOffset,
                                timestampFormatter);
                    } else if (stream.getType() == FloatEvent.class) {
                        dataLength = generateFloatStream(gen, (EventStream<FloatEvent>) stream, formatAsMillisSinceEpoch, adjustMillisWithServerOffset,
                                timestampFormatter, sigFigs);
                    } else if(stream.getType() == AnalyzedFloatEvent.class) {
                        dataLength = generateAnalyzedFloatStream(gen, (EventStream<AnalyzedFloatEvent>) stream, formatAsMillisSinceEpoch, adjustMillisWithServerOffset,
                                timestampFormatter, sigFigs);
                    } else if (stream.getType() == LabeledEnumEvent.class) {
                        dataLength = generateLabeledEnumStream(gen, (EventStream<LabeledEnumEvent>) stream, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
                    } else if (stream.getType() == MultiStringEvent.class) {
                        dataLength = generateMultiStringStream(gen, (EventStream<MultiStringEvent>) stream, formatAsMillisSinceEpoch, adjustMillisWithServerOffset,
                                timestampFormatter);
                    } else {
                        throw new ServletException("Unsupported data type: " + stream.getClass());
                    }
                    gen.writeEnd();

                    gen.write("returnCount", dataLength);
                }
                gen.writeEnd();

                gen.flush();
            }
            if (jsonp != null) {
                out.write((");").getBytes(StandardCharsets.UTF_8));
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
}
