package com.caiotcruz.mygamelist.infra.security;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="RUN_SECURITY_REDIS_TESTS",matches="true")
class SecurityRedisIT {
 @Test void atomicLimitUnderConcurrentRequests()throws Exception {
  var config=new RedisStandaloneConfiguration("127.0.0.1",6379);config.setPassword(RedisPassword.of(System.getenv("REDIS_PASSWORD")));
  var factory=new LettuceConnectionFactory(config);factory.afterPropertiesSet();factory.start();
  var redis=new StringRedisTemplate(factory);redis.afterPropertiesSet();
  String subject=UUID.randomUUID().toString();var metrics=new SimpleMeterRegistry();
  var limits=new RateLimits(redis,new SecurityEvents(metrics),"isolated-security-test-key-with-32-bytes");
  var executor=Executors.newFixedThreadPool(8);
  try {
   var futures=new ArrayList<Future<Integer>>();
   for(int i=0;i<40;i++)futures.add(executor.submit(()->{try{limits.check("test_atomic",subject,20,60);return 200;}catch(SecurityRejection e){return e.status;}}));
   int allowed=0,blocked=0;for(var f:futures){int status=f.get(10,TimeUnit.SECONDS);if(status==200)allowed++;else if(status==429)blocked++;else fail("Unexpected protection status "+status);}
   assertEquals(20,allowed);assertEquals(20,blocked);
  }finally{
   executor.shutdownNow();redis.delete("mgl:limits:v2:test_atomic:"+limits.fingerprint(subject));factory.destroy();metrics.close();
  }
 }
}
