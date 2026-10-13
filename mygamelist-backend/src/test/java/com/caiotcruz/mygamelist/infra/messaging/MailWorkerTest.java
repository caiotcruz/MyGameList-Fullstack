package com.caiotcruz.mygamelist.infra.messaging;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.amqp.core.*;
class MailWorkerTest {
 @Test void completedOrUnknownMessageDoesNotResendMail() {
  var db=mock(JdbcTemplate.class);var sender=mock(JavaMailSender.class);
  String id=java.util.UUID.randomUUID().toString();
  when(db.queryForList(anyString(),eq(id))).thenReturn(java.util.List.of());
  var worker=new MailWorker(db,mock(OutboxCrypto.class),sender,new SimpleMeterRegistry(),"mail@example.com");
  worker.receive(new Message(id.getBytes(java.nio.charset.StandardCharsets.UTF_8),new MessageProperties()));
  verifyNoInteractions(sender);
 }
}
