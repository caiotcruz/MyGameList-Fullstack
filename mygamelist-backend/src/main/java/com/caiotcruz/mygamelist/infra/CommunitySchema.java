package com.caiotcruz.mygamelist.infra;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
@Component
public class CommunitySchema implements ApplicationRunner {
 private final JdbcTemplate db;private final boolean enabled;
 public CommunitySchema(JdbcTemplate db,@Value("${app.community.migrate:true}") boolean enabled){this.db=db;this.enabled=enabled;}
 public void run(ApplicationArguments args){
  if(!enabled)return;
  Long duplicates=db.queryForObject("SELECT COUNT(*) FROM (SELECT user_id FROM tb_user_game_list WHERE is_favorite=true GROUP BY user_id HAVING COUNT(*)>1) d",Long.class);
  if(duplicates!=null && duplicates>0)throw new IllegalStateException("Há usuários com mais de um favorito. Consulte MIGRATION.md antes de iniciar; nenhum registro foi removido.");
  db.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_one_favorite_per_user ON tb_user_game_list(user_id) WHERE is_favorite=true");
  db.execute("CREATE INDEX IF NOT EXISTS idx_blocks_reverse ON tb_user_blocks(blocked_id,blocker_id)");
  db.execute("CREATE INDEX IF NOT EXISTS idx_reports_created ON tb_content_reports(created_at DESC)");
 }
}
