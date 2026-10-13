# MyGameList

Plataforma social para catalogar, avaliar e descobrir jogos. Cada usuário organiza sua biblioteca, escreve reviews, acompanha atividades da comunidade e desenvolve seu perfil com níveis, XP e conquistas.

Projeto full-stack de **Caio Teixeira da Cruz**, desenvolvido com Java, Spring Boot, Angular e PostgreSQL. Esta versão prioriza **execução local**, consistência transacional, privacidade e processamento assíncrono de e-mails.

## Funcionalidades

- **Biblioteca pessoal:** status de jogo, notas de 0 a 10, reviews, spoilers e um favorito por usuário.
- **Catálogo RAWG:** busca, detalhes, imagens, trailers, lojas e conteúdos relacionados.
- **Comunidade:** seguir usuários, feed de atividades agrupadas, curtidas, comentários e reviews com votos.
- **Em alta:** ranking calculado sobre atividades dos últimos **30 dias**, sem reiniciar na virada do mês.
- **Perfil:** carreira, jogo em destaque, XP, níveis e conquistas desbloqueadas. Exibição compacta com “Exibir mais” e descrição ao passar o mouse; conquistas ainda bloqueadas permanecem ocultas.
- **Mensagens:** conversas e contagem de não lidas, respeitando a preferência do destinatário: todos, pessoas que ele segue ou ninguém. O padrão é pessoas que o destinatário segue.
- **Proteção da comunidade:** bloqueio bidirecional e denúncias de perfis, atividades, comentários, reviews e mensagens. Controles contextuais em “Mais opções”.
- **Moderação:** triagem de denúncias com evidência, estado e nota de análise. Acesso autorizado no servidor por `MODERATOR_IDS`.
- **Experiência:** tema escuro com destaque verde-água, layout responsivo, mensagens de erro consistentes, atualização de notificações na primeira abertura e rascunhos de reviews preservados após falha de envio.

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Backend | Java 17, Spring Boot 4.0.1, Spring Security, Spring Data JPA, OpenFeign |
| Frontend | Angular 21, TypeScript, componentes standalone, RxJS |
| Banco de dados | PostgreSQL 15 |
| Cache e limites de autenticação | Redis 7.4 |
| Mensageria | RabbitMQ 4.1 e Spring AMQP |
| Observabilidade | Spring Boot Actuator, Micrometer e endpoint Prometheus |
| Testes | JUnit 5, Mockito, testes HTTP/PostgreSQL, Vitest e Angular TestBed |
| Ambiente local | Docker Compose, Maven Wrapper e scripts PowerShell |

## Decisões de arquitetura

**Consistência de XP e favorito.** Alterações usam transações e bloqueios por usuário, com ordenação de IDs nas operações entre duas pessoas. XP é calculado pelo delta entre estados. Um índice único parcial no PostgreSQL reforça a regra de no máximo um favorito por usuário.

**Cache compartilhado.** Respostas públicas do catálogo RAWG são armazenadas no Redis, com expiração por tipo de consulta. As chaves usam hash; dados privados e decisões de autorização não entram nesse cache. Uma falha no cache permite consultar a RAWG diretamente, com limites de concorrência e timeouts.

**E-mails fora da requisição principal.** A operação de autenticação e o pedido de e-mail são registrados na mesma transação PostgreSQL. Um relay publica o identificador da outbox no RabbitMQ; o consumidor processa o envio com retentativas, expiração e controle de estado. O conteúdo da outbox usa AES-256-GCM. O fluxo não promete entrega exatamente uma vez: uma falha entre envio SMTP e confirmação no banco pode gerar duplicidade.

**Segurança.** Autenticação JWT, senhas com BCrypt, validações de propriedade, configuração por variáveis de ambiente, CORS configurável e limites de autenticação no Redis. O interceptor do frontend restringe o envio do token ao destino da API. Se o Redis de autenticação estiver indisponível, as rotas protegidas pelo limitador retornam indisponibilidade em vez de ignorar a proteção.

**Privacidade.** Bloqueios são verificados no backend e removem os follows entre o par. Denúncias de mensagens exigem participação na conversa. Marcar uma denúncia como resolvida registra uma decisão; não remove conteúdo nem aplica banimento automaticamente.

**Rascunhos.** Reviews são guardadas em `sessionStorage`, por conta e jogo, por até 24 horas. Salvamento confirmado ou descarte remove o rascunho. Fechar a aba pode eliminá-lo; não há sincronização entre dispositivos.

## Estrutura

- `mygamelist-backend/`: API, regras de negócio, persistência, integrações, infraestrutura Docker e scripts locais.
- `mygamelist-frontend/`: aplicação Angular, componentes, páginas, serviços e testes.
- `mygamelist-backend/MIGRATION.md`: atualização de banco existente e configuração de moderadores.
- `mygamelist-backend/scripts/`: inicialização, carregamento de ambiente, testes críticos e SQL auxiliar.

## Executar localmente

### Pré-requisitos

- JDK 17 ou 21, com `java` disponível no terminal.
- Node.js 24 e npm.
- Docker Desktop iniciado, com containers Linux e Docker Compose com suporte a `--wait`.
- Chave da RAWG e credenciais SMTP para os fluxos de verificação e recuperação de conta.

Os comandos abaixo usam PowerShell no Windows. O Maven é fornecido pelo wrapper do projeto. A primeira execução precisa de internet para baixar dependências e imagens.

### 1. Obter o projeto

```powershell
git clone https://github.com/caiotcruz/MyGameList-Fullstack.git
cd MyGameList-Fullstack
```

### 2. Configurar o backend

```powershell
cd .\mygamelist-backend
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Initialize-Local.ps1
notepad .env
```

Preencha `DATABASE_PASSWORD`, `RAWG_API_KEY`, `SMTP_USERNAME` e `SMTP_PASSWORD`. O inicializador gera os segredos ausentes e preserva valores existentes. `.env.example` documenta a configuração; `.env` não deve ser versionado.

Se o banco já existe, preserve sua senha e o volume Docker. Preserve também `OUTBOX_KEY`: ela é necessária para ler e-mails pendentes na outbox. Antes de atualizar uma base existente, faça backup e leia [MIGRATION.md](mygamelist-backend/MIGRATION.md). Não use `docker compose down -v` se quiser manter os dados.

Para permitir triagem de denúncias, configure opcionalmente `MODERATOR_IDS` com IDs numéricos separados por vírgula e reinicie a API. Sem essa configuração, nenhuma conta recebe acesso à moderação.

### 3. Iniciar o backend

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-Local.ps1
```

O script carrega `.env`, inicia PostgreSQL, Redis e RabbitMQ, aguarda os healthchecks, aplica o SQL idempotente da outbox e inicia o Spring Boot. No ambiente local padrão, o Hibernate atualiza o schema e o inicializador da comunidade cria os índices adicionais. Aguarde `Started MygamelistApplication`.

### 4. Iniciar o frontend

Em outro terminal, na raiz do repositório:

```powershell
cd .\mygamelist-frontend
npm.cmd ci
npm.cmd start
```

| Serviço | Endereço local |
| --- | --- |
| Aplicação Angular | http://localhost:4200 |
| API | http://localhost:8080 |
| PostgreSQL | localhost:5433 |
| Redis | localhost:6379 |
| RabbitMQ Management | http://localhost:15672 |
| Saúde da API | http://127.0.0.1:9091/actuator/health |
| Métricas Prometheus | http://127.0.0.1:9091/actuator/prometheus |

Para acessar o RabbitMQ Management, use `RABBITMQ_USERNAME` e `RABBITMQ_PASSWORD` do `.env`. O Compose publica a infraestrutura em loopback. Não é necessário configurar Vercel, Render ou banco online para seguir este guia.

### Parar e retomar

Use `Ctrl+C` nos terminais da aplicação. Na pasta do backend, `docker compose --env-file .env stop` interrompe a infraestrutura preservando os volumes. Para retomar, execute novamente `Start-Local.ps1` e `npm.cmd start`.

## Testes

### Backend: testes unitários

Na pasta `mygamelist-backend`:

```powershell
. .\scripts\Load-Env.ps1
.\mvnw.cmd '-Dtest=OutboxCryptoTest,CachedRawgClientTest,MailWorkerTest,NotificationServiceTest,AchievementServiceTest,SocialServiceTest' test
```

### Backend: integração e concorrência

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Test-Critical.ps1
```

A suíte usa o banco **descartável** `mygamelist_critical_tests`, na porta 5433, com criação e remoção de tabelas. Não armazene dados permanentes nele. Os cenários cobrem XP sob operações simultâneas, favorito único, isolamento de lista/conversa/notificação, preferências de mensagens, bloqueios e denúncias. Sem a variável habilitada pelo script, essa classe é ignorada.

### Frontend

Na pasta `mygamelist-frontend`:

```powershell
npm.cmd test -- --watch=false
npm.cmd run test:boundary
npm.cmd run build
```

### Estado da validação

Na revisão de outubro de 2026, foram registrados 11 testes unitários aprovados no backend e 32 testes aprovados no frontend, incluindo regressões de notificações e preservação de reviews. O build do frontend passou com avisos de tamanho de CSS em perfil e detalhes do jogo. A execução completa da suíte crítica após a correção do script PowerShell ainda precisa ser confirmada. Esses resultados não representam um teste de capacidade sob carga nem uma validação visual completa.

## Próximos passos

- Concluir a validação de integração e concorrência no ambiente local.
- Revisar os fluxos com teclado e telas móveis.
- Automatizar build e testes em integração contínua.
- Adotar migrações versionadas e ampliar a observabilidade de falhas e backlog de e-mails.
- Medir latência e consumo de recursos antes de definir metas de capacidade ou preparar implantação online.

A infraestrutura atual é adequada a desenvolvimento e demonstração local. Cache e mensageria não tornam a composição automaticamente altamente disponível: os serviços do Compose usam instâncias únicas.

## Autor

[Caio Teixeira da Cruz](https://github.com/caiotcruz)
