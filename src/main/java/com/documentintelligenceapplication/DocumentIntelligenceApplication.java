package com.documentintelligenceapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocumentIntelligenceApplication {

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        SpringApplication.run(DocumentIntelligenceApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.web.filter.CharacterEncodingFilter characterEncodingFilter() {
        org.springframework.web.filter.CharacterEncodingFilter filter = new org.springframework.web.filter.CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }
}
