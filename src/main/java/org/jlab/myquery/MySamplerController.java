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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This method provides an end point for functionality similar to the mySampler command line application.
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
        Metadata metadata = null;
        List<ExtraInfo> enumLabels = null;


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

            Instant begin = LocalDateTime.parse(b).atZone(
                    ZoneId.systemDefault()).toInstant();

            String deployment = "ops";
            if (m != null && !m.trim().isEmpty()) {
                deployment = m;
            }

            MySamplerWebService service = new MySamplerWebService(deployment);

            metadata = service.findMetadata(c);
            if (metadata == null) {
                throw new Exception("Unable to find channel: '" + c + "' in deployment: '" + deployment + "'");
            }

            long intervalMillis, sampleCount;
            try {
                intervalMillis = Long.parseLong(s);
            } catch (NumberFormatException ex) {
                throw new Exception("Error parsing sample interval (s) in milliseconds: '" + s + "'");
            }
            try {
                sampleCount = Long.parseLong(n);
            } catch (NumberFormatException ex) {
                throw new Exception("Error parsing number of samples (n): '" + n + "'");
            }

            boolean updatesOnly = (d != null);
            boolean enumsAsStrings = (e != null);

            // Don't tell client to cache response if contains future bounds!
            Instant end = begin.plusMillis(intervalMillis * (sampleCount - 1));
            if (end.isAfter(Instant.now())) {
                CacheAndEncodingFilter.disableCaching(response);
            } else {
                response.setHeader("Cache-Control", "private");
            }

            // Get the sampled stream.  If it's an enum convert to use string labels if requested.
            stream = service.openEventStream(metadata, begin, intervalMillis, sampleCount, updatesOnly);
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
        DecimalFormat decimalFormatter = FormatUtil.getDecimalFormat(v);
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
                        if (metadata.getIoc() == null) {
                            gen.writeNull("ioc");
                        } else {
                            gen.write("ioc", metadata.getIoc());
                        }
                        gen.write("active", metadata.isActive());
                    }

                    if (enumLabels != null && enumLabels.size() > 0) {
                        gen.writeStartArray("labels");
                        for (ExtraInfo info : enumLabels) {
                            gen.writeStartObject();
                            FormatUtil.writeTimestampJSON(gen, "d", info.getTimestamp(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
                            gen.writeStartArray("value");
                            for (String token : info.getValueAsArray()) {
                                if (token != null && !token.isEmpty()) {
                                    gen.write(token);
                                }
                            }
                            gen.writeEnd();
                            gen.writeEnd();
                        }
                        gen.writeEnd();
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
