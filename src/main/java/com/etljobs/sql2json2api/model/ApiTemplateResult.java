package com.etljobs.sql2json2api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result of processing a template for a single row of SQL data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiTemplateResult {
    
    /**
     * The JSON payload generated from the template
     */
    private String jsonPayload;
    
    /**
     * The API endpoint information extracted from the template metadata
     */
    private ApiEndpointInfo endpointInfo;
}