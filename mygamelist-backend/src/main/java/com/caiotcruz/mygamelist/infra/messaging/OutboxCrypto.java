package com.caiotcruz.mygamelist.infra.messaging;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
@Component
public class OutboxCrypto {
 private final SecretKeySpec key;
 public OutboxCrypto(@Value("${app.outbox-key}") String encoded) {
  byte[] bytes=Base64.getDecoder().decode(encoded);
  if(bytes.length!=32) throw new IllegalArgumentException("OUTBOX_KEY must be Base64 of 32 random bytes");
  key=new SecretKeySpec(bytes,"AES");
 }
 public String encrypt(String plain) {
  try {
   byte[] iv=new byte[12]; new java.security.SecureRandom().nextBytes(iv);
   Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv));
   byte[] encrypted=c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
   return Base64.getEncoder().encodeToString(java.nio.ByteBuffer.allocate(12+encrypted.length).put(iv).put(encrypted).array());
  }catch(Exception e){throw new IllegalStateException("Cannot encrypt mail",e);}
 }
 public String decrypt(String encoded) {
  try {
   byte[] bytes=Base64.getDecoder().decode(encoded);
   Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,bytes,0,12));
   return new String(c.doFinal(bytes,12,bytes.length-12),StandardCharsets.UTF_8);
  }catch(Exception e){throw new IllegalStateException("Cannot decrypt mail",e);}
 }
}
