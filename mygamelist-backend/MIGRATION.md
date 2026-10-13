# Migração desta rodada

Atualize backend e frontend juntos, preservando seu .env e o volume PostgreSQL. Pare o backend antigo e faça backup conforme README. Não use `docker compose down -v`.

No ambiente local padrão, `JPA_DDL_AUTO=update` adiciona `tb_users.message_policy`, `tb_user_blocks` e `tb_content_reports`. A coluna de preferência recebe PEOPLE_I_FOLLOW como padrão para contas existentes. Depois, CommunitySchema cria os índices de bloqueio/denúncias e o índice único parcial de favorito. `COMMUNITY_AUTO_MIGRATE=true` é o padrão.

A aplicação interrompe a inicialização se já houver mais de um favorito para a mesma pessoa; nenhum favorito é escolhido ou apagado automaticamente. Consulte:

```sql
SELECT user_id, array_agg(id ORDER BY id) AS itens_favoritos
FROM tb_user_game_list WHERE is_favorite=true
GROUP BY user_id HAVING count(*)>1;
```

Após backup, decida qual item manter para cada usuário afetado e ajuste apenas os outros registros para `is_favorite=false`. Depois reinicie. Não desative o índice como solução: ele é parte da proteção de concorrência.

Para ambientes que já usam `ddl-auto=validate`, o SQL `scripts/002_community.sql` descreve o acréscimo desta rodada sobre a versão anterior completa. Aplique antes de iniciar e revise com seu processo de migração. Não transforma sozinho uma base antiga incompleta no schema inteiro. Desative o runner com `COMMUNITY_AUTO_MIGRATE=false` somente quando esses índices já estiverem instalados. O projeto ainda não usa Flyway.

Para habilitar triagem, adicione ao .env os IDs numéricos das contas escolhidas:

```dotenv
MODERATOR_IDS=1,7
```

Substitua pelos IDs corretos de `SELECT id,name FROM tb_users ORDER BY id;`, reinicie o backend e entre com uma dessas contas. A seção de triagem aparece em Privacidade. Sem IDs, nenhum usuário recebe esse acesso. Notas e snapshots de denúncias ficam no banco e acessíveis à moderação; esta versão não implementa expiração automática dessas evidências.
