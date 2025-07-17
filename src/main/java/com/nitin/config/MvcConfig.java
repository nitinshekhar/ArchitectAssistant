package com.nitin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(MvcConfig.class);

    @Value("${plantuml.output-directory}")
    private String storageLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation = "file:" + (storageLocation.endsWith("/") ? storageLocation : storageLocation + "/");
        log.info("Mapping URL path /diagram/** to physical path {}", resourceLocation);
        registry.addResourceHandler("/diagram/**").addResourceLocations(resourceLocation);
    }
}