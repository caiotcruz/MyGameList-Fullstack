package com.caiotcruz.mygamelist.infra.security;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
@Service
public class RateLimits {
    private final StringRedisTemplate redis;
    private final SecurityEvents events;
    private final byte[] key;
    // Bounded counter, atomic expiration and authoritative retry time. No client clock involved.
    static final String LUA="local n=tonumber(redis.call('GET',KEYS[1]) or '0'); "
        +"if n>=tonumber(ARGV[1]) then local ttl=redis.call('TTL',KEYS[1]); if ttl<1 then redis.call('EXPIRE',KEYS[1],ARGV[2]); ttl=tonumber(ARGV[2]) end; return ttl end; "
        +"n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[2]) end; return 0";
    private final DefaultRedisScript<Long> script=new DefaultRedisScript<>(LUA,Long.class);
    public RateLimits(StringRedisTemplate redis,SecurityEvents events,@Value("${api.security.token.secret}") String key){this.redis=redis;this.events=events;this.key=key.getBytes(StandardCharsets.UTF_8);}
    public void check(String scope,String subject,int limit,int seconds){
        Long retry;
        try {retry=redis.execute(script,List.of("mgl:limits:v2:"+scope+":"+fingerprint(subject)),Integer.toString(limit),Integer.toString(seconds));
            if(retry==null)throw new IllegalStateException();
        }catch(Exception e){events.record("limiter_unavailable");throw new SecurityRejection(503,"PROTECTION_UNAVAILABLE","Proteção temporariamente indisponível. Tente novamente.",30);}
        if(retry>0){events.record("rate_"+scope);throw new SecurityRejection(429,"RATE_LIMITED","Muitas solicitações. Aguarde antes de tentar novamente.",Math.toIntExact(retry));}
    }
    String fingerprint(String value){
        try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException("Cannot build limit key",e);}
    }
}
