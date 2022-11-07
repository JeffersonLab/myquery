package org.jlab.myquery;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.http.HttpServlet;
import org.jlab.mya.event.*;
import org.jlab.mya.stream.EventStream;

/**
 *
 * @author ryans
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
