package org.jlab.myquery;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import javax.json.stream.JsonGenerator;
import org.jlab.mya.TimeUtil;

/**
 *
 * @author ryans
 */
public class FormatUtil {

    public final static DateTimeFormatter DATE_TIME_NO_FRACTIONAL = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd'T'hh:mm:ss");

    public final static ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    public static DecimalFormat getDecimalFormat(String v) {
        int vint = 6;

        if (v != null && !v.trim().isEmpty()) {
            vint = Integer.parseInt(v);

            if (vint > 9) {
                vint = 9;
            }
        }

        String decimalPattern = "0";

        if (vint > 0) {
            decimalPattern = decimalPattern + ".#";
            for (int i = 1; i < vint; i++) {
                decimalPattern = decimalPattern + "#";
            }
        }

        //System.out.println(decimalPattern);
        // Not thread safe
        return new DecimalFormat(decimalPattern);
    }

    public static DateTimeFormatter getInstantFormatter(String f) {
        DateTimeFormatter timestampFormatter;

        if (f == null || f.trim().isEmpty()) {
            timestampFormatter = DATE_TIME_NO_FRACTIONAL;
        } else {
            String pattern = TimeUtil.getFractionalSecondsTimestampFormat(Integer.parseInt(f));

            timestampFormatter = DateTimeFormatter.ofPattern(pattern);
        }

        return timestampFormatter;
    }

    public static void writeTimestampJSON(JsonGenerator gen, String name, Instant timestamp,
            boolean formatAsMillisSinceEpoch, DateTimeFormatter formatter) {
        if (formatAsMillisSinceEpoch) {
            gen.write(name, (timestamp.getEpochSecond() * 1000)
                    + (timestamp.getLong(ChronoField.MILLI_OF_SECOND)));
        } else {
            gen.write(name,
                    timestamp.atZone(DEFAULT_ZONE).format(
                            formatter));
        }
    }
}
