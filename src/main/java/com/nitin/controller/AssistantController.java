package com.nitin.controller;

import com.nitin.dto.DesignRequest;
import com.nitin.dto.DesignResponse;
import com.nitin.dto.Conversation;
import com.nitin.dto.PlantUmlRequest;
import com.nitin.service.DesignService;
import com.nitin.service.PlantUmlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.nitin.service.HealthCheckService;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Controller
public class AssistantController {
    @Autowired
    private DesignService designService;

    @Autowired
    private PlantUmlService plantUmlService;

    @Autowired
    private HealthCheckService healthCheckService;

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
    @GetMapping("/diagram/{filename}")
    public ResponseEntity<Resource> getDiagram(@PathVariable String filename) throws IOException {
        String filePath = "./uml-diagram/" + filename;
        File file = new File(filePath);

        if(!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource =  new FileSystemResource(file);
        String contentType = Files.probeContentType(Paths.get(filePath));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    @PostMapping("/api/plantuml/generate")
    @ResponseBody
    public ResponseEntity<byte[]> generatePlantUmlDiagram(@RequestBody PlantUmlRequest request) {
        try {
            String validateUml = plantUmlService.validateAndFixUmlSyntax(request.getUmlCode());
            byte[] diagramBytes = plantUmlService.genereateDiagramBytes(validateUml);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(diagramBytes);
        } catch(IOException e) {
            return ResponseEntity.internalServerError().build();
        }

    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Boolean>> healthCheck() {
        boolean isRunning = healthCheckService.isLlamaRunning();
        return ResponseEntity.ok(Map.of("isLlamaRunning", isRunning));
    }
}