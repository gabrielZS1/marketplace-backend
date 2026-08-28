package com.marketplace.backend.exception;

import com.marketplace.backend.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "Não foi possível concluir a operação: dado duplicado ou inválido.";

        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("users_email_key")) message = "Este e-mail já está cadastrado.";
            else if (ex.getMessage().contains("users_phone_key")) message = "Este telefone já está cadastrado.";
            else if (ex.getMessage().toLowerCase().contains("appointments_employee_id")) message = "Este profissional já tem um agendamento nesse horário.";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDTO(message));
    }

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handlePlanLimitExceeded(PlanLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(new ErrorResponseDTO(ex.getMessage()));
    }

    /**
     * Os controllers sinalizam erros de regra de negócio com `throw new RuntimeException("mensagem pt-BR")`.
     * Só essas (classe exatamente RuntimeException) têm a mensagem devolvida ao cliente,
     * com o status inferido pelo texto. Qualquer outra exceção cai no handler genérico.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDTO> handleRuntimeException(RuntimeException ex) {
        if (ex.getClass() != RuntimeException.class || ex.getMessage() == null) {
            return handleUnexpected(ex);
        }

        String msg = ex.getMessage();
        String lower = msg.toLowerCase();

        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (lower.contains("permiss")) status = HttpStatus.FORBIDDEN;
        else if (lower.contains("não encontrad") || lower.contains("nao encontrad")) status = HttpStatus.NOT_FOUND;

        return ResponseEntity.status(status).body(new ErrorResponseDTO(msg));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Dados inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDTO(message));
    }

    @ExceptionHandler({
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO("Requisição inválida."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(Exception ex) {
        log.error("Erro não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("Erro interno. Tente novamente em instantes."));
    }
}
