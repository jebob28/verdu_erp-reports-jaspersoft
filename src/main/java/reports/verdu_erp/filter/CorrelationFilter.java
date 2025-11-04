package reports.verdu_erp.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro para gerenciar correlation ID nas requisições
 */
@Component
@Order(1)
public class CorrelationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Obter ou gerar correlation ID
            String correlationId = getOrGenerateCorrelationId(httpRequest);
            
            // Adicionar ao MDC para logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Adicionar ao request para uso posterior
            httpRequest.setAttribute(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Adicionar ao response header
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            logger.debug("Processing request with correlation ID: {}", correlationId);
            
            chain.doFilter(request, response);
            
        } finally {
            // Limpar MDC
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        // Tentar obter do header X-Correlation-ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            // Tentar obter do header X-Request-ID
            correlationId = request.getHeader(REQUEST_ID_HEADER);
        }
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            // Gerar novo UUID se não encontrado
            correlationId = UUID.randomUUID().toString();
        }
        
        return correlationId;
    }
}