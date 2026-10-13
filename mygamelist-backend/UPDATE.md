# Atualizar e rodar — Windows / PowerShell

Esta entrega usa ZIP, extraível pelo Windows. São dois projetos completos. Pare os processos anteriores, faça backup do banco conforme README e substitua os arquivos das mesmas pastas. Preserve o .env e os volumes Docker. Não extraia uma pasta mygamelist-backend dentro de outra de mesmo nome.

## Backend

Pré-requisitos: JDK 17 ou 21 e Docker Desktop iniciado. A configuração Redis/RabbitMQ da versão anterior continua válida. Na pasta do backend:

```powershell
cd C:\Users\caiot\Desktop\Projetos\java\Gamereviewd\mygamelist-backend
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Initialize-Local.ps1
notepad .env
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-Local.ps1
```

O inicializador preserva segredos existentes. Não troque OUTBOX_KEY nem a senha do PostgreSQL já inicializado. MODERATOR_IDS é opcional; veja MIGRATION.md. Espere `Started MygamelistApplication`. API em localhost:8080, saúde em 127.0.0.1:9091/actuator/health.

## Frontend, em outro terminal

Node 24. Na pasta do frontend:

```powershell
cd C:\Users\caiot\Desktop\Projetos\java\Gamereviewd\mygamelist-frontend
npm.cmd ci
npm.cmd start
```

Abra http://localhost:4200. Aguarde `npm.cmd ci` terminar sem interromper; `ng serve` precisa das dependências completas. Se o Windows bloquear a limpeza de node_modules, feche os processos deste frontend que estejam usando a pasta e tente novamente. Não apague package-lock.json.

## Testes do backend, em um terceiro terminal

```powershell
cd C:\Users\caiot\Desktop\Projetos\java\Gamereviewd\mygamelist-backend
. .\scripts\Load-Env.ps1
.\mvnw.cmd '-Dtest=OutboxCryptoTest,CachedRawgClientTest,MailWorkerTest,NotificationServiceTest,AchievementServiceTest,SocialServiceTest' test
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Test-Critical.ps1
```

O segundo comando de teste usa **mygamelist_critical_tests**, banco descartável dedicado na porta 5433. O Hibernate cria e remove suas tabelas. Nunca coloque dados que queira preservar nesse banco. A suíte não usa DATABASE_URL para escolher o destino, não envia e-mail e usa jogos locais de fixture. Requer PostgreSQL do Compose e as variáveis do .env; não é teste de carga conclusivo.

São oito testes de integração HTTP/PostgreSQL cobrindo XP simultâneo no mesmo jogo e em jogos diferentes, favorito simultâneo e restrição no banco, isolamento de lista/conversa/notificação, preferências, bloqueio e denúncias. Sem RUN_CRITICAL_TESTS=true, essa classe é ignorada; o script habilita a variável e a remove ao terminar. Consulte target/surefire-reports para resultados reais. Esses testes ainda não foram executados no ambiente desta entrega.

## Testes do frontend

```powershell
cd C:\Users\caiot\Desktop\Projetos\java\Gamereviewd\mygamelist-frontend
npm.cmd test -- --watch=false
npm.cmd run test:boundary
npm.cmd run build
```

## Conferência funcional e celular

1. Entre com duas contas de teste. Em Privacidade, alterne todos / pessoas que sigo / ninguém e confira o envio pela outra conta. No padrão, o destinatário precisa seguir o remetente.
2. Bloqueie uma conta: perfis/interações e conversa devem ficar indisponíveis entre o par. Desbloqueie em Privacidade e confira que follows não foram restaurados automaticamente.
3. Envie uma denúncia de teste. Com um moderador configurado, abra a triagem, leia a evidência e registre a decisão. Resolver não remove conteúdo automaticamente.
4. Edite uma review, interrompa temporariamente o backend e tente salvar. Texto e erro devem permanecer; reabra o formulário na mesma aba e tente novamente após iniciar o backend.
5. No navegador, confira 360, 390, 768 e 1280 px: perfil sem rolagem horizontal, três cartões compactos no desktop e empilhados no celular; títulos longos; conquistas vazias e muitas conquistas; abrir/fechar Exibir mais; formulário de denúncia e teclado no chat.

A revisão visual em navegador não foi concluída aqui por timeout da ferramenta. Os testes de componentes não substituem essa conferência.
