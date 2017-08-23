package org.jlab.myquery;

import javax.naming.NamingException;
import org.jlab.mya.DataNexus;
import org.jlab.mya.Deployment;
import org.jlab.mya.nexus.PooledNexus;

/**
 *
 * @author ryans
 */
public class QueryWebService {

    protected static final PooledNexus OPS_NEXUS;
    protected static final PooledNexus DEV_NEXUS;

    static {
        try {
            OPS_NEXUS = new PooledNexus(Deployment.ops);
            DEV_NEXUS = new PooledNexus(Deployment.dev);
        } catch (NamingException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected final DataNexus getNexus(Deployment deployment) {
        DataNexus nexus;

        switch (deployment) {
            case dev:
                nexus = DEV_NEXUS;
                break;
            default:
                nexus = OPS_NEXUS;
        }

        return nexus;
    }
}
