package org.jlab.myquery;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.jlab.mya.ExtraInfo;
import org.jlab.mya.Metadata;
import org.jlab.mya.MyaDataType;
import org.jlab.mya.event.*;
import org.jlab.mya.stream.EventStream;
import org.jlab.mya.stream.LabeledEnumStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
 * This method provides an end point for functionality similar to the mySampler command line application.
 *
 * @author adamc
 */
@WebServlet(name = "MySamplerController", value = "/mysampler")
public class MySamplerController extends QueryController {

    private static final Logger LOGGER = Logger.getLogger(MySamplerController.class.getName());

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String jsonp = request.getParameter("jsonp");

        if (jsonp != null) {
            response.setContentType("application/javascript");
        } else {
            response.setContentType("application/json");
        }

        String errorReason = null;
        List<String> channels = null;
        MySamplerWebService service = null;
        long intervalMillis = -1;
        long sampleCount = -1;
        String deployment = "ops";
        Instant begin = null;

        String c = request.getParameter("c"); // channel
        String b = request.getParameter("b"); // begin
        String n = request.getParameter("n"); // sampleCount
        String s = request.getParameter("s"); // intervalMillis
        String m = request.getParameter("m"); // deployment
        String f = request.getParameter("f"); // timestampFormatter (timestamp precision)
        String d = request.getParameter("d"); // updatesOnly
        String e = request.getParameter("e"); // enumsAsStrings
        String u = request.getParameter("u"); // formatAsMillisSinceEpoch
        String a = request.getParameter("a"); // adjustMillisWithServerOffset
        String v = request.getParameter("v"); // decimalFormatter (value precision)

        boolean updatesOnly = (d != null);
        boolean enumsAsStrings = (e != null);

        try {
            if (c == null || c.trim().isEmpty()) {
                throw new Exception("Channel (c) is required");
            }
            if (b == null || b.trim().isEmpty()) {
                throw new Exception("Begin Date (b) is required");
            }
            if (n == null || n.trim().isEmpty()) {
                throw new Exception("Number of sampler (n) is required");
            }
            if (s == null || s.trim().isEmpty()) {
                throw new Exception("Step size (s) in milliseconds is required");
            }

            // Replace ' ' with 'T' if present
            b = b.replace(' ', 'T');

            // If only date and no time then add explicit zero time
            if (b.length() == 10) {
                b = b + "T00:00:00";
            }

            begin = LocalDateTime.parse(b).atZone(ZoneId.systemDefault()).toInstant();

            if (m != null && !m.trim().isEmpty()) {
                deployment = m;
            }

            service = new MySamplerWebService(deployment);

            try {
                channels = Arrays.asList(c.split(","));
            } catch (PatternSyntaxException ex) {
                throw new Exception("Error parsing comma separated channel list: " + c);
            }

            try {
                intervalMillis = Long.parseLong(s);
            } catch (NumberFormatException ex) {
                throw new Exception("Error parsing sample interval (s) in milliseconds: '" + s + "'");
            }
            if (intervalMillis > 315_360_000_000L) {
                throw new IllegalArgumentException("Sample interval (s) must be less than 10 years.");
            }
            try {
                sampleCount = Long.parseLong(n);
            } catch (NumberFormatException ex) {
                throw new Exception("Error parsing number of samples (n): '" + n + "'");
            }


            // Don't tell client to cache response if contains future bounds!
            Instant end = begin.plusMillis(intervalMillis * (sampleCount - 1));
            if (end.isAfter(Instant.now())) {
                CacheAndEncodingFilter.disableCaching(response);
            } else {
                response.setHeader("Cache-Control", "private");
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to service request", ex);
            errorReason = ex.getMessage();
        }

        DateTimeFormatter timestampFormatter = FormatUtil.getInstantFormatter(f);
        DecimalFormat decimalFormatter = FormatUtil.getDecimalFormat(v);
        boolean formatAsMillisSinceEpoch = (u != null);
        boolean adjustMillisWithServerOffset = (a != null);

        try {
            OutputStream out = response.getOutputStream();

            if (jsonp != null) {
                out.write((jsonp + "(").getBytes(StandardCharsets.UTF_8));
            }

            // Won't compile without the -1 setting/check because of possibly uninitialized variables.
            if (errorReason == null) {
                if ( sampleCount == -1) {
                    errorReason = "sampleCount (n) is required";
                } else if (intervalMillis == -1) {
                    errorReason = "intervalMillis (s) is required";
                } else if (begin == null) {
                    errorReason = "begin time (b) is required";
                }
            }

            // The logic around multiple streams is getting a little confusing.  If we've hit an error before we process
            // any channels, let's write the error and close out.
            if (errorReason != null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write(("{\"error\": \"" + errorReason + "\"}").getBytes(StandardCharsets.UTF_8));
                if (jsonp != null) {
                    out.write((");").getBytes(StandardCharsets.UTF_8));
                }
                out.close();
                return;
            }

            try (JsonGenerator gen = Json.createGenerator(out)) {
                gen.writeStartObject();
                boolean anyErrors = false;
                gen.writeStartObject("channels");
                for(String channelName : channels) {
                    boolean error = processChannelRequest(service, deployment, gen, channelName, begin,
                            intervalMillis, sampleCount, updatesOnly, formatAsMillisSinceEpoch,
                            adjustMillisWithServerOffset, timestampFormatter, decimalFormatter, enumsAsStrings);
                    if (error) {
                        anyErrors = true;
                    }
                }
                gen.writeEnd();
                if (anyErrors) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                gen.writeEnd();
                gen.flush();

                if (jsonp != null) {
                    out.write((");").getBytes(StandardCharsets.UTF_8));
                }


            }
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            LOGGER.log(Level.SEVERE, "Unexpected error", ex);
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean processChannelRequest(MySamplerWebService service, String deployment, JsonGenerator gen, String channel,
                                          Instant begin, long intervalMillis, long sampleCount, boolean updatesOnly,
                                          boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset,
                                          DateTimeFormatter timestampFormatter, DecimalFormat decimalFormatter,
                                          boolean enumsAsStrings) throws ServletException {
        gen.writeStartObject(channel);
        boolean error = false;

        // Write out the channel's metadata.
        Metadata metadata = null;
        try {
            metadata = service.findMetadata(channel);
            if (metadata == null) {
                throw new Exception("Unable to find channel: '" + channel + "' in deployment: '" + deployment + "'");
            }
            writeMetadata("metadata", gen, metadata);
        } catch (Exception ex) {
            gen.write("error", ex.getMessage());
            gen.writeEnd();
            return true;
        }

        // Write out the channels enum label's if channel was an enumerated type
        List<ExtraInfo> enumLabels = null;
        if (metadata.getMyaType() == MyaDataType.DBR_ENUM) {
            try {
                enumLabels = service.findExtraInfo(metadata, "enum_strings", begin,
                        begin.plusMillis(intervalMillis * (sampleCount - 1)));
                writeEnumLabels("labels", gen, enumLabels, formatAsMillisSinceEpoch, adjustMillisWithServerOffset,
                        timestampFormatter);
            } catch (Exception ex) {
                gen.write("error", ex.getMessage());
                gen.writeEnd();
                return true;
            }
        }

        // Write out the channel's data
        EventStream stream = null;
        try {

            // Cannot use try with resources since we may sometimes wrap stream in a LabeledEnumStream
            stream = service.openEventStream(metadata, begin, intervalMillis, sampleCount, updatesOnly);
            if (enumsAsStrings && metadata.getMyaType() == MyaDataType.DBR_ENUM && enumLabels != null && !enumLabels.isEmpty()) {

                stream = new LabeledEnumStream(stream, enumLabels);
            }

            gen.writeStartArray("data");
            long dataLength = 0;
            if (stream.getType() == IntEvent.class) {
                dataLength = generateIntStream(gen, (EventStream<IntEvent>) stream, formatAsMillisSinceEpoch, adjustMillisWithServerOffset,
                        timestampFormatter);
            } else if (stream.getType() == FloatEvent.class) {
                dataLength = generateFloatStream(gen, (EventStream<FloatEvent>) stream, formatAsMillisSinceEpoch, adjustMillisWithServerOffset,
                        timestampFormatter, decimalFormatter);
            } else if (stream.getType() == AnalyzedFloatEvent.class) {
                dataLength = generateAnalyzedFloatStream(gen, (EventStream<AnalyzedFloatEvent>) stream, formatAsMillisSinceEpoch, adjustMillisWithServerOffset,
                        timestampFormatter, decimalFormatter);
            } else if (stream.getType() == LabeledEnumEvent.class) {
                dataLength = generateLabeledEnumStream(gen, (EventStream<LabeledEnumEvent>) stream, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
            } else if (stream.getType() == MultiStringEvent.class) {
                dataLength = generateMultiStringStream(gen, (EventStream<MultiStringEvent>) stream, formatAsMillisSinceEpoch, adjustMillisWithServerOffset,
                        timestampFormatter);
            } else {
                gen.writeEnd();
                throw new ServletException("Unsupported data type: " + stream.getClass());
            }

            gen.writeEnd();
            gen.write("returnCount", dataLength);

        } catch(Exception ex) {
            // Can't just return or else we leave a connection open
            error = true;
            try {
                gen.write("error", ex.getMessage());
            } catch(Exception writeEx) {
                LOGGER.log(Level.SEVERE, "Error trying to write error message.", writeEx);
                throw new ServletException("Error trying to write error message to JSON", writeEx);
            }
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception closeIssue) {
                LOGGER.log(Level.SEVERE, "Unable to close stream.  channel=" + metadata.getName());
                error = true;
            }
        }
        gen.writeEnd();
        return error;
    }
}
