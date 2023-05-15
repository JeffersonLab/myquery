package org.jlab.myquery;

import org.jlab.mya.ExtraInfo;
import org.jlab.mya.Metadata;
import org.jlab.mya.event.Event;
import org.jlab.mya.nexus.DataNexus;
import org.jlab.mya.stream.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides features similar to the mySampler command line utility.
 * @author adamc
 */
public class MySamplerWebService extends QueryWebService {
    private static final Logger LOGGER = Logger.getLogger((MySamplerWebService.class.getName()));
    private final DataNexus nexus;

    public MySamplerWebService(String deployment) {
        nexus = getNexus(deployment);
    }

    public Metadata findMetadata(String c) throws SQLException {
        return nexus.findMetadata(c);
    }

    public List<ExtraInfo> findExtraInfo(Metadata metadata, String type, Instant begin, Instant end) throws SQLException {
        return nexus.findExtraInfo(metadata, type, begin, end);
    }


    @SuppressWarnings("unchecked")
    public <T extends Event> MySamplerStream<T> openEventStream(Metadata<T> metadata, Instant begin, long intervalMillis,
                                                            long sampleCount, boolean updatesOnly) throws SQLException, UnsupportedOperationException {

        PointWebService pws = new PointWebService(nexus.getDeployment());
        T priorEvent = (T) pws.findEvent(metadata, updatesOnly, begin, true, false, false);

        Instant end = begin.plusMillis(intervalMillis * (sampleCount - 1));
        EventStream<T> stream = nexus.openEventStream(metadata, begin, end, DataNexus.IntervalQueryFetchStrategy.STREAM, updatesOnly);

        MySamplerStream<T> out;
        try {
            out = MySamplerStream.getMySamplerStream(stream, begin, intervalMillis, sampleCount, priorEvent, updatesOnly, metadata.getType());
        } catch (Exception ex) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException closeIssue) {
                    LOGGER.log(Level.SEVERE, "Could not close stream", ex);
                }
            }
            throw ex;
        }
        return out;
    }
}
