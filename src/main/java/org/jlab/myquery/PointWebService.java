package org.jlab.myquery;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.jlab.mya.*;
import org.jlab.mya.event.IntEvent;
import org.jlab.mya.event.LabeledEnumEvent;
import org.jlab.mya.params.PointQueryParams;
import org.jlab.mya.service.PointService;

/**
 *
 * @author ryans, adamc
 */
public class PointWebService extends QueryWebService {

    private final PointService service;
    
    public PointWebService(String deployment) {
        DataNexus nexus = getNexus(deployment);
        service = new PointService(nexus);
    }
    
    public Metadata findMetadata(String c) throws SQLException {
        return service.findMetadata(c);
    }    
    
    public Event findEvent(Metadata metadata, boolean updatesOnly, Instant t, boolean lessThan, boolean orEqual, boolean enumsAsStrings) throws SQLException {
        PointQueryParams params = new PointQueryParams(metadata, updatesOnly, t, lessThan, orEqual);
        
        Event event = service.findEvent(params);
        
        if(enumsAsStrings && metadata.getType() == DataType.DBR_ENUM) {
            List<ExtraInfo> extraInfoList = service.findExtraInfo(metadata, "enum_strings");
            event = LabeledEnumEvent.findLabelFromHistory((IntEvent)event, extraInfoList);
        }    
        
        return event;
    }
}
