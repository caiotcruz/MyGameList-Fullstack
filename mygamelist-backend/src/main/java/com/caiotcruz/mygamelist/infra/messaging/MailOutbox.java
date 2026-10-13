package com.caiotcruz.mygamelist.infra.messaging;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
@Service
public class MailOutbox {
 private final JdbcTemplate db; private final OutboxCrypto crypto;
 public MailOutbox(JdbcTemplate db,OutboxCrypto crypto){this.db=db;this.crypto=crypto;}
 @Transactional
 public void enqueue(String to,String subject,String html,int minutes) {
  db.update("INSERT INTO mail_outbox(id,recipient,subject,body,expires_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP + (? * INTERVAL '1 minute'))",
    UUID.randomUUID().toString(),crypto.encrypt(to),crypto.encrypt(subject),crypto.encrypt(html),minutes);
 }
}
