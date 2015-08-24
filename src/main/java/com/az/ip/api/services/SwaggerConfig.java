package com.az.ip.api.services;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Created by magnus on 19/08/15.
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket api(){
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(path -> path.toLowerCase().startsWith("/api/"))
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
                "AZ IP REST API",
                "This is a description of the API...",
                "v1.0.6",
                "API TOS",
                "me@wherever.com",
                "API License",
                "https://raw.githubusercontent.com/swagger-api/swagger-ui/master/LICENSE"
        );

        return apiInfo;
    }
}
