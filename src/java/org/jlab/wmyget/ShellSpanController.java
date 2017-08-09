package org.jlab.wmyget;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
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
@WebServlet(name = "SpanController", urlPatterns = {"/span-data"})
public class ShellSpanController extends HttpServlet {

    private final static Logger LOGGER = Logger.getLogger(ShellSpanController.class.getName());

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
        List<Record> recordList = null;

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

        ShellSpanService service = new ShellSpanService();

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
            recordList = service.getRecordList(c, b, e, l, p, m, M, d, f, s);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to service request", ex);
            errorReason = ex.getMessage();
        }

        OutputStream out = response.getOutputStream();

        if (jsonp != null) {
            out.write((jsonp + "(").getBytes("UTF-8"));
        }

        try (JsonGenerator gen = Json.createGenerator(out)) {
            gen.writeStartObject().writeStartArray("data");
            if (recordList != null) {
                for (Record record : recordList) {
                    gen.writeStartObject();
                    gen.write("date", record.getDate());
                    gen.write("value", record.getChannelValue());
                    gen.writeEnd();
                }
            }
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
