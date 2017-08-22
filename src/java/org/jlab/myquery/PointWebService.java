package org.jlab.myquery;

import java.sql.SQLException;
import java.time.Instant;
import java.util.logging.Logger;
import org.jlab.mya.Event;
import org.jlab.mya.Metadata;
import org.jlab.mya.params.PointQueryParams;
import org.jlab.mya.service.PointService;

/**
 *
 * @author ryans
 */
public class PointWebService extends QueryWebService {

    private final static Logger LOGGER = Logger.getLogger(PointWebService.class.getName());

    private final PointService service = new PointService(NEXUS);  
    
    public Metadata findMetadata(String c) throws SQLException {
        return service.findMetadata(c);
    }    
    
    public Event findEvent(Metadata metadata, Instant t, String m, String M, String d, String w, String s) throws SQLException {
        PointQueryParams params = new PointQueryParams(metadata, t);
        
        return service.findEvent(params);
    }
}
