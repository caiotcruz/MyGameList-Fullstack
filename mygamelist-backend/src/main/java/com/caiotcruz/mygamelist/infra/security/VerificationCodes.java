package com.caiotcruz.mygamelist.infra.security;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
@Component
public class VerificationCodes {
    private final byte[] key;
    public VerificationCodes(@Value("${api.security.token.secret}") String key){this.key=key.getBytes(StandardCharsets.UTF_8);}
    public String hash(String email,String code){
        try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(("verify:"+email+":"+code).getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public boolean matches(String email,String code,String hash){
        return hash!=null && java.security.MessageDigest.isEqual(hash(email,code).getBytes(StandardCharsets.US_ASCII),hash.getBytes(StandardCharsets.US_ASCII));
    }
}
