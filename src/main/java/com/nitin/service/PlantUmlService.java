package com.nitin.service;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PlantUmlService {

    @Value("${plantuml.output-directory}")
    private String outputDirectory;

    @Value("${plantuml.image-format}")
    private String imageFormat;

    public String generateDiagram(String umlSource) throws IOException {
        String validatedUml = validateAndFixUmlSyntax(umlSource);
        Path outputPath = Paths.get(outputDirectory);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "diagram_" + timestamp +"." + imageFormat.toLowerCase();
        Path fullPath = outputPath.resolve(fileName);

        SourceStringReader reader = new SourceStringReader(validatedUml);

        FileFormat format = getFileFormat(imageFormat);
        FileFormatOption option = new FileFormatOption(format);
        try (FileOutputStream fos = new FileOutputStream(fullPath.toFile())){
            reader.outputImage(fos, option);
        }
        return fullPath.toString();
    }

    public byte[] generateDiagramBytes(String umlSource) throws IOException {
        SourceStringReader reader = new SourceStringReader(umlSource);
        FileFormat format = getFileFormat(imageFormat);
        FileFormatOption option = new FileFormatOption(format);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            reader.outputImage(baos, option);
            return baos.toByteArray();
        }
    }

    private FileFormat getFileFormat(String format) {
        return switch (format.toUpperCase()) {
            case "SVG" -> FileFormat.SVG;
            case "PDF" -> FileFormat.PDF;
            default -> FileFormat.PNG;
        };
    }

    public String validateAndFixUmlSyntax(String umlSource) {
        String trimmed = umlSource.trim();

        if(!trimmed.startsWith("@startuml")){
            trimmed = "@startuml\n" + trimmed;
        }
        if (!trimmed.endsWith("@enduml")){
            trimmed = trimmed + "\n@enduml";
        }
        return trimmed;
    }
}
