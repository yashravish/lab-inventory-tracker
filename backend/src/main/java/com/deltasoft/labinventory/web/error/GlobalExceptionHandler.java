package com.deltasoft.labinventory.web.error;

import com.deltasoft.labinventory.domain.Reagent;
import com.deltasoft.labinventory.service.ReagentService;
import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = base(HttpStatus.BAD_REQUEST, "Validation failed");
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.put(fe.getField(), fe.getDefaultMessage());
        }
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ReagentService.NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ReagentService.NotFoundException ex) {
        return new ResponseEntity<>(base(HttpStatus.NOT_FOUND, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(base(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(OptimisticLockException ex) {
        Long currentVersion = null;
        Object entity = ex.getEntity();
        if (entity instanceof Reagent r) {
            currentVersion = r.getVersion();
        }
        return conflictBody(ex.getMessage(), currentVersion);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleSpringOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return conflictBody(
                "Reagent was modified by another user. Reload and try again.",
                null);
    }

    private ResponseEntity<Map<String, Object>> conflictBody(String message, Long currentVersion) {
        Map<String, Object> body = base(HttpStatus.CONFLICT, message);
        body.put("currentVersion", currentVersion);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    private Map<String, Object> base(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
