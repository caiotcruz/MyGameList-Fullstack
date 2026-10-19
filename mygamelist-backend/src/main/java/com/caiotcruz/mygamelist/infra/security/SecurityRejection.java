package com.caiotcruz.mygamelist.infra.security;
public class SecurityRejection extends RuntimeException {
    public final int status,retryAfter;
    public final String code;
    public SecurityRejection(int status,String code,String message,int retryAfter){super(message);this.status=status;this.code=code;this.retryAfter=retryAfter;}
}
