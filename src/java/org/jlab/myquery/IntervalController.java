package org.jlab.myquery;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jlab.mya.Deployment;
import org.jlab.mya.Event;
import org.jlab.mya.EventStream;
import org.jlab.mya.Metadata;
import org.jlab.mya.event.FloatEvent;
import org.jlab.mya.event.IntEvent;
import org.jlab.mya.event.MultiStringEvent;
import org.jlab.mya.stream.FloatEventStream;
import org.jlab.mya.stream.IntEventStream;
import org.jlab.mya.stream.MultiStringEventStream;
import org.jlab.mya.stream.wrapped.LabeledEnumStream;

/**
 *
 * @author ryans
 */
@WebServlet(name = "IntervalController", urlPatterns = {"/interval"})
public class IntervalController extends QueryController {

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
        Event priorEvent = null;
        Long count = null;
        Metadata metadata = null;
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
        String v = request.getParameter("v");

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

            Deployment deployment = Deployment.ops;

            if (m != null && !m.trim().isEmpty()) {
                deployment = Deployment.valueOf(m);
            }

            if (deployment != Deployment.ops && deployment != Deployment.dev) {
                throw new Exception("Unsupported deployment: " + deployment);
            }

            IntervalWebService service = new IntervalWebService(deployment);

            metadata = service.findMetadata(c);

            if (metadata == null) {
                throw new Exception("Unable to find channel: '" + c + "' in deployment: '" + deployment + "'");
            }

            boolean enumsAsStrings = (s != null);            
            
            if (p != null) { // Include prior point
                PointWebService pointService = new PointWebService(deployment);
                priorEvent = pointService.findEvent(metadata, begin, d, true, false, enumsAsStrings);
            }

            long limit = -1;

            if (l != null && !l.trim().isEmpty()) {
                limit = Long.parseLong(l);
                // We were given a limit so we must count
                count = service.count(metadata, begin, end, d);
                // This query seems to take about 0.1 second, so we only do it if necessary

                if (count > limit) {
                    sample = true;
                }
            }            
            
            if (sample) {
                stream = service.openSampleEventStream(metadata, begin, end, limit, d, count, enumsAsStrings);
            } else {
                stream = service.openEventStream(metadata, begin, end, d, enumsAsStrings);
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

        DateTimeFormatter timestampFormatter = FormatUtil.getInstantFormatter(f);
        DecimalFormat decimalFormatter = FormatUtil.getDecimalFormat(v);
        boolean formatAsMillisSinceEpoch = (u != null);

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

                    if (priorEvent != null) {
                        if (priorEvent instanceof IntEvent) {
                            writeIntEvent(gen, (IntEvent) priorEvent, formatAsMillisSinceEpoch, timestampFormatter);
                        } else if (priorEvent instanceof FloatEvent) {
                            writeFloatEvent(gen, (FloatEvent) priorEvent, formatAsMillisSinceEpoch, timestampFormatter, decimalFormatter);
                        } else if (priorEvent instanceof MultiStringEvent) {
                            writeMultiStringEvent(gen, (MultiStringEvent) priorEvent, formatAsMillisSinceEpoch, timestampFormatter);
                        } else {
                            throw new ServletException("Unsupported data type: " + priorEvent.getClass());
                        }
                    }

                    if (stream == null) {
                        // Didn't get a stream so presumably there is an errorReason
                    } else if (stream instanceof IntEventStream) {
                        generateIntStream(gen, (IntEventStream) stream, formatAsMillisSinceEpoch,
                                timestampFormatter);
                    } else if (stream instanceof FloatEventStream) {
                        generateFloatStream(gen, (FloatEventStream) stream, formatAsMillisSinceEpoch,
                                timestampFormatter, decimalFormatter);
                    } else if(stream instanceof LabeledEnumStream) {
                        generateLabeledEnumStream(gen, (LabeledEnumStream)stream, formatAsMillisSinceEpoch, timestampFormatter);
                    } else if (stream instanceof MultiStringEventStream) {
                        generateMultiStringStream(gen, (MultiStringEventStream) stream, formatAsMillisSinceEpoch,
                                timestampFormatter);
                    } else {
                        throw new ServletException("Unsupported data type: " + stream.getClass());
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
}
