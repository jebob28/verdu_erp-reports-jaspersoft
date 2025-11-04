package reports.verdu_erp.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtro para auditoria de requisições HTTP
 */
@Component
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true")
@Order(2)
public class AuditFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuditFilter.class);
    
    @Value("${logs.api.url:http://logs:8090/log/criar}")
    private String logsApiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Endpoints que devem ser ignorados na auditoria
    private final List<String> skipEndpoints = Arrays.asList(
        "/health", "/actuator", "/swagger", "/v3/api-docs", "/favicon.ico"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Verificar se deve pular auditoria
        if (shouldSkipAudit(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // Wrapping para capturar conteúdo
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            try {
                logAuditEvent(wrappedRequest, wrappedResponse, duration);
            } catch (Exception e) {
                logger.error("Erro ao registrar evento de auditoria", e);
            }
            
            // Importante: copiar o conteúdo de volta para a resposta
            wrappedResponse.copyBodyToResponse();
        }
    }

    private boolean shouldSkipAudit(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return skipEndpoints.stream().anyMatch(uri::contains);
    }

    private void logAuditEvent(ContentCachingRequestWrapper request, 
                              ContentCachingResponseWrapper response, 
                              long duration) {
        try {
            // Extrair informações da requisição
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String fullUri = queryString != null ? uri + "?" + queryString : uri;
            
            // Capturar corpo da requisição
            String requestBody = getRequestBody(request);
            
            // Capturar informações do ator
            String actor = extractActor(request);
            
            // Informações adicionais
            String userAgent = request.getHeader("User-Agent");
            String clientIp = getClientIpAddress(request);
            String correlationId = (String) request.getAttribute("correlationId");
            
            // Status da resposta
            int statusCode = response.getStatus();
            
            // Construir payload para serviço de logs
            Map<String, Object> detalhes = new HashMap<>();
            detalhes.put("query_parameters", queryString);
            detalhes.put("request_body", requestBody);
            detalhes.put("client_ip", clientIp);
            detalhes.put("user_agent", userAgent);
            detalhes.put("correlation_id", correlationId);
            detalhes.put("timestamp", LocalDateTime.now().toString());

            Map<String, Object> payload = new HashMap<>();
            payload.put("usuario", actor);
            payload.put("acao", method);
            payload.put("tabela", uri);
            payload.put("service", "reports-jaspersoft");
            payload.put("actor", actor);
            payload.put("resource", uri);
            payload.put("method", method);
            payload.put("uri", fullUri);
            payload.put("statusCode", statusCode);
            payload.put("durationMs", duration);
            payload.put("detalhesJson", objectMapper.writeValueAsString(detalhes));

            try {
                restTemplate.postForEntity(logsApiUrl, payload, String.class);
            } catch (Exception httpEx) {
                logger.warn("Falha ao enviar auditoria via HTTP: {}", httpEx.getMessage());
            }
            
            logger.debug("Evento de auditoria registrado: {} {} - Status: {} - Duração: {}ms", 
                        method, uri, statusCode, duration);
                        
        } catch (Exception e) {
            logger.error("Erro ao processar auditoria", e);
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, StandardCharsets.UTF_8);
                // Limitar tamanho do corpo para evitar logs muito grandes
                return body.length() > 5000 ? body.substring(0, 5000) + "..." : body;
            }
        } catch (Exception e) {
            logger.warn("Erro ao capturar corpo da requisição", e);
        }
        return null;
    }

    private String extractActor(HttpServletRequest request) {
        // Tentar extrair do header X-Actor
        String actor = request.getHeader("X-Actor");
        
        if (actor == null || actor.trim().isEmpty()) {
            // Tentar extrair de JWT ou outros headers de autenticação
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // Aqui você pode implementar a extração do usuário do JWT
                actor = "jwt_user"; // Placeholder
            } else {
                actor = "anonymous";
            }
        }
        
        return actor;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String extractResourceId(String uri) {
        // Tentar extrair ID do recurso da URI
        String[] parts = uri.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches("\\d+")) {
                return parts[i];
            }
        }
        return null;
    }
}
