package org.jlab.myGet;

import java.sql.SQLException;
import java.time.Instant;
import javax.naming.NamingException;
import org.jlab.mya.Deployment;
import org.jlab.mya.EventStream;
import org.jlab.mya.Metadata;
import org.jlab.mya.nexus.PooledNexus;
import org.jlab.mya.params.IntervalQueryParams;
import org.jlab.mya.params.NaiveSamplerParams;
import org.jlab.mya.service.IntervalService;
import org.jlab.mya.service.SamplingService;

/**
 *
 * @author ryans
 */
public class JmyapiSpanService {

    private static final PooledNexus NEXUS;
    
    static {
        try {
            NEXUS = new PooledNexus(Deployment.ops);
        } catch (NamingException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final IntervalService service = new IntervalService(NEXUS);
    
    public Metadata findMetadata(String c) throws SQLException {
        return service.findMetadata(c);
    }
    
    public EventStream openEventStream(Metadata metadata, Instant begin, Instant end, String p, String m,
            String M, String d) throws Exception {
        IntervalQueryParams params = new IntervalQueryParams(metadata, begin, end);
        return service.openFloatStream(params);  
    }

    public Long count(Metadata metadata, Instant begin, Instant end, String p, String m, String M, String d) throws SQLException {
        IntervalQueryParams params = new IntervalQueryParams(metadata, begin, end);
        return service.count(params);
    }

    public EventStream openSampleEventStream(Metadata metadata, Instant begin, Instant end, long limit, String p, String m,
            String M, String d) throws SQLException {
        SamplingService sampler = new SamplingService(NEXUS);
        NaiveSamplerParams params = new NaiveSamplerParams(metadata, begin, end, limit);
        return sampler.openNaiveSamplerFloatStream(params);
    }
}
