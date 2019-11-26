package org.jlab.myquery;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.jlab.mya.*;
import org.jlab.mya.event.Event;
import org.jlab.mya.event.IntEvent;
import org.jlab.mya.event.LabeledEnumEvent;
import org.jlab.mya.nexus.DataNexus;

/**
 *
 * @author ryans, adamc
 */
public class PointWebService extends QueryWebService {

    private DataNexus nexus;

    public PointWebService(String deployment) {
        DataNexus nexus = getNexus(deployment);
    }
    
    public Metadata findMetadata(String c) throws SQLException {
        return nexus.findMetadata(c);
    }    

    @SuppressWarnings("unchecked")
    public Event findEvent(Metadata metadata, boolean updatesOnly, Instant t, boolean lessThan, boolean orEqual, boolean enumsAsStrings) throws SQLException {
        Event event = nexus.findEvent(metadata, t, lessThan, orEqual, updatesOnly);
        
        if(enumsAsStrings && metadata.getMyaType() == MyaDataType.DBR_ENUM) {
            List<ExtraInfo> extraInfoList = nexus.findExtraInfo(metadata, "enum_strings");
            event = LabeledEnumEvent.findLabelFromHistory((IntEvent)event, extraInfoList);
        }    
        
        return event;
    }
}
