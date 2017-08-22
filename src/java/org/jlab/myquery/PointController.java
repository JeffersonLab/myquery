package org.jlab.myquery;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jlab.mya.Event;
import org.jlab.mya.EventCode;
import org.jlab.mya.event.FloatEvent;
import org.jlab.mya.event.IntEvent;
import org.jlab.mya.event.MultiStringEvent;

/**
 *
 * @author ryans
 */
@WebServlet(name = "PointController", urlPatterns = {"/point"})
public class PointController extends HttpServlet {

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

        String c = request.getParameter("c");
        String t = request.getParameter("t");
        String m = request.getParameter("m");
        String M = request.getParameter("M");
        String d = request.getParameter("d");
        String f = request.getParameter("f");
        String v = request.getParameter("v");
        String w = request.getParameter("w");
        String s = request.getParameter("s");
        String u = request.getParameter("u");
        
        PointWebService service = new PointWebService();

        try {
            if (c == null || c.trim().isEmpty()) {
                throw new Exception("Channel (c) is required");
            }
            if (t == null || t.trim().isEmpty()) {
                throw new Exception("Time of Interest (t) is required");
            }
            // Repace ' ' with 'T' if present
            t = t.replace(' ', 'T');

            Instant time = LocalDateTime.parse(t).atZone(
                    ZoneId.systemDefault()).toInstant();

            event = service.findEvent(c, time, m, M, d, w, s);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to service request", ex);
            errorReason = ex.getMessage();
        }

        boolean formatAsMillisSinceEpoch = (u != null);
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
                gen.writeStartObject("data");
                if (event != null) {
                    FormatUtil.writeTimestampJSON(gen, "d", event.getTimestamp(), formatAsMillisSinceEpoch, timestampFormatter);

                    if (event.getCode() == EventCode.UPDATE) {
                        if (event instanceof IntEvent) {
                            gen.write("v", ((IntEvent) event).getValue());
                        } else if (event instanceof FloatEvent) {
                            FloatEvent ev = (FloatEvent) event;
                            gen.write("v", new BigDecimal(decimalFormatter.format(ev.getValue())));
                        } else { // MultiStringEvent
                            MultiStringEvent ev = (MultiStringEvent) event;
                            gen.write("v", ev.getValue()[0]); // Just grab first one for now...
                        }
                    } else {
                        gen.write("v", event.getCode().name());
                    }

                } // otherwise empty object
                gen.writeEnd();
            }
            gen.writeEnd();

            gen.flush();
            if (jsonp != null) {
                out.write((");").getBytes("UTF-8"));
            }
        }
    }
}
