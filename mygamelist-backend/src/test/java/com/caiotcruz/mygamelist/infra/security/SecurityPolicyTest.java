package com.caiotcruz.mygamelist.infra.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SecurityPolicyTest {
 @Test void pureContracts(){SecurityPolicyContract.run();}
 @Test void verificationCodesAreKeyedAndBoundToAccount(){
  var codes=new VerificationCodes("test-key-with-at-least-thirty-two-bytes");var hash=codes.hash("alice@example.test","123456");
  assertNotEquals("123456",hash);assertTrue(codes.matches("alice@example.test","123456",hash));
  assertFalse(codes.matches("bob@example.test","123456",hash));assertFalse(codes.matches("alice@example.test","123457",hash));
 }
}
