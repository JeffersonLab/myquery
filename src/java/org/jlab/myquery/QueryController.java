package org.jlab.myquery;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServlet;
import org.jlab.mya.Event;
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

    private void writeInformationalEvent(JsonGenerator gen, Event event) {
        gen.write("t", event.getCode().name());
        if (event.getCode().isDisconnection()) {
            gen.write("x", true);
        }
    }

    public void writeIntEvent(String name, JsonGenerator gen, IntEvent event, boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }
        
        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestamp(), formatAsMillisSinceEpoch, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            gen.write("v", event.getValue());
        } else {
            writeInformationalEvent(gen, event);
        }

        gen.writeEnd();
    }

    public void writeFloatEvent(String name, JsonGenerator gen, FloatEvent event, boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter, DecimalFormat decimalFormatter) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }

        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestamp(), formatAsMillisSinceEpoch, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            // Round number (banker's rounding) and create String then create new BigDecimal to ensure no quotes are used in JSON
            gen.write("v", new BigDecimal(decimalFormatter.format(event.getValue())));
        } else {
            writeInformationalEvent(gen, event);
        }

        gen.writeEnd();
    }

    public void writeLabeledEnumEvent(String name, JsonGenerator gen, LabeledEnumEvent event, boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }
        
        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestamp(), formatAsMillisSinceEpoch, timestampFormatter);

        if (event.getCode() == EventCode.UPDATE) {
            gen.write("v", event.getLabel());
        } else {
            writeInformationalEvent(gen, event);
        }

        gen.writeEnd();
    }

    public void writeMultiStringEvent(String name, JsonGenerator gen, MultiStringEvent event, boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter) {
        if (name != null) {
            gen.writeStartObject(name);
        } else {
            gen.writeStartObject();
        }
        
        FormatUtil.writeTimestampJSON(gen, "d", event.getTimestamp(), formatAsMillisSinceEpoch, timestampFormatter);

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

    public void generateIntStream(JsonGenerator gen, IntEventStream stream,
            boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter) throws IOException {
        IntEvent event;
        while ((event = stream.read()) != null) {
            writeIntEvent(null, gen, event, formatAsMillisSinceEpoch, timestampFormatter);
        }
    }

    public void generateFloatStream(JsonGenerator gen, FloatEventStream stream,
            boolean formatAsMillisSinceEpoch, DateTimeFormatter timestampFormatter,
            DecimalFormat decimalFormatter) throws IOException {
        FloatEvent event;
        while ((event = stream.read()) != null) {
            writeFloatEvent(null, gen, event, formatAsMillisSinceEpoch, timestampFormatter, decimalFormatter);
        }
    }

    public void generateLabeledEnumStream(JsonGenerator gen,
            LabeledEnumStream stream, boolean formatAsMillisSinceEpoch,
            DateTimeFormatter timestampFormatter) throws IOException {
        LabeledEnumEvent event;
        while ((event = stream.read()) != null) {
            writeLabeledEnumEvent(null, gen, event, formatAsMillisSinceEpoch, timestampFormatter);
        }
    }

    public void generateMultiStringStream(JsonGenerator gen,
            MultiStringEventStream stream, boolean formatAsMillisSinceEpoch,
            DateTimeFormatter timestampFormatter) throws IOException {
        MultiStringEvent event;
        while ((event = stream.read()) != null) {
            writeMultiStringEvent(null, gen, event, formatAsMillisSinceEpoch, timestampFormatter);
        }
    }
}
