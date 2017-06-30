package org.jlab.myGet;

import java.util.List;

/**
 *
 * @author ryans
 */
public interface SpanService {

    public List<Record> getRecordList(String c, String b, String e,
            String l, String p, String m, String M, String d, String f,
            String s) throws
            Exception;
}
