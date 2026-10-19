package com.caiotcruz.mygamelist.infra.security;
public class SecurityPolicyContract {
 static void check(boolean condition){if(!condition)throw new AssertionError();}
 public static void main(String[] args){run();System.out.println("8 pure security contracts passed");}
 public static void run(){
  check(!RequestLimits.validPassword("123456",true));
  check(RequestLimits.validPassword("uma frase longa e memoravel",true));
  check(RequestLimits.validPassword("é".repeat(36),true));
  check(!RequestLimits.validPassword("é".repeat(37),true));
  check(RequestLimits.validPassword("legacy",false));
  check(RequestLimits.email(" Alice@EXAMPLE.test ").equals("alice@example.test"));
  check(RequestLimits.authPath("/auth/verify")&&!RequestLimits.authPath("/auth/logout-all"));
  boolean rejected=false;try{RequestLimits.boundedQuery("x".repeat(201),200);}catch(IllegalArgumentException e){rejected=true;}check(rejected);
 }
}
