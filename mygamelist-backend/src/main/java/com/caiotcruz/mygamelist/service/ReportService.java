package com.caiotcruz.mygamelist.service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.constraints.*;
import java.util.*;
@Service
public class ReportService {
 public enum Target { USER, ACTIVITY, COMMENT, REVIEW, MESSAGE }
 public enum Reason { SPAM, HARASSMENT, HATE, INAPPROPRIATE, OTHER }
 public enum Status { OPEN, REVIEWING, RESOLVED, DISMISSED }
 public record Request(@NotNull Target targetType,@NotNull @Positive Long targetId,@NotNull Reason reason,@NotNull @Size(max=2000) String details){}
 public record Decision(@NotNull Status status,@NotBlank @Size(max=2000) String note){}
 private final JdbcTemplate db;private final CommunitySafety safety;private final UserLocks locks;
 public ReportService(JdbcTemplate db,CommunitySafety safety,UserLocks locks){this.db=db;this.safety=safety;this.locks=locks;}
 @Transactional public long submit(Request request){
  long reporter=safety.me().getId();locks.lock(reporter);
  var previous=db.queryForList("SELECT id FROM tb_content_reports WHERE reporter_id=? AND target_type=? AND target_id=?",Long.class,reporter,request.targetType().name(),request.targetId());
  if(!previous.isEmpty())return previous.get(0);
  if(db.queryForObject("SELECT COUNT(*) FROM tb_content_reports WHERE reporter_id=? AND created_at>CURRENT_TIMESTAMP-INTERVAL '1 hour'",Long.class,reporter)>=5)
   throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Limite de denúncias atingido. Aguarde uma hora.");
  List<Map<String,Object>> rows;
  if(request.targetType()==Target.MESSAGE){
   rows=db.queryForList("SELECT m.sender_id AS owner,m.content AS text FROM tb_messages m JOIN tb_conversations c ON c.id=m.conversation_id WHERE m.id=? AND (c.user_a_id=? OR c.user_b_id=?)",request.targetId(),reporter,reporter);
  }else{
   String sql=switch(request.targetType()){
    case USER -> "SELECT id AS owner,name AS text FROM tb_users WHERE id=?";
    case ACTIVITY -> "SELECT user_id AS owner,detail AS text FROM tb_activities WHERE id=?";
    case COMMENT -> "SELECT user_id AS owner,text FROM tb_comments WHERE id=?";
    case REVIEW -> "SELECT user_id AS owner,review AS text FROM tb_user_game_list WHERE id=? AND review IS NOT NULL AND LENGTH(review)>0";
    default -> throw new IllegalArgumentException();
   };rows=db.queryForList(sql,request.targetId());
  }
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Conteúdo não encontrado.");
  long owner=((Number)rows.get(0).get("owner")).longValue();
  if(owner==reporter)throw new IllegalArgumentException("Você não pode denunciar seu próprio conteúdo.");
  String snapshot=Objects.toString(rows.get(0).get("text"),"");snapshot=snapshot.substring(0,Math.min(snapshot.length(),2000));
  return db.queryForObject("INSERT INTO tb_content_reports(reporter_id,reported_user_id,target_type,target_id,reason,details,snapshot,status,created_at) VALUES(?,?,?,?,?,?,?,'OPEN',CURRENT_TIMESTAMP) RETURNING id",Long.class,reporter,owner,request.targetType().name(),request.targetId(),request.reason().name(),request.details().trim(),snapshot);
 }
 public List<Map<String,Object>> list(int page){safety.requireModerator();if(page<0||page>10000)throw new IllegalArgumentException("Página inválida.");return db.queryForList("SELECT id,reporter_id,reported_user_id,target_type,target_id,reason,details,snapshot,status,created_at,reviewed_at,reviewer_id,moderator_note FROM tb_content_reports ORDER BY created_at DESC,id DESC LIMIT 30 OFFSET ?",page*30);}
 @Transactional public void review(long id,Decision decision){
  safety.requireModerator();int changed=db.update("UPDATE tb_content_reports SET status=?,moderator_note=?,reviewer_id=?,reviewed_at=CURRENT_TIMESTAMP WHERE id=?",decision.status().name(),decision.note().trim(),safety.me().getId(),id);
  if(changed==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Denúncia não encontrada.");
 }
}
