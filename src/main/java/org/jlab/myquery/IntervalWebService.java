package org.jlab.myquery;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.jlab.mya.*;
import org.jlab.mya.event.AnalyzedFloatEvent;
import org.jlab.mya.event.Event;
import org.jlab.mya.event.FloatEvent;
import org.jlab.mya.nexus.DataNexus;
import org.jlab.mya.stream.*;

/**
 * @author adamc, ryans
 */
public class IntervalWebService extends QueryWebService {

    private final DataNexus nexus;

    public IntervalWebService(String deployment) {
        nexus = getNexus(deployment);
    }

    public Metadata findMetadata(String c) throws SQLException {
        return nexus.findMetadata(c);
    }

    public List<ExtraInfo> findExtraInfo(Metadata metadata, String type, Instant begin, Instant end) throws SQLException {
        return nexus.findExtraInfo(metadata, type, begin, end);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> EventStream<T> openEventStream(Metadata metadata, boolean updatesOnly, Instant begin, Instant end,
                                                            T priorEvent, Class<T> type) throws Exception {
        EventStream stream = nexus.openEventStream(metadata, begin, end, DataNexus.IntervalQueryFetchStrategy.STREAM, updatesOnly);

        if (priorEvent != null) {
            stream = new BoundaryAwareStream(stream, begin, end, priorEvent, updatesOnly, type);
        }

        return stream;
    }

    public Long count(Metadata metadata, boolean updatesOnly, Instant begin, Instant end) throws SQLException {
        return nexus.count(metadata, begin, end, updatesOnly);
    }


    public EventStream<FloatEvent> openSampleEventStream(String sampleType, Metadata<FloatEvent> metadata, Instant begin, Instant end, long limit,
                                                         long count, boolean updatesOnly, boolean integrate, FloatEvent priorEvent, Class<FloatEvent> type) throws SQLException, UnsupportedOperationException {

        EventStream<FloatEvent> stream;

        if (sampleType == null || sampleType.isEmpty()) {
            throw new IllegalArgumentException("sampleType required.  Options include graphical, event, binned");
        }

        switch (sampleType) {
            case "mysampler": // Now application-level time-based. Results in one query against database.
                // Takes value at timed intervals (by looking for prior point)
                if (integrate) {
                    throw new UnsupportedOperationException("Integration of input into mysampler algorithm has not been implemented");
                }
            case "graphical":  // Application-level event-based, high graphical fidelity
            case "eventsimple": // Application-level event-based. This is likely never a good sampler option given above...
                stream = doApplicationSampling(sampleType, metadata, begin, end, limit, count, updatesOnly, integrate, priorEvent, type);
                break;
            case "myget": // Database-level time-based.  Stored Procedure: Fastest.  "Basic" graphical fidelity.  Takes first/next actual point at timed intervals
                if (integrate) {
                    throw new UnsupportedOperationException("Integration of input into myget sampler algorithm has not been implemented");
                }

                stream = doDatabaseSourceSampling(sampleType, metadata, updatesOnly, begin, end, limit, priorEvent, FloatEvent.class);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized sampleType - " + sampleType + ".  Options include graphical, eventsimple, myget, mysampler");
        }

        return stream;
    }

    @SuppressWarnings("unchecked")
    private EventStream<FloatEvent> doApplicationSampling(String sampleType, Metadata metadata, Instant begin, Instant end, long limit,
                                                          long count, boolean updatesOnly, boolean integrate, FloatEvent priorEvent, Class<FloatEvent> type) throws SQLException {

        EventStream<FloatEvent> stream = nexus.openEventStream(metadata, begin, end);

        // Don't do this for mysampler as it extends BoundaryAwareStream and requires a non-null priorEvent.
        if (priorEvent != null && !sampleType.equals("mysampler")) {
            stream = new BoundaryAwareStream<>(stream, begin, end, priorEvent, updatesOnly, type);
        }

        Class type2 = type;

        if (integrate) { // Careful, we have inner streams now
            stream = (EventStream) new FloatAnalysisStream(stream, new short[]{RunningStatistics.INTEGRATION});
            type2 = AnalyzedFloatEvent.class;
        }

        switch (sampleType) {
            case "graphical":  // Application-level event-based, high graphical fidelity
                stream = new FloatGraphicalSampleStream(stream, limit, count, type2);
                break;
            case "eventsimple": // Application-level event-based. This is likely never a good sampler option given above...
                stream = new FloatSimpleSampleStream(stream, limit, count, type2);
                break;
            case "mysampler": // Application-level event-based.  This mimics the CLI mySampler output.
                double endD = (end.getEpochSecond() + end.getNano() / 1_000_000_000d);
                double beginD = (begin.getEpochSecond() + begin.getNano() / 1_000_000_000d);
                long stepMillis = (long) (((endD - beginD) / (limit - 1)) * 1000);
                stream = MySamplerStream.getMySamplerStream(stream, begin, stepMillis, limit, priorEvent, updatesOnly, FloatEvent.class);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized sampleType - " + sampleType + ".  Options include graphical, eventsimple, myget, mysampler");
        }
        return stream;

    }

    private EventStream<FloatEvent> doDatabaseSourceSampling(String sampleType, Metadata<FloatEvent> metadata, boolean updatesOnly, Instant begin, Instant end, long limit,
                                                             FloatEvent priorEvent, Class<FloatEvent> type) throws SQLException {
        EventStream<FloatEvent> stream;

        if (sampleType != null && sampleType.equals("myget")) {
            // Database-level time-based.  Stored Procedure: Fastest.  "Basic" graphical fidelity.  Takes first/next actual point at timed intervals
            stream = nexus.openMyGetSampleStream(metadata, begin, end, limit);
        } else {
            throw new IllegalArgumentException("Unrecognized sampleType - " + sampleType + ".  Options include myget");
        }

        if (priorEvent != null) {
            stream = new BoundaryAwareStream<>(stream, begin, end, priorEvent, updatesOnly, type);
        }

        return stream;
    }
}
