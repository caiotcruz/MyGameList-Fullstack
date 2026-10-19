package com.caiotcruz.mygamelist.infra.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import com.caiotcruz.mygamelist.model.User;
@Component
public class AccountRateLimitFilter extends OncePerRequestFilter {
    private final RateLimits limits;
    public AccountRateLimitFilter(RateLimits limits){this.limits=limits;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws IOException,ServletException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.getPrincipal() instanceof User user) {
            try {
                String id=user.getId().toString(),path=req.getRequestURI();
                limits.check("account",id,300,60);
                if(RequestLimits.write(req.getMethod())){
                    limits.check("writes",id,30,60);
                    limits.check("writes_daily",id,200,86400);
                    limits.check("writes_global","all",600,60);
                }
                if(path.equals("/games/recommendations"))limits.check("recommendations",id,6,60);
                else if(path.startsWith("/games/"))limits.check("catalog",id,60,60);
                if(path.equals("/safety/reports") && req.getMethod().equals("POST"))limits.check("reports",id,5,3600);
            }catch(SecurityRejection e){SecurityResponses.reject(res,e);return;}
        }
        chain.doFilter(req,res);
    }
}
