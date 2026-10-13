package com.caiotcruz.mygamelist.service;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.enums.*;
import com.caiotcruz.mygamelist.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service
public class CommunitySafety {
 private final JdbcTemplate db; private final UserRepository users; private final UserLocks locks;
 private final UserFollowRepository follows; private final LevelService levels; private final Set<Long> moderators;
 public CommunitySafety(JdbcTemplate db,UserRepository users,UserLocks locks,UserFollowRepository follows,LevelService levels,@Value("${app.moderator-ids:}") String moderatorIds){
  this.db=db;this.users=users;this.locks=locks;this.follows=follows;this.levels=levels;
  this.moderators=Arrays.stream(moderatorIds.split(",")).map(String::trim).filter(s->!s.isEmpty()).map(Long::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet());
 }
 public User me(){var auth=SecurityContextHolder.getContext().getAuthentication();User user=auth==null?null:users.findByEmail(auth.getName());if(user==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Entre novamente.");return user;}
 public boolean isModerator(){return moderators.contains(me().getId());}
 public void requireModerator(){if(!isModerator())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acesso restrito à moderação.");}
 public Set<Long> hiddenIds(Long viewer){if(viewer==null)return Set.of();return new HashSet<>(db.queryForList("SELECT blocked_id FROM tb_user_blocks WHERE blocker_id=? UNION SELECT blocker_id FROM tb_user_blocks WHERE blocked_id=?",Long.class,viewer,viewer));}
 public boolean blocked(Long a,Long b){return Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM tb_user_blocks WHERE (blocker_id=? AND blocked_id=?) OR (blocker_id=? AND blocked_id=?))",Boolean.class,a,b,b,a));}
 public void requireVisible(Long a,Long b){if(blocked(a,b))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Conteúdo indisponível devido a um bloqueio.");}
 public void requireProfile(Long id){requireVisible(me().getId(),id);}
 public boolean canSend(User sender,User recipient){
  if(sender.getId().equals(recipient.getId())||blocked(sender.getId(),recipient.getId()))return false;
  return switch(recipient.getMessagePolicy()){
   case NOBODY -> false;
   case EVERYONE -> true;
   case PEOPLE_I_FOLLOW -> follows.findByFollowerAndFollowed(recipient,sender).isPresent();
  };
 }
 @Transactional public void block(Long target){
  User actor=me();if(actor.getId().equals(target))throw new IllegalArgumentException("Você não pode bloquear a si mesmo.");
  locks.lock(actor.getId(),target);User other=users.findById(target).orElseThrow();
  db.update("INSERT INTO tb_user_blocks(blocker_id,blocked_id,created_at) VALUES(?,?,CURRENT_TIMESTAMP) ON CONFLICT(blocker_id,blocked_id) DO NOTHING",actor.getId(),target);
  removeFollow(actor,other);removeFollow(other,actor);
  db.update("DELETE FROM tb_notifications WHERE (user_id=? AND actor_id=?) OR (user_id=? AND actor_id=?)",actor.getId(),target,target,actor.getId());
 }
 private void removeFollow(User a,User b){follows.findByFollowerAndFollowed(a,b).ifPresent(f->{follows.delete(f);levels.revoke(a,ExperienceSource.USER_FOLLOWED);levels.revoke(b,ExperienceSource.USER_GAINED_FOLLOWER);});}
 @Transactional public void unblock(Long target){User actor=me();locks.lock(actor.getId(),target);db.update("DELETE FROM tb_user_blocks WHERE blocker_id=? AND blocked_id=?",actor.getId(),target);}
 public List<Map<String,Object>> blocks(){return db.queryForList("SELECT u.id,u.name FROM tb_user_blocks b JOIN tb_users u ON u.id=b.blocked_id WHERE b.blocker_id=? ORDER BY b.created_at DESC",me().getId());}
 @Transactional public void policy(MessagePolicy policy){User user=me();locks.lock(user.getId());user.setMessagePolicy(policy);users.saveAndFlush(user);}
}
