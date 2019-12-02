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
import org.jlab.mya.Metadata;
import org.jlab.mya.event.*;

/**
 *
 * @author ryans
 */
@WebServlet(name = "PointController", urlPatterns = {"/point"})
public class PointController extends QueryController {

    private final static Logger LOGGER = Logger.getLogger(PointController.class.getName());

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
        Event event = null;
        Metadata metadata = null;

        String c = request.getParameter("c");
        String t = request.getParameter("t");
        String m = request.getParameter("m");
        String d = request.getParameter("d");
        String f = request.getParameter("f");
        String v = request.getParameter("v");
        String w = request.getParameter("w");
        String x = request.getParameter("x");
        String s = request.getParameter("s");
        String u = request.getParameter("u");
        String a = request.getParameter("a");

        try {
            if (c == null || c.trim().isEmpty()) {
                throw new Exception("Channel (c) is required");
            }
            if (t == null || t.trim().isEmpty()) {
                throw new Exception("Time of Interest (t) is required");
            }
            // Repace ' ' with 'T' if present
            t = t.replace(' ', 'T');

            // If only date and no time then add explicit zero time
            if (t.length() == 10) {
                t = t + "T00:00:00";
            }

            Instant time = LocalDateTime.parse(t).atZone(
                    ZoneId.systemDefault()).toInstant();

            String deployment = "ops";

            if (m != null && !m.trim().isEmpty()) {
                deployment = m;
            }

            PointWebService service = new PointWebService(deployment);

            metadata = service.findMetadata(c);

            if (metadata == null) {
                throw new Exception("Unable to find channel: '" + c + "' in deployment: '" + deployment + "'");
            }

            boolean lessThan = (w == null);
            boolean orEqual = (x == null);
            
            boolean updatesOnly = (d != null);     
            boolean enumsAsStrings = (s != null);

            event = service.findEvent(metadata, updatesOnly, time, lessThan, orEqual, enumsAsStrings);

            // Disable caching of response if:
            // (1) no forward event found (yet) - check again later
            // (2) requested future time - prior point might change
            if((!lessThan && event == null) || time.isAfter(Instant.now())) {
                CacheAndEncodingFilter.disableCaching(response);
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to service request", ex);
            errorReason = ex.getMessage();
        }

        boolean formatAsMillisSinceEpoch = (u != null);
        boolean adjustMillisWithServerOffset = (a != null);
        DateTimeFormatter timestampFormatter = FormatUtil.getInstantFormatter(f);
        DecimalFormat decimalFormatter = FormatUtil.getDecimalFormat(v);

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
                    gen.write("datatype", metadata.getMyaType().name());
                    gen.write("datasize", metadata.getSize());
                    gen.write("datahost", metadata.getHost());
                }
                if (event != null) {
                    if (event instanceof IntEvent) {
                        writeIntEvent("data", gen, (IntEvent) event, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
                    } else if (event instanceof FloatEvent) {
                        writeFloatEvent("data", gen, (FloatEvent) event, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter, decimalFormatter);
                    } else if (event instanceof LabeledEnumEvent) {
                        writeLabeledEnumEvent(null, gen, (LabeledEnumEvent) event, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
                    } else if (event instanceof MultiStringEvent) {
                        writeMultiStringEvent("data", gen, (MultiStringEvent) event, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
                    } else {
                        throw new ServletException("Unsupported data type: " + event.getClass());
                    }

                } else { // empty data
                    gen.writeStartObject("data");
                    gen.writeEnd();
                }
            }
            gen.writeEnd();

            gen.flush();
            if (jsonp != null) {
                out.write((");").getBytes("UTF-8"));
            }
        }
    }
}
