package org.jlab.myquery;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.jlab.mya.*;
import org.jlab.mya.params.BinnedSamplerParams;
import org.jlab.mya.params.EventSamplerParams;
import org.jlab.mya.params.GraphicalSamplerParams;
import org.jlab.mya.params.IntervalQueryParams;
import org.jlab.mya.service.IntervalService;
import org.jlab.mya.service.SamplingService;
import org.jlab.mya.stream.IntEventStream;
import org.jlab.mya.stream.wrapped.LabeledEnumStream;

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
            boolean enumsAsStrings) throws Exception {
        IntervalQueryParams params = new IntervalQueryParams(metadata, updatesOnly, begin, end);
        EventStream stream = service.openEventStream(params);

        if (enumsAsStrings && metadata.getType() == DataType.DBR_ENUM) {
            List<ExtraInfo> extraInfoList = service.findExtraInfo(metadata, "enum_strings");
            stream = new LabeledEnumStream((IntEventStream) stream, extraInfoList);
        }

        return stream;
    }

    public Long count(Metadata metadata, boolean updatesOnly, Instant begin, Instant end) throws SQLException {
        IntervalQueryParams params = new IntervalQueryParams(metadata, updatesOnly, begin, end);
        return service.count(params);
    }


    public EventStream openSampleEventStream(String sampleType, Metadata metadata, Instant begin, Instant end, long limit,
            long count, boolean enumsAsStrings) throws SQLException {

        // TODO: what about String or other non-numeric types?
        EventStream stream;
        SamplingService sampler = new SamplingService(nexus);

        if (sampleType == null || sampleType.isEmpty()){
            throw new IllegalArgumentException("sampleType required.  Options include graphical, event, binned");
        }

        switch(sampleType) {
            case "graphical":
                stream = sampler.openGraphicalSamplerFloatStream(new GraphicalSamplerParams(metadata, begin, end, limit, count));
                break;
            case "binned":
                stream = sampler.openBinnedSamplerFloatStream(new BinnedSamplerParams(metadata, begin, end, limit));
                break;
            case "event":
                stream = sampler.openEventSamplerFloatStream(new EventSamplerParams(metadata, begin, end, limit, count));
                break;
            default:
                throw new IllegalArgumentException("Unrecognized sampleType - " + sampleType + ".  Options include graphical, event, binned");
        }

        if (enumsAsStrings && metadata.getType() == DataType.DBR_ENUM) {
            List<ExtraInfo> extraInfoList = service.findExtraInfo(metadata, "enum_strings");
            stream = new LabeledEnumStream((IntEventStream) stream, extraInfoList);
        }

        return stream;
    }
}
