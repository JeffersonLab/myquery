package org.jlab.myquery;

import org.jlab.mya.RunningStatistics;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A class for containing the RunningStatistics data on multiple channels over periods of time.  This class is intended
 * to be used to represent the situation where a contiguous channel history has been split into bins and summary
 * statistics were calculated for each bin.  This class supports tracking multiple channels with different binning, but
 * you will probably be best served by keeping all of the bin sizes the same.
 * @author adamc
 */
public class MyStatsResults {
    private final Map<String, Map<Instant, RunningStatistics>> statMap;

    public MyStatsResults() {
        statMap = new TreeMap<>();
    }

    /**
     * Add statistics information about a channel for a given start time.
     * @param channel The name of PV/channel being updates
     * @param timestamp The timestamp for the beginning of the time period for which the statistics are valid
     * @param stats The object containing the statistics values.
     */
    public void add(String channel, Instant timestamp, RunningStatistics stats) {
        statMap.putIfAbsent(channel, new TreeMap<>());
        statMap.get(channel).put(timestamp, stats);
    }

    /**
     * Get the binned statistics for a given channel
     * @param channel Then name of the channel to fetch
     * @return A map that pairs the statistics with the bin start time for that channel.
     */
    public Map<Instant, RunningStatistics> get(String channel) {
        return statMap.get(channel);
    }

    /**
     * Get the set of channels currently tracked by this object.
     * @return A set of channel names
     */
    public Set<String> getChannels() {
        return statMap.keySet();
    }
}
