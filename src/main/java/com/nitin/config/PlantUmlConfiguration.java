package com.nitin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Configuration
public class PlantUmlConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PlantUmlConfiguration.class);
    private static final String PLANTUML_C4_INCLUDES_PATTERN = "classpath:plantuml/c4/*.puml";

    @PostConstruct
    public void setPlantUmlIncludePath() {
        try {
            Path tempDir = Files.createTempDirectory("plantuml-includes-");
            tempDir.toFile().deleteOnExit(); // Clean up on shutdown

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(PLANTUML_C4_INCLUDES_PATTERN);

            if (resources.length == 0) {
                log.warn("No PlantUML C4 include files found at {}. Local diagram generation may fail.", PLANTUML_C4_INCLUDES_PATTERN);
                return;
            }

            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    Files.copy(inputStream, tempDir.resolve(resource.getFilename()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.setProperty("plantuml.include.path", tempDir.toAbsolutePath().toString());
            log.info("Successfully set PlantUML include path to temporary directory: {}", tempDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("Could not set up PlantUML local includes. Local C4 diagram generation may fail.", e);
        }
    }
}