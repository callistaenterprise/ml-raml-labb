package com.az.ip.api.raml;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.ApplicationPath;

@Component
@ApplicationPath("/raml-api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {

        String jaxRsLoggerName = "jax-rs";
        Logger jaxRsLogger = LoggerFactory.getLogger(jaxRsLoggerName);
        if (jaxRsLogger.isInfoEnabled()) {
            register(new LoggingFilter(java.util.logging.Logger.getLogger(jaxRsLoggerName), jaxRsLogger.isDebugEnabled()));
        }

        register(StudiesResourceImpl.class);
        register(DoctorsResourceImpl.class);
        register(PatientsResourceImpl.class);
    }

}
