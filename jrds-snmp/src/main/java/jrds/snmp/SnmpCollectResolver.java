package jrds.snmp;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.OID;

import jrds.CollectResolver;

public class SnmpCollectResolver implements CollectResolver<OID> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final OID NULLOID = new OID(new int[] {0,0});

    /**
     * An OID mapping cache, filled in {@link SnmpConfigurator#configure(jrds.PropertiesManager)}
     */
    static final Map<String, OID> oidmapping = new HashMap<>();

    @Override
    public OID resolve(String collectKey) {
        try {
            if (oidmapping.containsKey(collectKey)) {
                return oidmapping.get(collectKey);
            } else {
                OID collect = new OID(collectKey);
                if (NULLOID.equals(collect) || ! collect.isValid()) {
                    throw new IllegalArgumentException("Resolved as invalid: '" + collect + "'");
                }
                oidmapping.put(collectKey, collect);
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Missing from OID mappings: %s=%s", collectKey, collect.toDottedString()));
                }
                return collect;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

}
