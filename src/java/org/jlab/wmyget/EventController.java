package org.jlab.wmyget;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author ryans
 */
@WebServlet(name = "EventController", urlPatterns = {"/event-data"})
public class EventController extends HttpServlet {

    private final static Logger LOGGER = Logger.getLogger(EventController.class.getName());

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
        Record record = null;

        String c = request.getParameter("c");
        String t = request.getParameter("t");
        String m = request.getParameter("m");
        String M = request.getParameter("M");
        String d = request.getParameter("d");
        String f = request.getParameter("f");
        String w = request.getParameter("w");
        String s = request.getParameter("s");

        EventService service = new EventService();

        try {
            record = service.getRecord(c, t, m, M, d, f, w, s);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to service request", ex);
            errorReason = ex.getMessage();
        }
        OutputStream out = response.getOutputStream();

        if (jsonp != null) {
            out.write((jsonp + "(").getBytes("UTF-8"));
        }

        try (JsonGenerator gen = Json.createGenerator(out)) {
            gen.writeStartObject().writeStartObject("data");
            if (record != null) {
                gen.write("date", record.getDate());
                gen.write("value", record.getChannelValue());
            } // otherwise empty object
            gen.writeEnd();
            gen.writeEnd();
            if (errorReason != null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                gen.write("error", errorReason);
            }
            gen.writeEnd();

            gen.flush();
            if (jsonp != null) {
                out.write((");").getBytes("UTF-8"));
            }
        }
    }
}
