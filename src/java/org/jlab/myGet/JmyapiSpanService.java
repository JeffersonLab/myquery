package org.jlab.myGet;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;
import org.jlab.mya.Deployment;
import org.jlab.mya.EventStream;
import org.jlab.mya.Metadata;
import org.jlab.mya.event.FloatEvent;
import org.jlab.mya.nexus.PooledNexus;
import org.jlab.mya.params.IntervalQueryParams;
import org.jlab.mya.service.IntervalService;
import org.jlab.mya.stream.FloatEventStream;

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

    public List<Record> getRecordList(String c, String b, String e, String l, String p, String m,
            String M, String d, String f, String s) throws Exception {
        List<Record> recordList = new ArrayList<>();
        IntervalService service = new IntervalService(NEXUS); 
        Metadata metadata = service.findMetadata(c);
        Instant begin = LocalDateTime.parse(b).atZone(
            ZoneId.systemDefault()).toInstant();
        Instant end = LocalDateTime.parse(e).atZone(
            ZoneId.systemDefault()).toInstant();
        IntervalQueryParams params = new IntervalQueryParams(metadata, begin, end);
        try (FloatEventStream stream = service.openFloatStream(params)) {
            FloatEvent event;
            while ((event = stream.read()) != null) {
                System.out.println(event);
            }
        }

        return recordList;
    }

    public EventStream openEventStream(String c, String b, String e, String l, String p, String m,
            String M, String d, String f, String s) throws Exception {
        IntervalService service = new IntervalService(NEXUS); 
        Metadata metadata = service.findMetadata(c);
        Instant begin = LocalDateTime.parse(b).atZone(
            ZoneId.systemDefault()).toInstant();
        Instant end = LocalDateTime.parse(e).atZone(
            ZoneId.systemDefault()).toInstant();
        IntervalQueryParams params = new IntervalQueryParams(metadata, begin, end);
        final FloatEventStream stream = service.openFloatStream(params);

        return stream;      
    }
}
