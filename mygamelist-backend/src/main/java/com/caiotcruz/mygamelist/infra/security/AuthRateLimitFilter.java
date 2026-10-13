package com.caiotcruz.mygamelist.infra.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final org.springframework.data.redis.core.StringRedisTemplate redis;
    private final org.springframework.data.redis.core.script.DefaultRedisScript<Long> script = new org.springframework.data.redis.core.script.DefaultRedisScript<>(
        "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],60) end; return n", Long.class);
    public AuthRateLimitFilter(org.springframework.data.redis.core.StringRedisTemplate redis) { this.redis=redis; }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
            FilterChain chain) throws ServletException, IOException {
        String path = req.getRequestURI();
        if (!"POST".equalsIgnoreCase(req.getMethod()) || !(
                path.equals("/auth/login") || path.equals("/auth/register") ||
                path.equals("/auth/verify") || path.equals("/auth/forgot-password") ||
                path.equals("/auth/reset-password") || path.equals("/auth/resend-verification"))) {
            chain.doFilter(req,res); return;
        }
        int max = path.equals("/auth/login") ? 15 : 8;
        Long count;
        try {
            count=redis.execute(script,java.util.List.of("mgl:auth:"+req.getRemoteAddr()+":"+path));
            if(count==null) throw new IllegalStateException();
        } catch(Exception ex) {
            res.setStatus(503);res.setHeader("Retry-After","30");
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"message\":\"Autenticação temporariamente indisponível. Tente novamente.\"}");return;
        }
        if (count > max) {
            res.setStatus(429); res.setHeader("Retry-After", "60");
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"message\":\"Muitas tentativas. Aguarde um minuto.\"}"); return;
        }
        chain.doFilter(req,res);
    }
}
