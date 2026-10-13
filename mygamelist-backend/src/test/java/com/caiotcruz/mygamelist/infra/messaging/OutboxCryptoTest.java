package com.caiotcruz.mygamelist.infra.messaging;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Base64;
class OutboxCryptoTest {
 @Test void encryptsWithUniqueNonceAndRejectsTampering() {
  var crypto=new OutboxCrypto(Base64.getEncoder().encodeToString(new byte[32]));
  String a=crypto.encrypt("código secreto"),b=crypto.encrypt("código secreto");
  assertNotEquals(a,b);assertEquals("código secreto",crypto.decrypt(a));
  byte[] corrupted=Base64.getDecoder().decode(a);corrupted[15]^=1;
  assertThrows(IllegalStateException.class,()->crypto.decrypt(Base64.getEncoder().encodeToString(corrupted)));
 }
 @Test void rejectsWrongKeySize() {assertThrows(IllegalArgumentException.class,()->new OutboxCrypto("YWJj"));}
}
