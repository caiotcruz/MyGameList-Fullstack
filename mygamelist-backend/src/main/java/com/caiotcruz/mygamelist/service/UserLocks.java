package com.caiotcruz.mygamelist.service;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import com.caiotcruz.mygamelist.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service
public class UserLocks {
 private final EntityManager em;
 public UserLocks(EntityManager em){this.em=em;}
 @Transactional(propagation=Propagation.MANDATORY)
 public void lock(Long... ids) {
  for(Long id:Arrays.stream(ids).distinct().sorted().toList()) {
   User existing=em.find(User.class,id);
   if(existing!=null && em.getLockMode(existing)==LockModeType.PESSIMISTIC_WRITE) continue;
   User user=em.find(User.class,id,LockModeType.PESSIMISTIC_WRITE,Map.of("jakarta.persistence.lock.timeout",5000));
   if(user==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuário não encontrado.");
   // Refresh after acquiring the lock: the security layer may have loaded an older instance.
   em.refresh(user,LockModeType.PESSIMISTIC_WRITE);
  }
 }
}
