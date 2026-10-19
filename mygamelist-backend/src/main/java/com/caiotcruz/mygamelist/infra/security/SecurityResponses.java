package com.caiotcruz.mygamelist.infra.security;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
public final class SecurityResponses {
    private SecurityResponses(){}
    public static void reject(HttpServletResponse response,SecurityRejection rejection)throws IOException {
        response.setStatus(rejection.status);response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control","no-store");
        if(rejection.retryAfter>0)response.setHeader("Retry-After",Integer.toString(rejection.retryAfter));
        // All messages/codes are application constants, never request input.
        response.getWriter().write("{\"code\":\""+rejection.code+"\",\"message\":\""+rejection.getMessage()+"\"}");
    }
}
