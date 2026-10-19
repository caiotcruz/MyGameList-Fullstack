package com.caiotcruz.mygamelist.infra.security;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class RateLimitsTest {
 @Test void hashesPersonalIdentifiersAndRejectsUnavailableRedis(){
  var redis=mock(StringRedisTemplate.class);var limits=new RateLimits(redis,mock(SecurityEvents.class),"fixture-key-at-least-thirty-two-bytes");
  assertEquals(64,limits.fingerprint("alice@example.test").length());assertNotEquals(limits.fingerprint("alice@example.test"),limits.fingerprint("bob@example.test"));
  var error=assertThrows(SecurityRejection.class,()->limits.check("login","alice@example.test",5,60));assertEquals(503,error.status);
 }
 @Test void returnsRetryAfterWhenTheWindowIsFull(){
  var redis=mock(StringRedisTemplate.class);
  doReturn(27L).when(redis).execute(any(RedisScript.class),anyList(),any(Object[].class));
  var limits=new RateLimits(redis,mock(SecurityEvents.class),"fixture-key-at-least-thirty-two-bytes");
  var error=assertThrows(SecurityRejection.class,()->limits.check("login","fixture",5,60));assertEquals(429,error.status);assertEquals(27,error.retryAfter);
 }
 @Test void throttlesVerificationAcrossDifferentIpsBySameAccount()throws Exception {
  var limits=mock(RateLimits.class);var filter=new AuthRateLimitFilter(limits,new ClientAddress(""));
  for(String ip:java.util.List.of("192.0.2.1","192.0.2.2")){
   var req=new MockHttpServletRequest("POST","/auth/verify");req.setRemoteAddr(ip);req.setAttribute("mgl.auth.email","alice@example.test");filter.doFilter(req,new MockHttpServletResponse(),(r,s)->{});
  }
  verify(limits,times(2)).check("auth_account_verify","alice@example.test",5,900);
 }
 @Test void trustsOnlyAnExplicitImmediateProxy(){
  var req=new MockHttpServletRequest();req.setRemoteAddr("192.0.2.1");req.addHeader("X-Real-IP","198.51.100.2");
  assertEquals("192.0.2.1",new ClientAddress("").of(req));assertEquals("198.51.100.2",new ClientAddress("192.0.2.1").of(req));
  req.removeHeader("X-Real-IP");req.addHeader("X-Real-IP","attacker.example");assertEquals("192.0.2.1",new ClientAddress("192.0.2.1").of(req));
 }
}
