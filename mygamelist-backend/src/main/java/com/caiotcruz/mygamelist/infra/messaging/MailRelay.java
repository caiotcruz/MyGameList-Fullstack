package com.caiotcruz.mygamelist.infra.messaging;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.core.*;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.mail.worker-enabled",havingValue="true",matchIfMissing=true)
public class MailRelay {
 private final JdbcTemplate db; private final TransactionTemplate tx; private final RabbitTemplate rabbit; private final MeterRegistry metrics;
 public MailRelay(JdbcTemplate db,TransactionTemplate tx,RabbitTemplate rabbit,MeterRegistry metrics){this.db=db;this.tx=tx;this.rabbit=rabbit;this.metrics=metrics;}
 @Scheduled(fixedDelayString="${app.mail.relay-delay-ms:3000}")
 public void publish() {
  try {
   tx.executeWithoutResult(status->{
    var ids=db.queryForList("SELECT id FROM mail_outbox WHERE state='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP AND expires_at>CURRENT_TIMESTAMP ORDER BY next_attempt_at LIMIT 20 FOR UPDATE SKIP LOCKED",String.class);
    for(String id:ids) {
     try {
      CorrelationData correlation=new CorrelationData(id);
      MessageProperties properties=new MessageProperties();properties.setContentType("text/plain");properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
      rabbit.send("",MessagingConfiguration.QUEUE,new Message(id.getBytes(StandardCharsets.UTF_8),properties),correlation);
      var confirm=correlation.getFuture().get(3,TimeUnit.SECONDS);
      if(!confirm.isAck() || correlation.getReturned()!=null) throw new IllegalStateException("Broker did not accept mail ID");
      db.update("UPDATE mail_outbox SET next_attempt_at=CURRENT_TIMESTAMP+INTERVAL '2 minutes' WHERE id=?",id);
      metrics.counter("mgl.mail.published").increment();
     } catch(Exception ex) {
      if(ex instanceof InterruptedException) Thread.currentThread().interrupt();
      db.update("UPDATE mail_outbox SET next_attempt_at=CURRENT_TIMESTAMP+INTERVAL '30 seconds' WHERE id=?",id);
      metrics.counter("mgl.mail.publish.errors").increment();
      break;
     }
    }
   });
  } catch(Exception ex) { metrics.counter("mgl.mail.relay.errors").increment(); }
 }
 @Scheduled(fixedDelay=60000)
 public void cleanup() {
  db.update("UPDATE mail_outbox SET state='EXPIRED',recipient='',subject='',body='' WHERE state='PENDING' AND expires_at<=CURRENT_TIMESTAMP");
  db.update("DELETE FROM mail_outbox WHERE state<>'PENDING' AND created_at<CURRENT_TIMESTAMP-INTERVAL '7 days'");
 }
}
