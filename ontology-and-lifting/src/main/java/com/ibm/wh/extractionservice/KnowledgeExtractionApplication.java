package com.ibm.wh.extractionservice;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@EnableCaching
public class KnowledgeExtractionApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(KnowledgeExtractionApplication.class)
                .build()
                .run(args);
    }

}
