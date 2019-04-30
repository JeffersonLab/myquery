package org.jlab.myquery;

import javax.naming.NamingException;

import org.jlab.mya.DataNexus;
import org.jlab.mya.nexus.PooledNexus;

import java.util.concurrent.ConcurrentHashMap;


/**
 * @author ryans
 */
public class QueryWebService {

    protected static final ConcurrentHashMap<String, PooledNexus> nexusMap = new ConcurrentHashMap<>();
    static {
        for(String d : DataNexus.getDeploymentNames()) {
            try {
                nexusMap.putIfAbsent(d, new PooledNexus(d));
            } catch (NamingException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    protected final DataNexus getNexus(String deployment) {
        if (!nexusMap.containsKey(deployment)) {
            throw new IllegalArgumentException("Unrecognized deployment - " + deployment);
        }

        return nexusMap.get(deployment);
    }
}
