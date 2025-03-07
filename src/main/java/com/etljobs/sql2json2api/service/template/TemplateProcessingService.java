package com.etljobs.sql2json2api.service.template;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;
import com.etljobs.sql2json2api.model.ApiEndpointInfo;
import com.etljobs.sql2json2api.model.ApiTemplateResult;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for processing Freemarker templates.
 */
@Service
@Slf4j
public class TemplateProcessingService {
    
    @Value("${app.template.directory}")
    private String templateDirectory;
    
    private final Configuration freemarkerConfig;
    private final TemplateMetadataService metadataService;
    
    public TemplateProcessingService(Configuration freemarkerConfig, TemplateMetadataService metadataService) {
        this.freemarkerConfig = freemarkerConfig;
        this.metadataService = metadataService;
    }
    
    /**
     * Reads the content of a template file.
     * 
     * @param templateName The name of the template file
     * @return The content of the template file as a String
     * @throws IOException If the template file cannot be read
     */
    public String getTemplateContent(String templateName) throws IOException {
        Resource resource = new ClassPathResource(templateDirectory + "/" + templateName);
        byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Processes a template with a single row of data.
     * 
     * @param templateName The name of the template to process
     * @param rowData The data for a single row from SQL results
     * @return ApiTemplateResult containing the processed JSON and API endpoint information
     * @throws TemplateProcessingException If template processing fails
     */
    public ApiTemplateResult processTemplate(String templateName, Map<String, Object> rowData) {
        try {
            log.debug("Processing template {} with data: {}", templateName, rowData);
            
            // Get template content for metadata extraction
            String templateContent = getTemplateContent(templateName);
            
            // Extract API endpoint information from template
            ApiEndpointInfo endpointInfo = metadataService.extractMetadataFromTemplate(templateContent);
            
            // Process route placeholders with row data
            endpointInfo.setRoute(processRoutePlaceholders(endpointInfo.getRoute(), rowData));
            
            // Prepare data model for template processing
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("result", rowData); // Single row data
            
            // Process template to generate JSON
            Template template = new Template(templateName, new StringReader(templateContent), freemarkerConfig);
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            String jsonPayload = writer.toString();
            
            log.debug("Template processing completed. Generated JSON: {}", jsonPayload);
            
            return new ApiTemplateResult(jsonPayload, endpointInfo);
            
        } catch (IOException | TemplateException e) {
            throw new TemplateProcessingException("Failed to process template: " + templateName, e);
        }
    }
    
    /**
     * Process route placeholders with actual values from row data.
     * Example: "/api/users/${result.id}" -> "/api/users/123"
     * 
     * @param route The route with placeholders
     * @param rowData The data to fill the placeholders
     * @return The processed route with placeholders replaced by actual values
     */
    private String processRoutePlaceholders(String route, Map<String, Object> rowData) {
        String processedRoute = route;
        
        // Simple placeholder processing
        for (Map.Entry<String, Object> entry : rowData.entrySet()) {
            String placeholder = "${result." + entry.getKey() + "}";
            if (processedRoute.contains(placeholder)) {
                processedRoute = processedRoute.replace(placeholder, 
                        entry.getValue() != null ? entry.getValue().toString() : "");
            }
        }
        
        return processedRoute;
    }
}