package org.jlab.myquery;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.http.HttpServlet;
import org.jlab.mya.RunningStatistics;
import org.jlab.mya.event.*;
import org.jlab.mya.stream.EventStream;

/**
 *
 * @author ryans, adamc
 */
@SuppressWarnings("JavaDoc")
public class QueryController extends HttpServlet {

    private void writeInformationalEvent(JsonGenerator gen, Event event) {
        gen.write("t", event.getCode().name());
        if (event.getCode().isDisconnection()) {
            gen.write("x", true);
        }
    }

    public void writeIntEvent(String name, JsonGenerator gen, IntEvent event, boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }

        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestampAsInstant(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            gen.write("v", event.getValue());
        } else {
            writeInformationalEvent(gen, event);
        }

        gen.writeEnd();
    }

    public void writeFloatEvent(String name, JsonGenerator gen, FloatEvent event, boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter, DecimalFormat decimalFormatter) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }

        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestampAsInstant(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            // Round number (banker's rounding) and create String then create new BigDecimal to ensure no quotes are used in JSON
            gen.write("v", new BigDecimal(decimalFormatter.format(event.getValue())));
        } else {
            writeInformationalEvent(gen, event);
        }

        gen.writeEnd();
    }

    public void writeAnalyzedFloatEvent(String name, JsonGenerator gen, AnalyzedFloatEvent event, boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter, DecimalFormat decimalFormatter) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }

        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestampAsInstant(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            // Round number (banker's rounding) and create String then create new BigDecimal to ensure no quotes are used in JSON
            gen.write("v", new BigDecimal(decimalFormatter.format(event.getValue())));
            double[] stats = event.getEventStats();
            if(stats != null && stats.length == 1) {
                // We only support integration at this point.  Once more are supported this will have to be a loop that
                // references short[] map for position/index of each stat
                // i for integration?   Good enough for now
                gen.write("i", new BigDecimal(decimalFormatter.format(stats[0])));
            }
        } else {
            writeInformationalEvent(gen, event);
        }

        gen.writeEnd();
    }

    public void writeLabeledEnumEvent(String name, JsonGenerator gen, LabeledEnumEvent event, boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }
        
        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestampAsInstant(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            gen.write("v", event.getLabel());
        } else {
            writeInformationalEvent(gen, event);
        }

        gen.writeEnd();
    }

    public void writeMultiStringEvent(String name, JsonGenerator gen, MultiStringEvent event, boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }
        
        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestampAsInstant(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            String[] values = event.getValue();
            gen.writeStartArray("v");
            for (String value : values) {
                gen.write(value);
            }
            gen.writeEnd();
        } else {
            writeInformationalEvent(gen, event);
        }

        gen.writeEnd();
    }

    /**
     * Write out the IntEventStream to a JsonGenerator.
     * @param gen
     * @param stream
     * @param formatAsMillisSinceEpoch
     * @param timestampFormatter
     * @return The number of events written to the JSON generator
     * @throws IOException
     */
    public long generateIntStream(JsonGenerator gen, EventStream<IntEvent> stream,
            boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter) throws IOException {
        long count = 0;
        IntEvent event;
        while ((event = stream.read()) != null) {
            count++;
            writeIntEvent(null, gen, event, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
        }
        return count;
    }

    /**
     * This method write a stream of RunningStatistics associated with a given start time to a JSON generator.
     * @param gen The JSON generator to write them to
     * @param stats The Map of timestamps to RunningStatistics that will be written to the JSON generator
     * @param timestampFormatter How to format timestamps
     * @param decimalFormatter How to format decimal values
     * @param formatAsMillisSinceEpoch Should timestamps be in seconds from Epoch
     * @param adjustMillisWithServerOffset
     * @return The number of RunningStatistics written to the stream
     */
    public long generateStatisticsStream(JsonGenerator gen, Map<Instant, RunningStatistics> stats,
                                         DateTimeFormatter timestampFormatter, DecimalFormat decimalFormatter,
                                         boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset) {
        long count = 0;
        for (Instant begin : stats.keySet()) {
            RunningStatistics stat = stats.get(begin);
            gen.writeStartObject();
            FormatUtil.writeTimestampJSON(gen, "begin", begin, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
            gen.write("eventCount", stat.getEventCount());
            gen.write("updateCount", stat.getUpdateCount());

            if (stat.getDuration() == null) {
                gen.writeNull("duration");
            } else {
                gen.write("duration", new BigDecimal(decimalFormatter.format(stat.getDuration())));
            }
            if (stat.getIntegration() == null) {
                gen.writeNull("integration");
            } else {
                gen.write("integration", new BigDecimal(decimalFormatter.format(stat.getIntegration())));
            }

            if (stat.getMax() == null) {
                gen.writeNull("max");
            } else {
                gen.write("max", new BigDecimal(decimalFormatter.format(stat.getMax())));
            }
            if (stat.getMean() == null) {
                gen.writeNull("mean");
            } else {
                gen.write("mean", new BigDecimal(decimalFormatter.format(stat.getMean())));
            }
            if (stat.getMin() == null) {
                gen.writeNull("min");
            } else {
                gen.write("min", new BigDecimal(decimalFormatter.format(stat.getMin())));
            }
            if (stat.getRms() == null) {
                gen.writeNull("rms");
            } else {
                gen.write("rms", new BigDecimal(decimalFormatter.format(stat.getRms())));
            }
            if (stat.getSigma() == null) {
                gen.writeNull("stdev");
            } else {
                gen.write("stdev", new BigDecimal(decimalFormatter.format(stat.getSigma())));
            }
            gen.writeEnd();
            count++;
        }

        return count;
    }

    /**
     * Write out the FloatEvents to a JsonGenerator.
     *
     * @param gen
     * @param stream
     * @param formatAsMillisSinceEpoch
     * @param timestampFormatter
     * @param decimalFormatter
     * @return The count of events written to the JsonGenerator
     * @throws IOException
     */
    public long generateFloatStream(JsonGenerator gen, EventStream<FloatEvent> stream,
            boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter,
            DecimalFormat decimalFormatter) throws IOException {
        long count = 0;
        FloatEvent event;
        while ((event = stream.read()) != null) {
            count++;
            writeFloatEvent(null, gen, event, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter, decimalFormatter);
        }
        return count;
    }

    /**
     * Write out the AnalyzedFloatEvents to a JsonGenerator.
     *
     * @param gen
     * @param stream
     * @param formatAsMillisSinceEpoch
     * @param timestampFormatter
     * @param decimalFormatter
     * @return The count of events written to the JsonGenerator
     * @throws IOException
     */
    public long generateAnalyzedFloatStream(JsonGenerator gen, EventStream<AnalyzedFloatEvent> stream,
                                    boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter,
                                    DecimalFormat decimalFormatter) throws IOException {
        long count = 0;
        AnalyzedFloatEvent event;
        while ((event = stream.read()) != null) {
            count++;
            writeAnalyzedFloatEvent(null, gen, event, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter, decimalFormatter);
        }
        return count;
    }

    /**
     * Write out the LabeledEnumStream to a JsonGenerator
     * @param gen
     * @param stream
     * @param formatAsMillisSinceEpoch
     * @param timestampFormatter
     * @return The count of events written to the JsonGenerator
     * @throws IOException
     */
    public long generateLabeledEnumStream(JsonGenerator gen,
            EventStream<LabeledEnumEvent> stream, boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset,
            DateTimeFormatter timestampFormatter) throws IOException {
        LabeledEnumEvent event;
        long count = 0;
        while ((event = stream.read()) != null) {
            count++;
            writeLabeledEnumEvent(null, gen, event, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
        }
        return count;
    }

    /**
     * Write out a MultiStringStream to a JsonGenerator
     * @param gen
     * @param stream
     * @param formatAsMillisSinceEpoch
     * @param timestampFormatter
     * @return The count of events written to the JsonGenerator
     * @throws IOException
     */
    public long generateMultiStringStream(JsonGenerator gen,
            EventStream<MultiStringEvent> stream, boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset,
            DateTimeFormatter timestampFormatter) throws IOException {
        MultiStringEvent event;
        long count = 0;
        while ((event = stream.read()) != null) {
            count++;
            writeMultiStringEvent(null, gen, event, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
        }
        return count;
    }
}
