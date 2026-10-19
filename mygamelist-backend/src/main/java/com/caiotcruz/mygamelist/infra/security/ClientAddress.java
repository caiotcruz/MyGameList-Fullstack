package com.caiotcruz.mygamelist.infra.security;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;
@Component
public class ClientAddress {
    private final Set<String> trusted;
    public ClientAddress(@Value("${app.security.trusted-proxy-addresses:}") String addresses){
        trusted=new HashSet<>(Arrays.stream(addresses.split(",")).map(String::trim).filter(v->!v.isEmpty()).toList());
    }
    public String of(HttpServletRequest request){
        String peer=request.getRemoteAddr();
        if(!trusted.contains(peer))return peer;
        // The configured immediate proxy must OVERWRITE this single-valued header.
        String value=request.getHeader("X-Real-IP");
        if(value==null||value.length()>45||!value.matches("[0-9a-fA-F:.]+")||!value.contains(":")&&!value.matches("[0-9]+(?:\\.[0-9]+){3}"))return peer;
        try{return java.net.InetAddress.getByName(value).getHostAddress();}catch(java.net.UnknownHostException e){return peer;}
    }
}
