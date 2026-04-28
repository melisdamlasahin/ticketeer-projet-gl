package ticket_train.ticketeer.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import ticket_train.ticketeer.dto.ApiErrorResponse;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation de la requete echouee",
                fieldErrors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        ApiErrorResponse response = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getReason() != null ? ex.getReason() : "Erreur",
                Map.of()
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
