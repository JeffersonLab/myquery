package org.jlab.myquery;

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
}
