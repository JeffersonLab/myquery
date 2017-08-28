package org.jlab.myquery;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServlet;
import org.jlab.mya.EventCode;
import org.jlab.mya.event.FloatEvent;
import org.jlab.mya.event.IntEvent;
import org.jlab.mya.event.LabeledEnumEvent;
import org.jlab.mya.event.MultiStringEvent;
import org.jlab.mya.stream.FloatEventStream;
import org.jlab.mya.stream.IntEventStream;
import org.jlab.mya.stream.MultiStringEventStream;
import org.jlab.mya.stream.wrapped.LabeledEnumStream;

/**
 *
 * @author ryans
 */
public class QueryController extends HttpServlet {

    public void writeIntEvent(JsonGenerator gen, IntEvent event, boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter) {
        gen.writeStartObject();
        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestamp(), formatAsMillisSinceEpoch, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            gen.write("v", event.getValue());
        } else {
            gen.write("v", event.getCode().name());
        }

        gen.writeEnd();
    }

    public void writeFloatEvent(JsonGenerator gen, FloatEvent event, boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter, DecimalFormat decimalFormatter) {
        gen.writeStartObject();
        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestamp(), formatAsMillisSinceEpoch, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            // Round number (banker's rounding) and create String then create new BigDecimal to ensure no quotes are used in JSON
            gen.write("v", new BigDecimal(decimalFormatter.format(event.getValue())));
        } else {
            gen.write("v", event.getCode().name());
        }

        gen.writeEnd();
    }

    public void writeLabeledEnumEvent(JsonGenerator gen, LabeledEnumEvent event, boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter) {
        gen.writeStartObject();
        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestamp(), formatAsMillisSinceEpoch, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            gen.write("v", event.getLabel());
        } else {
            gen.write("v", event.getCode().name());
        }

        gen.writeEnd();
    }

    public void writeMultiStringEvent(JsonGenerator gen, MultiStringEvent event, boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter) {
        gen.writeStartObject();
        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestamp(), formatAsMillisSinceEpoch, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            String[] values = event.getValue();
            gen.writeStartArray("v");
            for (String value : values) {
                gen.write(value);
            }
            gen.writeEnd();
        } else {
            gen.write("v", event.getCode().name());
        }

        gen.writeEnd();
    }

    public void generateIntStream(JsonGenerator gen, IntEventStream stream,
            boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter) throws IOException {
        IntEvent event;
        while ((event = stream.read()) != null) {
            writeIntEvent(gen, event, formatAsMillisSinceEpoch, timestampFormatter);
        }
    }

    public void generateFloatStream(JsonGenerator gen, FloatEventStream stream,
            boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter,
            DecimalFormat decimalFormatter) throws IOException {
        FloatEvent event;
        while ((event = stream.read()) != null) {
            writeFloatEvent(gen, event, formatAsMillisSinceEpoch, timestampFormatter, decimalFormatter);
        }
    }

    public void generateLabeledEnumStream(JsonGenerator gen,
            LabeledEnumStream stream, boolean formatAsMillisSinceEpoch,
            DateTimeFormatter timestampFormatter) throws IOException {
        LabeledEnumEvent event;
        while ((event = stream.read()) != null) {
            writeLabeledEnumEvent(gen, event, formatAsMillisSinceEpoch, timestampFormatter);
        }
    }

    public void generateMultiStringStream(JsonGenerator gen,
            MultiStringEventStream stream, boolean formatAsMillisSinceEpoch,
            DateTimeFormatter timestampFormatter) throws IOException {
        MultiStringEvent event;
        while ((event = stream.read()) != null) {
            writeMultiStringEvent(gen, event, formatAsMillisSinceEpoch, timestampFormatter);
        }
    }
}
