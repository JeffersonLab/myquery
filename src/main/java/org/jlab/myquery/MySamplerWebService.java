package org.jlab.myquery;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.mya.ExtraInfo;
import org.jlab.mya.Metadata;
import org.jlab.mya.event.Event;
import org.jlab.mya.nexus.DataNexus;
import org.jlab.mya.stream.*;

/**
 * This class provides features similar to the mySampler command line utility.
 *
 * @author adamc
 */
public class MySamplerWebService extends QueryWebService {
  private static final Logger LOGGER = Logger.getLogger((MySamplerWebService.class.getName()));
  private final DataNexus nexus;

  public MySamplerWebService(String deployment) {
    nexus = getNexus(deployment);
  }

  public Metadata findMetadata(String c) throws SQLException {
    return nexus.findMetadata(c);
  }

  public List<ExtraInfo> findExtraInfo(Metadata metadata, String type, Instant begin, Instant end)
      throws SQLException {
    return nexus.findExtraInfo(metadata, type, begin, end);
  }

  @SuppressWarnings("unchecked")
  public <T extends Event> MySamplerStream<T> openEventStream(
      Metadata<T> metadata,
      Instant begin,
      long intervalMillis,
      long sampleCount,
      boolean updatesOnly,
      MySamplerStream.Strategy strategy)
      throws SQLException, UnsupportedOperationException {

    PointWebService pws = new PointWebService(nexus.getDeployment());
    T priorEvent = (T) pws.findEvent(metadata, updatesOnly, begin, true, false, false);

    Instant end = begin.plusMillis(intervalMillis * (sampleCount - 1));

    EventStream<T> stream = null;
    MySamplerStream<T> out;
    try {
      if (strategy == MySamplerStream.Strategy.STREAM) {
        stream =
            nexus.openEventStream(
                metadata, begin, end, DataNexus.IntervalQueryFetchStrategy.STREAM, updatesOnly);
        out =
            MySamplerStream.getMySamplerStream(
                stream,
                begin,
                intervalMillis,
                sampleCount,
                priorEvent,
                updatesOnly,
                metadata.getType());
      } else if (strategy == MySamplerStream.Strategy.N_QUERIES) {
        out =
            MySamplerStream.getMySamplerStream(
                begin,
                intervalMillis,
                sampleCount,
                updatesOnly,
                metadata.getType(),
                nexus,
                metadata);
      } else {
        throw new IllegalArgumentException(("Unsupported strategy - " + strategy));
      }

    } catch (Exception ex) {
      if (stream != null) {
        try {
          stream.close();
        } catch (IOException closeIssue) {
          LOGGER.log(Level.SEVERE, "Could not close stream", ex);
        }
      }
      throw ex;
    }
    return out;
  }
}
