package com.caiotcruz.mygamelist.infra.security;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
/** Pure validation helpers; no network or persistence. */
public final class RequestLimits {
    public static final int MAX_BODY=384*1024;
    private RequestLimits(){}
    public static boolean write(String method){return method.equals("POST")||method.equals("PUT")||method.equals("PATCH")||method.equals("DELETE");}
    public static String email(String value){return value==null?null:value.trim().toLowerCase(Locale.ROOT);}
    public static boolean validPassword(String value,boolean creation){
        return value!=null && value.length()>=(creation?12:1) && value.getBytes(StandardCharsets.UTF_8).length<=72;
    }
    public static void boundedQuery(String query,int max){
        if(query==null || query.isBlank() || query.length()>max)throw new IllegalArgumentException("Invalid query");
    }
    public static boolean authPath(String path){return java.util.Set.of("/auth/login","/auth/register","/auth/verify","/auth/forgot-password","/auth/reset-password","/auth/resend-verification").contains(path);}
}
