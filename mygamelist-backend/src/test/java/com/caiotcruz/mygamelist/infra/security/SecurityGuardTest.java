package com.caiotcruz.mygamelist.infra.security;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SecurityGuardTest {
 private ApiAdmissionFilter filter(RateLimits limits){return new ApiAdmissionFilter(limits,mock(SecurityEvents.class),new ClientAddress(""),1,120);}
 private MockHttpServletRequest request(String body){var r=new MockHttpServletRequest("POST","/auth/login");r.setContentType("application/json");r.setRemoteAddr("192.0.2.1");r.setContent(body.getBytes(StandardCharsets.UTF_8));return r;}
 private void rejected(String body,int code)throws Exception {
  var response=new MockHttpServletResponse();filter(mock(RateLimits.class)).doFilter(request(body),response,(r,s)->fail("Must not reach controller"));assertEquals(code,response.getStatus());
 }
 @Test void rejectsOversizedBody(){assertDoesNotThrow(()->rejected("x".repeat(RequestLimits.MAX_BODY+1),413));}
 @Test void rejectsDuplicateKeysTrailingTokensAndDeepJson()throws Exception {
  rejected("{\"email\":\"a\",\"email\":\"b\"}",400);
  rejected("{} {}",400);
  rejected("{\"x\":"+"[".repeat(30)+"0"+"]".repeat(30)+"}",400);
 }
 @Test void capsChunkedBodiesWithoutContentLength()throws Exception {
  var req=new MockHttpServletRequest("POST","/my-games"){@Override public long getContentLengthLong(){return -1;}@Override public int getContentLength(){return -1;}};
  req.setContentType("application/json");req.setContent(new byte[RequestLimits.MAX_BODY+1]);
  var response=new MockHttpServletResponse();filter(mock(RateLimits.class)).doFilter(req,response,(r,s)->fail());assertEquals(413,response.getStatus());
 }
 @Test void replaysValidJsonAndNormalizesOnlyTheLimitIdentity()throws Exception {
  String body="{\"email\":\" Alice@Example.test \",\"password\":\"long password\"}";
  var response=new MockHttpServletResponse();var limits=mock(RateLimits.class);var req=request(body);req.addHeader("X-Forwarded-For","198.51.100.9");
  filter(limits).doFilter(req,response,(r,s)->{
   assertEquals("alice@example.test",r.getAttribute("mgl.auth.email"));assertArrayEquals(body.getBytes(StandardCharsets.UTF_8),r.getInputStream().readAllBytes());
  });
  verify(limits).check("ip_burst","192.0.2.1",80,10);assertEquals(200,response.getStatus());
 }
 @Test void rejectsEncodedAuthPathAndCompressedBody()throws Exception {
  var req=request("{}");req.setRequestURI("/auth/%6cogin");var response=new MockHttpServletResponse();filter(mock(RateLimits.class)).doFilter(req,response,(r,s)->fail());assertEquals(400,response.getStatus());
  req=request("{}");req.addHeader("Content-Encoding","gzip");response=new MockHttpServletResponse();filter(mock(RateLimits.class)).doFilter(req,response,(r,s)->fail());assertEquals(415,response.getStatus());
 }
 @Test void limitsConcurrentWorkAndReleasesPermitAfterCompletion()throws Exception {
  var f=filter(mock(RateLimits.class));var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var executor=Executors.newSingleThreadExecutor();
  try {
   var running=executor.submit(()->{try{f.doFilter(request("{}"),new MockHttpServletResponse(),(r,s)->{entered.countDown();try{release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});}catch(Exception e){throw new RuntimeException(e);}});
   assertTrue(entered.await(2,TimeUnit.SECONDS));var response=new MockHttpServletResponse();f.doFilter(request("{}"),response,(r,s)->fail());assertEquals(503,response.getStatus());
   release.countDown();running.get(3,TimeUnit.SECONDS);response=new MockHttpServletResponse();f.doFilter(request("{}"),response,(r,s)->{});assertEquals(200,response.getStatus());
  }finally{release.countDown();executor.shutdownNow();}
 }
}
