package org.jlab.myquery;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.jlab.mya.*;
import org.jlab.mya.analysis.RunningStatistics;
import org.jlab.mya.event.AnalyzedFloatEvent;
import org.jlab.mya.event.FloatEvent;
import org.jlab.mya.params.*;
import org.jlab.mya.service.IntervalService;
import org.jlab.mya.service.SourceSamplingService;
import org.jlab.mya.stream.IntEventStream;
import org.jlab.mya.stream.wrapped.*;

/**
 *
 * @author adamc, ryans
 */
public class IntervalWebService extends QueryWebService {

    private final IntervalService service;
    private final DataNexus nexus;

    public IntervalWebService(String deployment) {
        nexus = getNexus(deployment);
        service = new IntervalService(nexus);
    }

    public Metadata findMetadata(String c) throws SQLException {
        return service.findMetadata(c);
    }

    public EventStream openEventStream(Metadata metadata, boolean updatesOnly, Instant begin, Instant end,
            boolean enumsAsStrings, Event priorEvent) throws Exception {
        IntervalQueryParams params = new IntervalQueryParams(metadata, updatesOnly, IntervalQueryParams.IntervalQueryFetchStrategy.STREAM, begin, end);
        EventStream stream = service.openEventStream(params);

        if(priorEvent != null) {
            stream = new BoundaryAwareStream<>(stream, begin, end, priorEvent, updatesOnly, null);
        }

        if (enumsAsStrings && metadata.getType() == DataType.DBR_ENUM) {
            List<ExtraInfo> extraInfoList = service.findExtraInfo(metadata, "enum_strings");
            stream = new LabeledEnumStream((IntEventStream) stream, extraInfoList);
        }

        return stream;
    }

    public Long count(Metadata metadata, boolean updatesOnly, Instant begin, Instant end) throws SQLException {
        IntervalQueryParams params = new IntervalQueryParams(metadata, updatesOnly, IntervalQueryParams.IntervalQueryFetchStrategy.STREAM, begin, end);
        return service.count(params);
    }


    public EventStream openSampleEventStream(String sampleType, Metadata metadata, Instant begin, Instant end, long limit,
            long count, boolean enumsAsStrings, boolean updatesOnly, boolean integrate, Event priorEvent) throws SQLException, UnsupportedOperationException {

        // TODO: what about String or other non-numeric types?
        EventStream stream;

        if (sampleType == null || sampleType.isEmpty()){
            throw new IllegalArgumentException("sampleType required.  Options include graphical, event, binned");
        }

        switch(sampleType) {
            case "graphical":  // Application-level event-based, high graphical fidelity
            case "eventsimple": // Application-level event-based. This is likely never a good sampler option given above...
                stream = doApplicationSampling(sampleType, metadata, begin, end, limit, count, updatesOnly, integrate, priorEvent);
                break;
            case "myget": // Database-level time-based.  Stored Procedure: Fastest.  "Basic" graphical fidelity.  Takes first/next actual point at timed intervals
            case "mysampler": // Database-level time-based. Results in n-queries against database.  Takes value at timed intervals (by looking for prior point)
                if(integrate) {
                    throw new UnsupportedOperationException("Integration of input into myget sampler / mysampler algorithm has not been implemented");
                }

                stream = doDatabaseSourceSampling(sampleType, metadata, begin, end, limit, priorEvent);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized sampleType - " + sampleType + ".  Options include graphical, eventsimple, myget, mysampler");
        }

        if (enumsAsStrings && metadata.getType() == DataType.DBR_ENUM) {
            List<ExtraInfo> extraInfoList = service.findExtraInfo(metadata, "enum_strings");
            stream = new LabeledEnumStream((IntEventStream) stream, extraInfoList);
        }

        return stream;
    }

    private EventStream doApplicationSampling(String sampleType, Metadata metadata, Instant begin, Instant end, long limit,
                                              long count, boolean updatesOnly, boolean integrate, Event priorEvent) throws SQLException {

        IntervalService intervalService = new IntervalService(nexus);
        EventStream stream = intervalService.openFloatStream(new IntervalQueryParams(metadata, begin, end));
        Class type = FloatEvent.class;

        if(priorEvent != null) {
            stream = new BoundaryAwareStream(stream, begin, end, priorEvent, updatesOnly, FloatEvent.class);
        }

        if (integrate) { // Careful, we have inner streams now
            stream = new FloatAnalysisStream(stream, new short[]{RunningStatistics.INTEGRATION});
            type = AnalyzedFloatEvent.class;
        }

        switch(sampleType) {
            case "graphical":  // Application-level event-based, high graphical fidelity
                stream = new FloatGraphicalEventBinSampleStream(stream, new GraphicalEventBinSamplerParams(limit, count), type);
                break;
            case "eventsimple": // Application-level event-based. This is likely never a good sampler option given above...
                stream = new FloatSimpleEventBinSampleStream(stream, new SimpleEventBinSamplerParams(limit, count), type);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized sampleType - " + sampleType + ".  Options include graphical, eventsimple, myget, mysampler");
        }
        return stream;

    }

    private EventStream doDatabaseSourceSampling(String sampleType, Metadata metadata, Instant begin, Instant end, long limit,
                                                 Event priorEvent) throws SQLException {
        SourceSamplingService sourceSampler = new SourceSamplingService(nexus);
        EventStream stream;

        switch(sampleType) {
            case "myget": // Database-level time-based.  Stored Procedure: Fastest.  "Basic" graphical fidelity.  Takes first/next actual point at timed intervals
                stream = sourceSampler.openMyGetSampleFloatStream(new MyGetSampleParams(metadata, begin, end, limit));
                break;
            case "mysampler": // Database-level time-based. Results in n-queries against database.  Takes value at timed intervals (by looking for prior point)
                long stepMillis = ((end.getEpochSecond() - begin.getEpochSecond()) / limit) * 1000;
                stream = sourceSampler.openMySamplerFloatStream(new MySamplerParams(metadata, begin, stepMillis, limit));
                break;
            default:
            throw new IllegalArgumentException("Unrecognized sampleType - " + sampleType + ".  Options include graphical, eventsimple, myget, mysampler");
        }

        return stream;
    }
}
