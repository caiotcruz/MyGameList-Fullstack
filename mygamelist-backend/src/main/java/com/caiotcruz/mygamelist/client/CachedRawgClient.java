package com.caiotcruz.mygamelist.client;
import com.caiotcruz.mygamelist.dto.*;
import com.caiotcruz.mygamelist.dto.rawg.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.function.Supplier;
@Component @Primary
public class CachedRawgClient implements RawgClient {
 private final java.util.concurrent.Semaphore upstreamSlots = new java.util.concurrent.Semaphore(8);
 private final RawgClient delegate;
 private final StringRedisTemplate redis;
 private final JsonMapper json;
 private final MeterRegistry metrics;
 public CachedRawgClient(@Qualifier("com.caiotcruz.mygamelist.client.RawgClient") RawgClient delegate, StringRedisTemplate redis, JsonMapper json, MeterRegistry metrics) {
  this.delegate=delegate; this.redis=redis; this.json=json; this.metrics=metrics;
 }
 private <T> T cached(String method, Object[] args, Class<T> type, int seconds, Supplier<T> loader) {
  String key;
  try {
   key="mgl:rawg:v1:"+method+":"+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(args)));
  } catch(Exception e) { throw new IllegalStateException("Cache key error",e); }
  try {
   String value=redis.opsForValue().get(key);
   if(value!=null) { T result=json.readValue(value,type); metrics.counter("mgl.rawg.cache","result","hit").increment(); return result; }
  } catch(Exception e) { metrics.counter("mgl.rawg.cache","result","error").increment(); }
  metrics.counter("mgl.rawg.cache","result","miss").increment();
  if(!upstreamSlots.tryAcquire()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Catálogo ocupado. Tente novamente.");
  T result;
  try { result=loader.get(); } finally { upstreamSlots.release(); }
  if(result!=null) try { redis.opsForValue().set(key,json.writeValueAsString(result),Duration.ofSeconds(seconds)); } catch(Exception e) { metrics.counter("mgl.rawg.cache","result","error").increment(); }
  return result;
 }
 public RawgSearchResponse searchGames(String apiKey, String query, int pageSize, int page) { return cached("searchGames",new Object[]{query, pageSize, page},RawgSearchResponse.class,300,()->delegate.searchGames(apiKey,query, pageSize, page)); }
 public GameDetailsDTO getGameDetails(String apiKey, Long id) { return cached("getGameDetails",new Object[]{id},GameDetailsDTO.class,3600,()->delegate.getGameDetails(apiKey,id)); }
 public RawgGameListResponse getAdditions(String apiKey, Long id) { return cached("getAdditions",new Object[]{id},RawgGameListResponse.class,3600,()->delegate.getAdditions(apiKey,id)); }
 public RawgGameListResponse getGameSeries(String apiKey, Long id) { return cached("getGameSeries",new Object[]{id},RawgGameListResponse.class,3600,()->delegate.getGameSeries(apiKey,id)); }
 public RawgScreenshotsResponse getScreenshots(String apiKey, Long id) { return cached("getScreenshots",new Object[]{id},RawgScreenshotsResponse.class,3600,()->delegate.getScreenshots(apiKey,id)); }
 public RawgStoresResponse getStores(String apiKey, Long id) { return cached("getStores",new Object[]{id},RawgStoresResponse.class,3600,()->delegate.getStores(apiKey,id)); }
 public RawgAchievementsResponse getAchievements(String apiKey, Long id, int page, int pageSize) { return cached("getAchievements",new Object[]{id, page, pageSize},RawgAchievementsResponse.class,3600,()->delegate.getAchievements(apiKey,id, page, pageSize)); }
 public RawgMoviesResponse getMovies(String apiKey, Long id) { return cached("getMovies",new Object[]{id},RawgMoviesResponse.class,3600,()->delegate.getMovies(apiKey,id)); }
}
