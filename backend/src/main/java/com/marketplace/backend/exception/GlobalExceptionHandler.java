package com.marketplace.backend.exception;

import com.marketplace.backend.dto.ErrorResponseDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDTO> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDTO(ex.getMessage()));
    }


}