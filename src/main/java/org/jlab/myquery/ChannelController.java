package org.jlab.myquery;

import org.jlab.mya.Metadata;
import org.jlab.mya.event.*;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ryans
 */
@WebServlet(name = "ChannelController", urlPatterns = {"/channel"})
public class ChannelController extends QueryController {

    private final static Logger LOGGER = Logger.getLogger(ChannelController.class.getName());

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
        List<Metadata> metadataList = null;

        String q = request.getParameter("q");
        String l = request.getParameter("l");
        String o = request.getParameter("o");
        String m = request.getParameter("m");

        try {
            if (q == null || q.trim().isEmpty()) {
                throw new Exception("Query (q) is required");
            }

            String deployment = "ops";

            if (m != null && !m.trim().isEmpty()) {
                deployment = m;
            }

            PointWebService service = new PointWebService(deployment);

            long limit = 10;
            long offset = 0;

            if(l != null && !l.isEmpty()) {
                try {
                    limit = Long.parseLong(l);
                } catch(NumberFormatException e) {
                    throw new ServletException("limit is not a number: " + l, e);
                }
            }

            if(o != null && !o.isEmpty()) {
                try {
                    offset = Long.parseLong(o);
                } catch(NumberFormatException e) {
                    throw new ServletException("offset is not a number: " + o, e);
                }
            }

            metadataList = service.findChannel(q, limit, offset);

            // Disable caching of response so we don't miss new channels added:
            CacheAndEncodingFilter.disableCaching(response);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unable to service request", ex);
            errorReason = ex.getMessage();
        }

        OutputStream out = response.getOutputStream();

        if (jsonp != null) {
            out.write((jsonp + "(").getBytes("UTF-8"));
        }

        try (JsonGenerator gen = Json.createGenerator(out)) {
            if (errorReason != null) {
                gen.writeStartObject();
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                gen.write("error", errorReason);
                gen.writeEnd();
            } else {
                gen.writeStartArray();
                if (metadataList != null) {
                    for (Metadata metadata : metadataList) {
                        gen.writeStartObject();
                        gen.write("name", metadata.getName());
                        gen.write("datatype", metadata.getMyaType().name());
                        gen.write("datasize", metadata.getSize());
                        gen.write("datahost", metadata.getHost());
                        //gen.write("ioc", metadata.getIoc());
                        //gen.write("active", metadata.isActive());
                        gen.writeEnd();
                    }
                }
                gen.writeEnd();
            }

            gen.flush();
            if (jsonp != null) {
                out.write((");").getBytes("UTF-8"));
            }
        }
    }
}
