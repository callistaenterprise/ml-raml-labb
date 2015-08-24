package com.az.ip.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import javax.annotation.PreDestroy;
import javax.servlet.Filter;

@SpringBootApplication
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final String VERSION = "v1.0.0-M2";

    @Bean
    public Filter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(5120);
        return filter;
    }

    @PreDestroy
    public void cleanUp() throws Exception {
        LOG.info("### AZ-IP-SERVER {} stops", VERSION);
    }

    public static void main(String[] args) {
        LOG.info("### AZ-IP-SERVER {} starts", VERSION);
        SpringApplication.run(Application.class, args);
    }
}
