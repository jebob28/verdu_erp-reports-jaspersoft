package reports.verdu_erp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import reports.verdu_erp.dto.ReportUploadResponseDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler global para tratamento de exceções
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ReportUploadResponseDTO> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(ReportUploadResponseDTO.error("Arquivo muito grande. Tamanho máximo permitido excedido."));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Argumento inválido");
        error.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Erro interno do servidor");
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Erro inesperado");
        error.put("message", "Ocorreu um erro inesperado. Tente novamente.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}