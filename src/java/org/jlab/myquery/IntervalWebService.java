package org.jlab.myquery;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.jlab.mya.DataType;
import org.jlab.mya.Deployment;
import org.jlab.mya.EventStream;
import org.jlab.mya.ExtraInfo;
import org.jlab.mya.Metadata;
import org.jlab.mya.params.ImprovedSamplerParams;
import org.jlab.mya.params.IntervalQueryParams;
import org.jlab.mya.params.NaiveSamplerParams;
import org.jlab.mya.service.IntervalService;
import org.jlab.mya.service.SamplingService;
import org.jlab.mya.stream.IntEventStream;
import org.jlab.mya.stream.wrapped.LabeledEnumStream;

/**
 *
 * @author ryans
 */
public class IntervalWebService extends QueryWebService {

    private static final long ALWAYS_STREAM_THRESHOLD = 100000; // Just fetch everything (and sample application-side) if under this number of points
    //private static final long EVENTS_PER_BIN_THRESHOLD = 1000; // Just fetch everything (and sample client-side) if bins contain less than 1,000 points (Assuming MAX_POINTS = MIN_SAMPLE_POINTS)
    private static final long MIN_SAMPLE_POINTS = 3000; // If we're doing the iterative query thing we can "cheat" and actually return much less than the limit 

    private final IntervalService service;

    public IntervalWebService(Deployment deployment) {
        service = new IntervalService(getNexus(deployment));
    }

    public Metadata findMetadata(String c) throws SQLException {
        return service.findMetadata(c);
    }

    public EventStream openEventStream(Metadata metadata, Instant begin, Instant end,
            String d, boolean enumsAsStrings) throws Exception {
        IntervalQueryParams params = new IntervalQueryParams(metadata, begin, end);
        EventStream stream = service.openEventStream(params);

        if (enumsAsStrings && metadata.getType() == DataType.DBR_ENUM) {
            List<ExtraInfo> extraInfoList = service.findExtraInfo(metadata, "enum_strings");
            stream = new LabeledEnumStream((IntEventStream) stream, extraInfoList);
        }

        return stream;
    }

    public Long count(Metadata metadata, Instant begin, Instant end, String d) throws SQLException {
        IntervalQueryParams params = new IntervalQueryParams(metadata, begin, end);
        return service.count(params);
    }

    public EventStream openSampleEventStream(Metadata metadata, Instant begin, Instant end, long limit,
            String d, long count, boolean enumsAsStrings) throws SQLException {

        // TODO: what about String or other non-numeric types?
        EventStream stream;
        SamplingService sampler = new SamplingService(OPS_NEXUS);

        long eventsPerBin = count / limit;

        if (count < ALWAYS_STREAM_THRESHOLD) {
            System.out.println("Using 'improved' algorithm");
            ImprovedSamplerParams params = new ImprovedSamplerParams(metadata, begin, end, limit, count);
            stream = sampler.openImprovedSamplerFloatStream(params);
        } else { // Perform n-queries
            System.out.println("Using 'naive' algorithm");
            NaiveSamplerParams params = new NaiveSamplerParams(metadata, begin, end, Math.min(limit, MIN_SAMPLE_POINTS));
            stream = sampler.openNaiveSamplerFloatStream(params);
        }

        if (enumsAsStrings && metadata.getType() == DataType.DBR_ENUM) {
            List<ExtraInfo> extraInfoList = service.findExtraInfo(metadata, "enum_strings");
            stream = new LabeledEnumStream((IntEventStream) stream, extraInfoList);
        }

        return stream;
    }
}
