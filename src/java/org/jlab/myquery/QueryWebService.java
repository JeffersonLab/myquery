package org.jlab.myquery;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.naming.NamingException;
import org.jlab.mya.Deployment;
import org.jlab.mya.nexus.PooledNexus;

/**
 *
 * @author ryans
 */
public class QueryWebService {

    protected static final PooledNexus NEXUS;

    static {
        try {
            NEXUS = new PooledNexus(Deployment.ops);
        } catch (NamingException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public final static DateTimeFormatter DATE_TIME_NO_FRACTIONAL = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd'T'hh:mm:ss");

    public final static ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    public DecimalFormat getDecimalFormat(String v) {
        int vint = 2;

        if (v != null && !v.trim().isEmpty()) {
            vint = Integer.parseInt(v);

            if (vint > 16) {
                vint = 16;
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

    public DateTimeFormatter getInstantFormatter(String f) {
        DateTimeFormatter timestampFormatter;

        if (f == null || f.trim().isEmpty()) {
            timestampFormatter = DATE_TIME_NO_FRACTIONAL;
        } else {
            String pattern = "yyyy-MM-dd'T'hh:mm:ss";

            int fint = Integer.parseInt(f);

            if (fint > 9) {
                fint = 9;
            }

            if (fint > 0) {
                pattern = pattern + ".S";
                for (int i = 1; i < fint; i++) {
                    pattern = pattern + "S";
                }
            }

            timestampFormatter = DateTimeFormatter.ofPattern(pattern);
        }

        return timestampFormatter;
    }
}
