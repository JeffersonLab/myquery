package org.jlab.myGet;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
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
import org.jlab.mya.EventStream;
import org.jlab.mya.event.FloatEvent;
import org.jlab.mya.event.MultiStringEvent;
import org.jlab.mya.stream.FloatEventStream;
import org.jlab.mya.stream.MultiStringEventStream;

/**
 *
 * @author ryans
 */
@WebServlet(name = "JmyapiSpanController", urlPatterns = {"/jmyapi-span-data"})
public class JmyapilSpanController extends HttpServlet {

    private final static Logger LOGGER = Logger.getLogger(JmyapilSpanController.class.getName());

    private final static DateTimeFormatter DATE_TIME_NO_FRACTIONAL = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd'T'hh:mm:ss");

    private final static ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

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

        JmyapiSpanService service = new JmyapiSpanService();

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
            stream = service.openEventStream(c, b, e, l, p, M, M, d, f, s);
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

        DateTimeFormatter timestampFormatter;

        if (f == null || f.trim().isEmpty()) {
            timestampFormatter = DATE_TIME_NO_FRACTIONAL;
        } else {
            String pattern = "yyyy-MM-dd'T'hh:mm:ss";

            int fint = Integer.parseInt(f);

            if (fint > 9) {
                fint = 9;
            }

            if (fint > 0) {
                pattern = pattern + ".S";
                for (int i = 1; i < fint; i++) {
                    pattern = pattern + "S";
                }
            }

            timestampFormatter = DateTimeFormatter.ofPattern(pattern);
        }

        int vint = 2;

        if (v != null && !v.trim().isEmpty()) {
            vint = Integer.parseInt(v);

            if (vint > 16) {
                vint = 16;
            }
        }

        String decimalPattern = "0";

        if (vint > 0) {
            decimalPattern = decimalPattern + ".#";
            for (int i = 1; i < vint; i++) {
                decimalPattern = decimalPattern + "#";
            }
        }

        //System.out.println(decimalPattern);
        
        // Not thread safe so we just re-create each request
        DecimalFormat decimalFormatter = new DecimalFormat(decimalPattern);

        try {
            OutputStream out = response.getOutputStream();

            if (jsonp != null) {
                out.write((jsonp + "(").getBytes("UTF-8"));
            }

            try (JsonGenerator gen = Json.createGenerator(out)) {
                gen.writeStartObject().writeStartArray("data");
                if(stream == null) {
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
                if (errorReason != null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    gen.write("error", errorReason);
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
                    timestamp.atZone(DEFAULT_ZONE).format(
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
            gen.write("v", decimalFormatter.format(event.getValue()));
            //gen.write("v", event.getValue());
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
