package com.caiotcruz.mygamelist.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.springframework.data.redis.core.*;
import com.caiotcruz.mygamelist.dto.RawgSearchResponse;
import tools.jackson.databind.json.JsonMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
class CachedRawgClientTest {
 @Test void cacheHitDoesNotCallRawg() {
  var delegate=mock(RawgClient.class);var redis=mock(StringRedisTemplate.class);
  ValueOperations<String,String> values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
  when(values.get(anyString())).thenReturn("{\"count\":0,\"results\":[]}");
  var client=new CachedRawgClient(delegate,redis,JsonMapper.builder().build(),new SimpleMeterRegistry());
  assertEquals(0,client.searchGames("secret","zelda",10,1).count());verifyNoInteractions(delegate);
  verify(values).get(argThat(key->key instanceof String text && !text.contains("secret") && !text.contains("zelda")));
 }
 @Test void unavailableRedisFallsBackToRawg() {
  var delegate=mock(RawgClient.class);var redis=mock(StringRedisTemplate.class);
  when(redis.opsForValue()).thenThrow(new IllegalStateException());
  var expected=new RawgSearchResponse(0,java.util.List.of());when(delegate.searchGames("key","test",10,1)).thenReturn(expected);
  var client=new CachedRawgClient(delegate,redis,JsonMapper.builder().build(),new SimpleMeterRegistry());
  assertSame(expected,client.searchGames("key","test",10,1));
 }
}
