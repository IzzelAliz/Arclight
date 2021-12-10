package org.apache.logging.log4j.core.lookup;

public class JndiLookup {

    public JndiLookup() {
        throw new RuntimeException("Jndi lookup disabled due to security restrictions");
    }
}
