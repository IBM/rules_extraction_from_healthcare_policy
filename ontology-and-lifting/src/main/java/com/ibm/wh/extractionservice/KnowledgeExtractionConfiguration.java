package com.ibm.wh.extractionservice;

import com.ibm.wh.extractionservice.ontology.DomainOntology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
public class KnowledgeExtractionConfiguration {

    public KnowledgeExtractionConfiguration(DomainOntology domainOntology) {
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**");
            }
        };
    }


    @Configuration
    @EnableSwagger2
    static class SpringfoxConfiguration {

        @Bean
        public Docket docket() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .select()
                    .apis(RequestHandlerSelectors.basePackage(getClass().getPackage().getName()))
                    .paths(PathSelectors.any())
                    .build();
        }

    }


}