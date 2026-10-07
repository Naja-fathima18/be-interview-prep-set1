package com.example.beinterviewprep.common.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  static final String ERRORS_PROPERTY = "errors";

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Resource not found");
    return problem;
  }

  @ExceptionHandler(ShortUrlExpiredException.class)
  public ProblemDetail handleExpired(ShortUrlExpiredException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
    problem.setTitle("Resource expired");
    return problem;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) {
    log.error("Unexpected error", ex);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    problem.setTitle("Internal server error");
    return problem;
  }

  @ExceptionHandler(PropertyReferenceException.class)
  public ProblemDetail handleUnknownSortProperty(PropertyReferenceException ex) {
    return validationProblem(
        List.of(
            new FieldViolation("sort", "Unknown property '%s'".formatted(ex.getPropertyName()))));
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<FieldViolation> violations =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldViolation(error.getField(), messageOf(error)))
            .sorted(Comparator.comparing(FieldViolation::field))
            .toList();
    ProblemDetail body = validationProblem(violations);
    return handleExceptionInternal(ex, body, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail body =
        ex.getCause() instanceof MismatchedInputException mismatch && hasPath(mismatch)
            ? validationProblem(List.of(violationOf(mismatch)))
            : ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request");
    return handleExceptionInternal(ex, body, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String field = ex.getPropertyName() != null ? ex.getPropertyName() : "parameter";
    ProblemDetail body =
        validationProblem(
            List.of(
                new FieldViolation(
                    field, invalidValueMessage(ex.getValue(), ex.getRequiredType()))));
    return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
  }

  private static ProblemDetail validationProblem(List<FieldViolation> violations) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    problem.setTitle("Invalid request");
    problem.setProperty(ERRORS_PROPERTY, violations);
    return problem;
  }

  private static String messageOf(FieldError error) {
    return error.isBindingFailure()
        ? invalidValueMessage(error.getRejectedValue(), null)
        : error.getDefaultMessage();
  }

  private static boolean hasPath(MismatchedInputException ex) {
    return !ex.getPath().isEmpty();
  }

  private static FieldViolation violationOf(MismatchedInputException ex) {
    String field =
        ex.getPath().stream()
            .map(
                reference ->
                    reference.getFieldName() != null
                        ? reference.getFieldName()
                        : String.valueOf(reference.getIndex()))
            .collect(Collectors.joining("."));
    Object value = ex instanceof InvalidFormatException invalid ? invalid.getValue() : null;
    return new FieldViolation(field, invalidValueMessage(value, ex.getTargetType()));
  }

  private static String invalidValueMessage(Object value, Class<?> targetType) {
    String prefix = value == null ? "Invalid value" : "Invalid value '" + value + "'";
    if (targetType == null) {
      return prefix;
    }
    if (targetType.isEnum()) {
      return prefix + "; must be one of " + Arrays.toString(targetType.getEnumConstants());
    }
    if (LocalDate.class.equals(targetType)) {
      return prefix + "; expected a date in yyyy-MM-dd format";
    }
    if (Number.class.isAssignableFrom(targetType) || targetType.isPrimitive()) {
      return prefix + "; expected a number";
    }
    return prefix;
  }
}
