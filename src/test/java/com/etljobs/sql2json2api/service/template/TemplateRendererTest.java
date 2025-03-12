package com.etljobs.sql2json2api.service.template;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
class TemplateRendererTest {

    @Mock
    private Configuration freemarkerConfig;
    
    @Mock
    private Template mockTemplate;
    
    private TemplateRenderer templateRenderer;
    
    @BeforeEach
    void setUp() throws Exception {
        templateRenderer = new TemplateRenderer(freemarkerConfig);
        
        // Configure le mock pour retourner notre template
        when(freemarkerConfig.getTemplate(anyString())).thenReturn(mockTemplate);
    }
    
    @Test
    void renderTemplate_ShouldProcessTemplateWithData() throws Exception {
        // Arrange
        String templateName = "test.ftlh";
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("name", "John");
        dataModel.put("age", 30);
        
        // Configure le comportement du template pour écrire un résultat
        doAnswer(invocation -> {
            StringWriter writer = invocation.getArgument(1);
            writer.write("{\"name\":\"John\",\"age\":30}");
            return null;
        }).when(mockTemplate).process(eq(dataModel), any(StringWriter.class));
        
        // Act
        String result = templateRenderer.renderTemplate(templateName, dataModel);
        
        // Assert
        assertEquals("{\"name\":\"John\",\"age\":30}", result);
        verify(freemarkerConfig).getTemplate(templateName);
        verify(mockTemplate).process(eq(dataModel), any(StringWriter.class));
    }
    
    @Test
    void renderTemplate_ShouldThrowException_WhenTemplateProcessingFails() throws Exception {
        // Arrange
        String templateName = "test.ftlh";
        Map<String, Object> dataModel = new HashMap<>();
        
        // Configure le template pour lancer une exception
        doAnswer(invocation -> {
            throw new TemplateException("Template error", null);
        }).when(mockTemplate).process(any(), any(StringWriter.class));
        
        // Act & Assert
        assertThrows(TemplateProcessingException.class, () -> {
            templateRenderer.renderTemplate(templateName, dataModel);
        });
    }
}