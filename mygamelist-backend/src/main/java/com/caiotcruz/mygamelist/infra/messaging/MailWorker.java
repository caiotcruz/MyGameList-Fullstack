package com.caiotcruz.mygamelist.infra.messaging;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.*;
import org.springframework.beans.factory.annotation.Value;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.mail.worker-enabled",havingValue="true",matchIfMissing=true)
public class MailWorker {
 private final JdbcTemplate db; private final OutboxCrypto crypto; private final JavaMailSender sender; private final MeterRegistry metrics; private final String from;
 public MailWorker(JdbcTemplate db,OutboxCrypto crypto,JavaMailSender sender,MeterRegistry metrics,@Value("${spring.mail.username}") String from){this.db=db;this.crypto=crypto;this.sender=sender;this.metrics=metrics;this.from=from;}
 @RabbitListener(queues=MessagingConfiguration.QUEUE) @Transactional
 public void receive(Message message) {
  String id=new String(message.getBody(),StandardCharsets.UTF_8);
  if(!id.matches("[0-9a-f-]{36}")) return;
  var rows=db.queryForList("SELECT * FROM mail_outbox WHERE id=? AND state='PENDING' AND expires_at>CURRENT_TIMESTAMP FOR UPDATE",id);
  if(rows.isEmpty()) return;
  var row=rows.get(0);
  try {
   var mime=sender.createMimeMessage();var helper=new MimeMessageHelper(mime,true,"UTF-8");
   helper.setFrom(from);helper.setTo(crypto.decrypt((String)row.get("recipient")));helper.setSubject(crypto.decrypt((String)row.get("subject")));helper.setText(crypto.decrypt((String)row.get("body")),true);
   sender.send(mime);
  }catch(Exception ex){
   int attempt=((Number)row.get("attempts")).intValue()+1;
   db.update("UPDATE mail_outbox SET attempts=?,state=?, next_attempt_at=CURRENT_TIMESTAMP+(? * INTERVAL '1 second'), recipient=CASE WHEN ? THEN '' ELSE recipient END,subject=CASE WHEN ? THEN '' ELSE subject END,body=CASE WHEN ? THEN '' ELSE body END WHERE id=?",attempt,attempt>=5?"FAILED":"PENDING",Math.min(300,15*(1<<attempt)),attempt>=5,attempt>=5,attempt>=5,id);
   metrics.counter("mgl.mail.delivery","result",attempt>=5?"failed":"retry").increment();return;
  }
  db.update("UPDATE mail_outbox SET state='SENT',recipient='',subject='',body='' WHERE id=?",id);
  metrics.counter("mgl.mail.delivery","result","sent").increment();
 }
}
