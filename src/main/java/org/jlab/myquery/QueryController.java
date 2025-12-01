package org.jlab.myquery;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.http.HttpServlet;
import org.jlab.mya.ExtraInfo;
import org.jlab.mya.Metadata;
import org.jlab.mya.RunningStatistics;
import org.jlab.mya.event.*;
import org.jlab.mya.stream.EventStream;

/**
 *
 * @author ryans, adamc
 */
@SuppressWarnings("JavaDoc")
public class QueryController extends HttpServlet {

    private void writeDisconnectAndType(JsonGenerator gen, Event event) {
        if (event.getCode().isDisconnection()) {
            gen.write("x", JsonValue.TRUE);
        }
        if (!event.getCode().getDescription().isEmpty()) {
            gen.write("t", event.getCode().name());
        }
    }
    public void writeIntEvent(String name, JsonGenerator gen, IntEvent event, boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }

        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestampAsInstant(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);

        if (!event.getCode().isDisconnection()) {
            gen.write("v", event.getValue());
        }
        writeDisconnectAndType(gen, event);

        gen.writeEnd();
    }

    public void writeFloatEvent(String name, JsonGenerator gen, FloatEvent event, boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter, short sigFigs) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }

        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestampAsInstant(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);

        if (!event.getCode().isDisconnection()) {
            // Round number (banker's rounding) and create String then create new BigDecimal to ensure no quotes are used in JSON
            BigDecimal bd = new BigDecimal(event.getValue());
            gen.write("v", bd.round(new MathContext(sigFigs)));
        }
        writeDisconnectAndType(gen, event);

        gen.writeEnd();
    }

    public void writeAnalyzedFloatEvent(String name, JsonGenerator gen, AnalyzedFloatEvent event, boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter, short sigFigs) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }

        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestampAsInstant(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);

        if (!event.getCode().isDisconnection()) {
            // Round number (banker's rounding) and create String then create new BigDecimal to ensure no quotes are used in JSON
            BigDecimal bd = new BigDecimal(event.getValue());
            gen.write("v", bd.round(new MathContext(sigFigs)));
        }
        writeDisconnectAndType(gen, event);

        // stats should be available for even disconnect events since things like integration would be happening up to
        // the disconnection.
        double[] stats = event.getEventStats();
        if(stats != null && stats.length == 1) {
            // We only support integration at this point.  Once more are supported this will have to be a loop that
            // references short[] map for position/index of each stat
            // i for integration?   Good enough for now
            BigDecimal bd = new BigDecimal(stats[0]);
            gen.write("i", bd.round(new MathContext(sigFigs)));
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

        if (!event.getCode().isDisconnection()) {
            if(event.getLabel() == null) {
                gen.write("v", event.getValue());
            } else {
                gen.write("v", event.getLabel());
            }
        }
        writeDisconnectAndType(gen, event);

        gen.writeEnd();
    }

    public void writeMultiStringEvent(String name, JsonGenerator gen, MultiStringEvent event, boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }
        
        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestampAsInstant(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);

        if (!event.getCode().isDisconnection()) {
            String[] values = event.getValue();
            gen.writeStartArray("v");
            for (String value : values) {
                gen.write(value);
            }
            gen.writeEnd();
        }
        writeDisconnectAndType(gen, event);

        gen.writeEnd();
    }

    public void writeRunningStatistics(String name, JsonGenerator gen, RunningStatistics stat, Instant begin,
                                       boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset,
                                       DateTimeFormatter timestampFormatter, short sigFigs){
        if (name == null) {
            gen.writeStartObject();
        } else {
            gen.writeStartObject(name);
        }

        FormatUtil.writeTimestampJSON(gen, "begin", begin, formatAsMillisSinceEpoch, adjustMillisWithServerOffset,
                timestampFormatter);
        gen.write("eventCount", stat.getEventCount());
        gen.write("updateCount", stat.getUpdateCount());

        MathContext context = new MathContext(sigFigs);

        if (stat.getDuration() == null) {
            gen.writeNull("duration");
        } else {
            BigDecimal bd = new BigDecimal(stat.getDuration());
            gen.write("duration", bd);
        }
        if (stat.getIntegration() == null) {
            gen.writeNull("integration");
        } else {
            BigDecimal bd = new BigDecimal(stat.getIntegration());
            gen.write("integration", bd);
        }

        if (stat.getMax() == null) {
            gen.writeNull("max");
        } else {
            BigDecimal bd = new BigDecimal(stat.getMax());
            gen.write("max", bd.round(context));
        }
        if (stat.getMean() == null) {
            gen.writeNull("mean");
        } else {
            BigDecimal bd = new BigDecimal(stat.getMean());
            gen.write("mean", bd.round(context));
        }
        if (stat.getMin() == null) {
            gen.writeNull("min");
        } else {
            BigDecimal bd = new BigDecimal(stat.getMin());
            gen.write("min", bd.round(context));
        }
        if (stat.getRms() == null) {
            gen.writeNull("rms");
        } else {
            BigDecimal bd = new BigDecimal(stat.getRms());
            gen.write("rms", bd.round(context));
        }
        if (stat.getSigma() == null) {
            gen.writeNull("stdev");
        } else {
            BigDecimal bd = new BigDecimal(stat.getSigma());
            gen.write("stdev", bd.round(context));
        }
        gen.writeEnd();
    }

    /**
     * Write out Metadata to a JSON generator
     * @param gen The generator to write to
     * @param metadata The metadata to write
     */
    public void writeMetadata(String name, JsonGenerator gen, Metadata metadata) {
        if (name == null) {
            gen.writeStartObject();
        } else {
            gen.writeStartObject(name);
        }
        gen.write("name", metadata.getName());
        gen.write("datatype", metadata.getMyaType().name());
        gen.write("datasize", metadata.getSize());
        gen.write("datahost", metadata.getHost());
        if (metadata.getIoc() == null) {
            gen.writeNull("ioc");
        } else {
            gen.write("ioc", metadata.getIoc());
        }
        gen.write("active", metadata.isActive());
        gen.writeEnd();
    }

    /**
     * Write out the enum labels for a single channel to a JSON generator
     * @param name Optional name for the label list
     * @param gen The JSON generator to write to
     * @param enumLabelList The list of ExtraInfo, i.e., the enum labels over time
     * @param formatAsMillisSinceEpoch Write timestamps in more precise Unix-like time format
     * @param adjustMillisWithServerOffset Adjust timestamps for server
     * @param timestampFormatter How to format the datetimes associated with values
     */
    public void writeEnumLabels(String name, JsonGenerator gen, List<ExtraInfo> enumLabelList,
                                boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset,
                                DateTimeFormatter timestampFormatter) {
        if (name != null) {
            gen.writeStartArray(name);
        } else {
            gen.writeStartArray();
        }
        for (ExtraInfo info : enumLabelList) {
            gen.writeStartObject();
            FormatUtil.writeTimestampJSON(gen, "d", info.getTimestamp(), formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter);
            gen.writeStartArray("value");
            for (String token : info.getValueAsArray()) {
                if (token != null && !token.isEmpty()) {
                    gen.write(token);
                }
            }
            gen.writeEnd(); // value array
            gen.writeEnd(); // object
        }
        gen.writeEnd(); // array

    }

    /**
     * Write a list of metadata objects as a series of JSON objects.  Assumes that a JSON array has been started/ended
     * outside of this method.
     * @param gen The JSON generator used to write the data
     * @param metadataList The metadata objects to write
     */
    public void generateMetadataStream(JsonGenerator gen, List<Metadata> metadataList) {
        if (metadataList != null) {
            for (Metadata metadata : metadataList) {
                writeMetadata(null, gen, metadata);
            }
        } else {
            gen.writeNull();
        }
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
     * This method write a stream of RunningStatistics associated with a given start time to a JSON generator.  This
     * creates the JSON array, and does not expect one to be started outside the method.
     *
     * @param name The array name
     * @param gen The JSON generator to write them to
     * @param stats The Map of timestamps to RunningStatistics that will be written to the JSON generator
     * @param timestampFormatter How to format timestamps
     * @param sigFigs The number of significant figures to round to (using Banker's rounding / HALF_UP)
     * @param formatAsMillisSinceEpoch Should timestamps be in seconds from Epoch
     * @param adjustMillisWithServerOffset true to adjust timestamp to server timezone offset
     * @return The number of RunningStatistics written to the stream
     */
    public long generateStatisticsStream(String name, JsonGenerator gen, Map<Instant, RunningStatistics> stats,
                                         DateTimeFormatter timestampFormatter, short sigFigs,
                                         boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset) {
        if (name == null) {
            gen.writeStartArray();
        } else {
            gen.writeStartArray(name);
        }
        long count = 0;
        for(Instant begin : stats.keySet()) {
            writeRunningStatistics(null, gen, stats.get(begin), begin, formatAsMillisSinceEpoch,
                    adjustMillisWithServerOffset, timestampFormatter, sigFigs);
            count++;
        }
        gen.writeEnd();

        return count;
    }

    /**
     * Write out the FloatEvents to a JsonGenerator.
     *
     * @param gen The JsonGenerator
     * @param stream The EventStream
     * @param formatAsMillisSinceEpoch true to format timestamp as millis since epoch
     * @param adjustMillisWithServerOffset true to adjust timestamp to server timezone offset
     * @param timestampFormatter timestamp formatter
     * @param sigFigs The number of significant figures to round to (using Banker's rounding / HALF_UP)
     * @return The count of events written to the JsonGenerator
     * @throws IOException If Unable to generate the stream
     */
    public long generateFloatStream(JsonGenerator gen, EventStream<FloatEvent> stream,
            boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter,
            short sigFigs) throws IOException {
        long count = 0;
        FloatEvent event;
        while ((event = stream.read()) != null) {
            count++;
            writeFloatEvent(null, gen, event, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter, sigFigs);
        }
        return count;
    }

    /**
     * Write out the AnalyzedFloatEvents to a JsonGenerator.
     *
     * @param gen The JsonGenerator
     * @param stream The EventStream
     * @param formatAsMillisSinceEpoch true to format timestamp as millis since epoch
     * @param adjustMillisWithServerOffset true to adjust timestamp to server timezone offset
     * @param timestampFormatter timestamp formatter
     * @param sigFigs The number of significant figures to round to (using Banker's rounding / HALF_UP)
     * @return The count of events written to the JsonGenerator
     * @throws IOException If Unable to generate the stream
     */
    public long generateAnalyzedFloatStream(JsonGenerator gen, EventStream<AnalyzedFloatEvent> stream,
                                    boolean formatAsMillisSinceEpoch, boolean adjustMillisWithServerOffset, DateTimeFormatter timestampFormatter,
                                    short sigFigs) throws IOException {
        long count = 0;
        AnalyzedFloatEvent event;
        while ((event = stream.read()) != null) {
            count++;
            writeAnalyzedFloatEvent(null, gen, event, formatAsMillisSinceEpoch, adjustMillisWithServerOffset, timestampFormatter, sigFigs);
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
