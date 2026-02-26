package com.nebula.catalog.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    problemDetail.setTitle("Access Denied");
    problemDetail.setType(URI.create("https://nebula.com/probs/access-denied"));
    return problemDetail;
  }

  // Add other generic handlers here as needed (e.g. IllegalArgumentException ->
  // 400 Bad Request)
}
