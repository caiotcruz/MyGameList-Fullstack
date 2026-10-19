package com.caiotcruz.mygamelist.infra.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final RateLimits limits;
    private final ClientAddress address;
    public AuthRateLimitFilter(RateLimits limits,ClientAddress address){this.limits=limits;this.address=address;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws IOException,ServletException {
        String path=req.getRequestURI();
        if("POST".equals(req.getMethod()) && RequestLimits.authPath(path)) {
            try {
                String group=path.substring(path.lastIndexOf('/')+1);
                if(path.endsWith("/login"))limits.check("login_global","all",300,60);
                if(path.endsWith("/register"))limits.check("registration_global","all",30,3600);
                if(path.endsWith("/register")||path.endsWith("/forgot-password")||path.endsWith("/resend-verification"))
                    limits.check("mail_global","all",100,3600);
                limits.check("auth_ip_"+group,address.of(req),path.endsWith("/login")?15:8,60);
                String email=(String)req.getAttribute("mgl.auth.email");
                if(email!=null && !email.isBlank()) {
                    boolean mail=path.endsWith("/forgot-password")||path.endsWith("/resend-verification")||path.endsWith("/register");
                    limits.check(mail?"auth_mail":"auth_account_"+group,email,mail?3:path.endsWith("/verify")?5:15,mail?3600:900);
                }
            }catch(SecurityRejection e){SecurityResponses.reject(res,e);return;}
        }
        chain.doFilter(req,res);
    }
}
