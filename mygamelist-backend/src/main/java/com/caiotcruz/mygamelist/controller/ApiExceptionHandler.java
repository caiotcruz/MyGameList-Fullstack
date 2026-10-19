package com.caiotcruz.mygamelist.controller;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
 private final com.caiotcruz.mygamelist.infra.security.SecurityEvents events;
 public ApiExceptionHandler(com.caiotcruz.mygamelist.infra.security.SecurityEvents events){this.events=events;}
 @ExceptionHandler(com.caiotcruz.mygamelist.infra.security.SecurityRejection.class)
 public ResponseEntity<Map<String,String>> protection(com.caiotcruz.mygamelist.infra.security.SecurityRejection e){
  var builder=ResponseEntity.status(e.status);if(e.retryAfter>0)builder.header("Retry-After",Integer.toString(e.retryAfter));
  return builder.body(Map.of("code",e.code,"message",e.getMessage()));
 }
 @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
   jakarta.validation.ConstraintViolationException.class,org.springframework.web.method.annotation.HandlerMethodValidationException.class})
 public ResponseEntity<Map<String,String>> parameters(Exception e){return response(HttpStatus.BAD_REQUEST,"INVALID_PARAMETERS","Parâmetros inválidos.");}
 @ExceptionHandler({org.springframework.web.HttpRequestMethodNotSupportedException.class})
 public ResponseEntity<Map<String,String>> method(Exception e){return response(HttpStatus.METHOD_NOT_ALLOWED,"METHOD_NOT_ALLOWED","Método não permitido.");}
 @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class})
 public ResponseEntity<Map<String,String>> missing(Exception e){return response(HttpStatus.NOT_FOUND,"NOT_FOUND","Recurso não encontrado.");}
 @ExceptionHandler({org.springframework.dao.QueryTimeoutException.class,org.springframework.dao.DataAccessResourceFailureException.class})
 public ResponseEntity<Map<String,String>> database(Exception e){return response(HttpStatus.SERVICE_UNAVAILABLE,"DATABASE_BUSY","Serviço temporariamente indisponível.");}
 private ResponseEntity<Map<String,String>> response(HttpStatusCode status,String code,String message){return ResponseEntity.status(status).body(Map.of("code",code,"message",message));}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> status(ResponseStatusException e){return response(e.getStatusCode(),"REQUEST_REJECTED",Objects.requireNonNullElse(e.getReason(),"Não foi possível concluir a solicitação."));}
 @ExceptionHandler(AuthenticationException.class) public ResponseEntity<Map<String,String>> auth(AuthenticationException e){events.record("authentication_failed");return response(HttpStatus.UNAUTHORIZED,"AUTHENTICATION_FAILED","Credenciais inválidas ou conta ainda não verificada.");}
 @ExceptionHandler(AccessDeniedException.class) public ResponseEntity<Map<String,String>> forbidden(AccessDeniedException e){events.record("access_denied");return response(HttpStatus.FORBIDDEN,"FORBIDDEN","Você não tem permissão para esta ação.");}
 @ExceptionHandler({DataIntegrityViolationException.class,ConcurrencyFailureException.class}) public ResponseEntity<Map<String,String>> conflict(Exception e){return response(HttpStatus.CONFLICT,"CONFLICT","Os dados foram alterados ou já existem. Atualize a página e tente novamente.");}
 @ExceptionHandler(feign.FeignException.class) public ResponseEntity<Map<String,String>> upstream(Exception e){return response(HttpStatus.SERVICE_UNAVAILABLE,"CATALOG_UNAVAILABLE","Catálogo temporariamente indisponível. Tente novamente.");}
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e){return response(HttpStatus.BAD_REQUEST,"INVALID_REQUEST","Dados inválidos. Confira os campos e tente novamente.");}
 @ExceptionHandler(HttpMessageNotReadableException.class) public ResponseEntity<Map<String,String>> unreadable(Exception e){return response(HttpStatus.BAD_REQUEST,"INVALID_BODY","Formato de dados inválido.");}
 @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException e){String message=e.getBindingResult().getFieldErrors().stream().findFirst().map(f->Objects.requireNonNullElse(f.getDefaultMessage(),"Campo inválido.")).orElse("Confira os dados informados.");return response(HttpStatus.BAD_REQUEST,"VALIDATION_FAILED",message);}
 @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,String>> unexpected(Exception e){String id=UUID.randomUUID().toString();org.slf4j.LoggerFactory.getLogger(getClass()).error("Request {} failed: {}",id,e.getClass().getName());return ResponseEntity.status(500).body(Map.of("code","INTERNAL_ERROR","message","Não foi possível concluir agora. Tente novamente.","requestId",id));}
}
