# MyGameList — comunidade, concorrência e experiência (2026-10-06)

Backend completo, Java 17+ / Spring Boot 4.0.1. Use Node 24 apenas para o benchmark opcional.
Compatível com o frontend após as alterações de conquistas enviadas na conversa: o endpoint de conquistas devolve apenas title/icon/tier das desbloqueadas. Ranking usa últimos 30 dias; o campo legado interactionsThisMonth permanece para compatibilidade e contém PONTOS (adição=1, review=2). Atualize também o frontend com o pacote de 2026-10-06; ele inclui a interface de privacidade e denúncias.

## Novidades desta rodada

- Perfil: favorito, carreira e conquistas com cartões compactos e linguagem visual comum. Até três conquistas no cartão; “Exibir mais” abre uma janela com as desbloqueadas. Sem total, catálogo secreto ou critérios. No celular os cartões se empilham com altura automática.
- XP: gravações serializadas por usuário dentro de transações; pares de usuários bloqueados sempre por ordem de ID. Ações repetidas de adicionar um mesmo jogo não premiam XP novamente. Perfil, preferências e recuperação de senha usam a mesma disciplina de bloqueio.
- Favorito: troca transacional e índice único parcial no PostgreSQL, garantindo no máximo um favorito por usuário. Timeout/conflito retorna erro tratável, sem sucesso silencioso.
- Bloqueio bidirecional: oculta perfis e conteúdo nas consultas e impede seguir, curtir, comentar, votar em reviews e enviar mensagens entre o par. Remove os follows e notificações entre ambos. Desbloquear não restaura follows; histórico de mensagens é preservado e pode voltar a aparecer.
- Denúncias de perfil, atividade, comentário, review e mensagem. Snapshot de evidência; mensagem só pode ser denunciada por participante da conversa. Limite de cinco denúncias novas por hora por conta e deduplicação por alvo.
- `/privacy`: preferência de mensagens, lista de bloqueados e triagem para moderadores. Padrão inicial, inclusive contas existentes: **pessoas que eu sigo** podem me escrever. Opções: todos / pessoas que sigo / ninguém. Preferência não apaga histórico; bloquear esconde a conversa entre o par.
- Triagem protegida por `MODERATOR_IDS` no servidor. Moderadores podem ler denúncias, registrar nota e mudar estado. Não há banimento nem remoção automática do conteúdo ao marcar como resolvido.
- Respostas de erro com mensagem segura; formulários de review mantêm texto após falha e recuperam rascunho por usuário e jogo na mesma aba por até 24h. Salvamento confirmado ou descarte remove o rascunho. Fechar a aba pode eliminá-lo.
- O ranking continua em últimos 30 dias; Redis, RabbitMQ e outbox continuam incluídos.

Leia **MIGRATION.md**, **UPDATE.md** e **VALIDATION.md** antes de atualizar.

## Infraestrutura incluída

- Redis: cache compartilhado SOMENTE para respostas RAWG. Busca: 5 minutos; detalhes, imagens, lojas, vídeos, séries e conquistas RAWG: 1 hora. Chaves SHA-256 sem API key ou busca em texto aberto. JSON tipado, sem desserialização polimórfica. Dados privados e autorização NÃO ficam no cache.
- Redis: contador atômico de autenticação por IP + endpoint, 15 tentativas/minuto de login e 8/minuto das demais rotas de autenticação. Compartilhado por instâncias. Redis indisponível: autenticação retorna 503, sem ignorar a proteção. Cache indisponível: consulta diretamente a RAWG.
- RAWG: timeouts (conexão 2s, leitura 5s), no máximo 8 chamadas simultâneas POR INSTÂNCIA, sem retry automático adicional. Falhas não entram no cache. Nenhum circuit breaker ou bloqueio distribuído de cache foi implementado nesta rodada.
- E-mail: outbox transacional PostgreSQL + RabbitMQ. Cadastro/recuperação e pedido de e-mail são gravados na mesma transação. RabbitMQ recebe somente o UUID; destinatário, assunto e corpo ficam no PostgreSQL criptografados com AES-256-GCM, chave separada OUTBOX_KEY.
- Fila durável, mensagens persistentes, publisher confirms e mandatory returns. Até 20 publicações por lote, rechecagem a cada 3s. Sem confirmação: nova tentativa após 30s. Registro pendente confirmado é republicado após 2min se ainda não tiver sido concluído.
- Consumer com lock por registro, até 5 tentativas SMTP, atrasos de 30/60/120/240s; FAILED após a quinta falha. Idempotência por estado para mensagens já concluídas. Expiração de 15min para verificação e 60min para recuperação. SENT/FAILED/EXPIRED apagam o conteúdo criptografado; metadados são removidos após 7 dias.
- FAILED no banco é a lista de falhas terminais (não há uma DLQ RabbitMQ separada). Não republique IDs FAILED esperando reenvio: conteúdo já foi removido. É necessário um novo fluxo de autenticação. Endpoint novo POST /auth/resend-verification aceita {"email":"..."}, limita tentativas e responde genericamente; a UI ainda precisa de botão para chamá-lo. Apenas o código mais recente vale.
- Métricas Micrometer/Prometheus, health, pool Hikari limitado, limites Tomcat, encerramento gracioso e erros RAWG sem URL/chave em logs.
- Script PowerShell carrega .env, inicia infraestrutura, aplica SQL idempotente da outbox e executa Maven.

## Limite de validação desta entrega

Maven foi executado, mas não conseguiu resolver repo.maven.apache.org para baixar o parent Spring Boot 4.0.1. Portanto, build, testes JUnit e integração NÃO foram executados. Docker/PostgreSQL/RabbitMQ/Redis não estão disponíveis neste ambiente de geração. Há testes novos no pacote; execute os comandos abaixo. Verificações executadas estão em VALIDATION.md. Esta é uma implementação para validação local, não uma capacidade de produção comprovada.

## 1. Preserve banco e ambiente existentes

Pare o backend antigo (Ctrl+C). Não apague volumes e não execute docker compose down -v.
Antes de substituir arquivos, na pasta do backend antigo:

```powershell
docker exec mygamelist-postgres pg_dump -U postgres -d mygamelist -Fc -f /tmp/pre-infra.dump
if ($LASTEXITCODE -ne 0) { throw 'Backup falhou' }
docker cp mygamelist-postgres:/tmp/pre-infra.dump ..\mygamelist-pre-infra.dump
if ($LASTEXITCODE -ne 0) { throw 'Copia do backup falhou' }
docker inspect mygamelist-postgres --format '{{json .Mounts}}'
```

Registre o nome do volume. Extraia o pacote para substituir a pasta mygamelist-backend no MESMO diretório de antes, preservando .env (raiz e/ou backend). O Compose usa o nome da pasta como projeto; mudar pasta/projeto pode criar outro volume vazio. Se aparecer conflito de container ou banco vazio, pare e confira o volume antes de continuar. Um volume já inicializado exige a senha antiga de PostgreSQL; trocar DATABASE_PASSWORD não altera a senha no banco.

## 2. Configure uma vez

Pré-requisitos: JDK 17 ou 21, Docker Desktop em execução (containers Linux), Docker Compose com --wait. Maven vem pelo wrapper. API 8080; Postgres 5433; Redis 6379; RabbitMQ 5672/15672; monitoramento 9091. Internet necessária para dependências e imagens na primeira execução.

Na pasta mygamelist-backend:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Initialize-Local.ps1
notepad .env
```

O inicializador reaproveita o .env da pasta acima se ainda não houver um no backend, preserva valores existentes e gera somente segredos ausentes/CHANGE_ME para JWT, Redis, RabbitMQ e OUTBOX_KEY. Preencha DATABASE_PASSWORD, RAWG_API_KEY, SMTP_USERNAME e SMTP_PASSWORD. Para banco NOVO escolha uma senha local; para banco existente preserve a senha atual. Valores sem aspas e sem comentários no final da linha. Use credenciais SMTP/RAWG novas, não as expostas nas versões antigas.

Guarde OUTBOX_KEY de modo seguro. Trocar essa chave impede ler os e-mails pendentes; não a gere a cada execução. Ela NÃO é o segredo JWT. .env não é incluído no pacote nem deve entrar no git.

## 3. Inicie o backend

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-Local.ps1
```

Esse comando é suficiente a cada nova sessão. O uso de ExecutionPolicy se limita ao processo aberto pelo comando; não modifica a política global. O script inicia PostgreSQL, Redis e RabbitMQ, espera healthchecks, aplica scripts/001_mail_outbox.sql e inicia Spring Boot. O SQL só cria a outbox e índices, sem remover tabelas. As tabelas antigas seguem sob JPA_DDL_AUTO=update local, como antes. Migração histórica da revisão de segurança está em scripts/migration_existing_db.sql para instalações que ainda não a aplicaram.

Aguarde `Started MygamelistApplication`. Não considere somente `BUILD SUCCESS` como prova de inicialização.

Frontend: extraia também o pacote de frontend desta entrega. Na pasta mygamelist-frontend, execute `npm.cmd ci` e depois `npm.cmd start`.

## 4. Observe infraestrutura

- Aplicação: http://localhost:8080
- Frontend: http://localhost:4200
- RabbitMQ Management: http://localhost:15672 (RABBITMQ_USERNAME / RABBITMQ_PASSWORD do .env).
- Saúde geral: http://127.0.0.1:9091/actuator/health
- Métricas: http://127.0.0.1:9091/actuator/prometheus

Métricas não são expostas na interface de rede externa: a porta de gerenciamento usa loopback. Infraestrutura Docker também publica somente em 127.0.0.1. Em containers de produção, redes e bind de monitoramento precisam ser projetados explicitamente.

```powershell
docker compose --env-file .env ps
docker compose --env-file .env logs --tail 80 rabbitmq
Invoke-RestMethod http://127.0.0.1:9091/actuator/health
```

Health geral pode indicar DOWN se Rabbit/Redis estiverem fora. Liveness/readiness estão disponíveis; readiness padrão não representa toda dependência externa. Não use health geral como política de reinício cego.

## 5. Testes automatizados

Em outro PowerShell, na pasta do backend:

```powershell
. .\scripts\Load-Env.ps1
.\mvnw.cmd '-Dtest=OutboxCryptoTest,CachedRawgClientTest,MailWorkerTest,NotificationServiceTest,AchievementServiceTest,SocialServiceTest' test
```

Para suíte completa (inclui contextLoads e requer infraestrutura + tabela mail_outbox):

```powershell
.\mvnw.cmd test
```

O teste de contexto compartilha banco/filas configurados, por isso execute em ambiente LOCAL de teste, nunca com credenciais de produção. Suíte não substitui testes de concorrência PostgreSQL.

## 6. Roteiro manual de falhas

1. Cadastre uma conta de teste, confira e-mail, valide o código e faça login. Recuperação de senha também deve enviar e invalidar token conforme antes.
2. Faça a mesma busca de jogo duas vezes. Métricas mgl_rawg_cache_total devem registrar miss e hit; perfis/reviews continuam atuais, não cacheados.
3. Pare RabbitMQ com `docker compose --env-file .env stop rabbitmq`. Cadastre outra conta: API grava conta e outbox mesmo sem broker. Em menos de 15min, reinicie com `docker compose --env-file .env start rabbitmq`; envio deve ocorrer após retomada do relay.
4. Confira estados sem ler payload: `docker compose --env-file .env exec -T postgres psql -U postgres -d mygamelist -c "SELECT state,COUNT(*) FROM mail_outbox GROUP BY state;"`.
5. Teste SMTP incorreto SOMENTE em ambiente de teste: após reiniciar API e solicitar recuperação, attempts deve crescer até FAILED ou EXPIRED. Reponha SMTP, reinicie e peça NOVA recuperação. Erro SMTP não deve apagar uma conta gravada.
6. Pare Redis: buscas podem usar RAWG, mas login/registro retornam 503. Reinicie Redis e confirme recuperação. Redis não usa persistência nesta configuração, então reinício limpa cache e limites temporários.
7. Mais de 15 logins no minuto, do mesmo IP, devem retornar 429 com Retry-After. Não use sua conta real para esse teste.
8. No perfil, apenas conquistas desbloqueadas; ranking não zera na virada do mês. Verifique duas contas para isolamento de dados.
9. Reinicie backend com um e-mail pendente, confira recuperação. Mensagem já SENT não deve ser enviada de novo ao reaparecer seu ID. SMTP não oferece exatamente-uma-vez: falha após envio e antes do commit pode duplicar o e-mail.

## 7. Benchmark leve opcional

Não há promessa de usuários simultâneos. O script é um smoke benchmark local: 100 GETs com concorrência 5, tempos p50/p95 e status. Copie um token de CONTA DE TESTE e uma rota GET de busca na aba Network. Não use endpoint de mutação.

```powershell
$env:MGL_TEST_TOKEN='TOKEN_LOCAL_DE_TESTE'
$env:MGL_TEST_PATH='/ROTA_GET_DE_BUSCA_COPIADA_DO_NETWORK'
node scripts/load-smoke.mjs
Remove-Item Env:MGL_TEST_TOKEN
```

Execute com cache frio e quente; compare latência, erros, CPU, pool e métricas. Não é teste de capacidade ou soak test. Só aceita localhost:8080 e não segue redirects, para não enviar JWT a outro host.

## 8. Parar e voltar

Ctrl+C no backend. `docker compose --env-file .env stop` para infraestrutura sem apagar dados. Para voltar, execute Start-Local.ps1. Não use down -v em banco que queira preservar.

## Produção e limites honestos

- Esta composição é um ambiente local, não alta disponibilidade. RabbitMQ e Postgres são nós únicos; Redis e SMTP podem limitar capacidade.
- Redis noeviction evita que pressão do cache remova contadores de autenticação. Se atingir 128MB, cache pode falhar e autenticação fica 503; monitore memória. Próxima evolução: separar Redis de limites e Redis de cache. Credenciais/ACLs por aplicação e TLS são necessários ao sair da rede local.
- Contadores usam remoteAddr. Headers X-Forwarded-For não são confiados automaticamente. Atrás de proxy, configure proxies confiáveis e limite também no gateway; sem isso usuários podem compartilhar o IP e o limite. Não habilite confiança irrestrita em headers enviados pelo cliente.
- Configure CORS_ORIGINS com domínios reais separados por vírgula; padrão somente localhost:4200.
- Não há limite geral de requisições de usuários autenticados, circuit breaker, HA, cache stampede distribuído, orquestração ou teste de carga conclusivo. Bulkhead RAWG é por instância; múltiplas réplicas multiplicam os limites e conexões.
- Os locks de outbox usam transações curtas, mas SMTP ocupa uma conexão do banco durante envio (timeouts limitados). Dimensione pool e workers medindo; atualmente 1-2 consumidores com prefetch 5.
- Metadados FAILED são auditáveis por 7 dias; conteúdo é removido. Para expiração/falha de verificação, use POST /auth/resend-verification; ainda falta o botão na UI e alertas operacionais de backlog.
- A migração SQL é idempotente e explícita, ainda sem Flyway. Antes de produção: migrar schema inteiro para migrações versionadas, usar validate, testar backup/restauração, executar a suíte CriticalFlowsTest incluída e revisar bloqueio/denúncia e adicionar botão de reenvio de verificação.
- A fila evita perder pedido de envio quando broker está offline; não garante entrega dentro da validade se indisponibilidade for longa. Backups precisam do banco e OUTBOX_KEY separadamente.
- Acesso às métricas contém dados operacionais: mantenha a porta 9091 privada e configure um coletor seguro. A triagem de denúncias exige autenticação e ID presente em MODERATOR_IDS; a configuração não pode ser alterada pela API.

Referências oficiais consultadas: https://docs.spring.io/spring-amqp/reference/4.0/amqp/template.html ; https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html ; https://docs.spring.io/spring-boot/4.0/reference/features/json.html

## Reenviar verificação após falha ou expiração

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/auth/resend-verification -ContentType 'application/json' -Body '{"email":"sua-conta-de-teste@example.com"}'
```

Resposta é genérica para conta inexistente/ativa/pendente. Limite: 8 solicitações por minuto/IP. Use apenas o último código recebido.
