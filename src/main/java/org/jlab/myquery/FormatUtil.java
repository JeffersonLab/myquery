package org.jlab.myquery;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
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
        int vInt = 6;

        if (v != null && !v.trim().isEmpty()) {
            vInt = Integer.parseInt(v);

            if (vInt > 9) {
                vInt = 9;
            }
        }

        String decimalPattern = "0";

        if (vInt > 0) {
            decimalPattern = decimalPattern + ".#";
            for (int i = 1; i < vInt; i++) {
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
            boolean formatAsMillisSinceEpoch, boolean formatAsMillisPlusLocalOffset, DateTimeFormatter formatter) {
        if (formatAsMillisSinceEpoch || formatAsMillisPlusLocalOffset) {
            long millis = (timestamp.getEpochSecond() * 1000)
                    + (timestamp.getLong(ChronoField.MILLI_OF_SECOND));

            if(formatAsMillisPlusLocalOffset) {
                millis = getLocalTime(millis);
            }

            gen.write(name, millis);
        } else {
            gen.write(name,
                    timestamp.atZone(DEFAULT_ZONE).format(
                            formatter));
        }
    }

    /**
     * Return then number of milliseconds since Jan 01 1970 plus a local time offset.
     * <p>
     * Web Browser JavaScript environments do not have a timezone database such as IANA.
     * In a web browser environment your application must either include (download) a timezone database library such as
     * "Moment Timezone" or else have the server do any unix time to local timezone shifting for you.
     * This is the later.
     * </p><p>
     * Generally software should store and pass around UTC time.  Then applications can format the time into any
     * timezone.  This is tricky on the web due to limited access on browsers to IANA data.
     * </p><p>
     * Why not just use browser's local time?  Because if you are looking at historic data the browser has no idea
     * where daylight savings changes occurred in the past, it only knows current timezone offset of host OS at the
     * current instant.  Also, it is possible the user browser is outside our local timezone
     * (though this is a separate issue).
     * </p><p>
     * Why not just use UTC?  Well, we kind of are with this approach.   Users query for data in local time, but we
     * offset the result such that browsers can interpret the results as if it were UTC.  It isn't really UTC though
     * because it is shifted for local time.
     * </p>
     * <p>
     * What about duplicate hour (1:00 AM EST vs 1:00 AM EDT)? There is no way to distinguish between the two in UTC as
     * there is no timezone offset so two points will show up with the same timestamp (result order is correct, but
     * otherwise cannot be sorted).  This is probably the biggest drawback of server-side shifting is that an otherwise
     * monotonically increasing series can step backwards for one hour then continue.   Similarly in the Spring the
     * "skipped" hour can also show up on a monotonically increasing series in a possibly unexpected way
     * (a literally skipped hour).  It probably is at least graphically doing what many of us envision daylight savings
     * to be doing so may be okay depending on your use-case.
     * </p>
     * @param millisSinceEpoch The milliseconds since Epoch in UTC
     * @return milliseconds since Epoch adjusted with local time offset
     */
    public static long getLocalTime(long millisSinceEpoch) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(millisSinceEpoch));
        // Here is where timezone database is used to lookup offsets for both zone and day light savings
        long localOffset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);

        // Example: Nov. 3 01:00 2019 EST is -05:00 or 1572757200000 milliseconds from Epoch
        // But in UTC that would be Nov. 3 06:00 2019.  So we can adjust it back by adding the -5 hour EST offset
        // Note: Nov. 3 01:00 2019 EDT is -04:00.   At 02:00 time "falls back" to 01:00 EST.

        return cal.getTimeInMillis() + localOffset;
    }
}
