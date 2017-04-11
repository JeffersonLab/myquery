package org.jlab.myGet;


/**
 *
 * @author ryans
 */
public class Record {
    private final String date;
    private final String channelValue;

    public Record(String date, String channelValue) {
        this.date = date;
        this.channelValue = channelValue;
    }

    public String getDate() {
        return date;
    }

    public String getChannelValue() {
        return channelValue;
    }
}
