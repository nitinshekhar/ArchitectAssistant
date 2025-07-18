package com.nitin.controller;

import com.nitin.dto.DesignRequest;
import com.nitin.dto.DesignResponse;
import com.nitin.dto.PlantUmlRequest;
import com.nitin.service.C4ModelService;
import com.nitin.service.DesignService;
import com.nitin.service.HealthCheckService;
import com.nitin.service.PlantUmlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Controller
public class AssistantController {

    private static final Logger log = LoggerFactory.getLogger(AssistantController.class);
    @Autowired
    private DesignService designService;

    @Autowired
    private PlantUmlService plantUmlService;

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    private C4ModelService c4ModelService;

    @Value("${plantuml.output-directory}")
    private String storageLocation;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/design")
    public String generateDesign(@ModelAttribute("request") DesignRequest request, Model model){
        DesignResponse response = designService.generateDesign(request.getRequest(), request.getConversationHistory());
        model.addAttribute("response", response);
        return "result";
    }

    @PostMapping("/api/design")
    public ResponseEntity<DesignResponse> generateDesignApi(@RequestBody DesignRequest request) {
        DesignResponse response = designService.generateDesign(request.getRequest(), request.getConversationHistory());
        return ResponseEntity.ok(response);
    }

    @Autowired
    private ApplicationContext applicationContext;

    @GetMapping("/diagram/{filename}")
    public ResponseEntity<Resource> getDiagram(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(storageLocation).resolve(filename).normalize();
            log.debug("Attempting to serve diagram from path: {}", filePath.toAbsolutePath());

            Resource resource = new FileSystemResource(filePath.toFile());

            if (!resource.exists()) {
                log.warn("Diagram file not found at resolved path: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            log.info("Successfully served diagram: {}", filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e){
            log.error("Could not determine file type for diagram: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/plantuml/generate")
    @ResponseBody
    public ResponseEntity<byte[]> generatePlantUmlDiagram(@RequestBody PlantUmlRequest request) {
        try {
            String validateUml = c4ModelService.validateAndEnhanceC4Syntax(request.getUmlCode());
            byte[] diagramBytes = plantUmlService.generateDiagramBytes(validateUml);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(diagramBytes);
        } catch(IOException e) {
            log.error("Error generating PlantUML diagram:", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Boolean>> healthCheck() {
        boolean isRunning = healthCheckService.isLlamaRunning();
        return ResponseEntity.ok(Map.of("isLlamaRunning", isRunning));
    }
}