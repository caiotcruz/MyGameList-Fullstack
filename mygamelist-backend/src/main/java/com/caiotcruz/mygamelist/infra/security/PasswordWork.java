package com.caiotcruz.mygamelist.infra.security;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import java.util.concurrent.Semaphore;
@Component
public class PasswordWork {
    private final Semaphore slots=new Semaphore(4);
    private final PasswordEncoder encoder;
    public PasswordWork(PasswordEncoder encoder){this.encoder=encoder;}
    private void enter(){if(!slots.tryAcquire())throw new SecurityRejection(503,"AUTH_BUSY","Autenticação ocupada. Tente novamente.",3);}
    public String encode(String password){enter();try{return encoder.encode(password);}finally{slots.release();}}
    public Authentication authenticate(AuthenticationManager manager,String email,String password){
        enter();try{return manager.authenticate(new UsernamePasswordAuthenticationToken(email,password));}finally{slots.release();}
    }
}
