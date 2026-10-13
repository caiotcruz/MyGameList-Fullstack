# MyGameList frontend — 2026-10-06

Use com o backend da mesma entrega. Node 24 e npm 11. Na pasta do projeto:

```powershell
npm.cmd ci
npm.cmd start
```

Abra http://localhost:4200. API local definida em src/environments/environment.ts. Produção usa environment.prod.ts: ajuste para seu backend antes de publicar.

Inclui cartões compactos e responsivos de perfil, conquistas desbloqueadas com Exibir mais, controles de denúncia/bloqueio, página Privacidade, preferências de mensagens e triagem de moderadores autorizados pelo backend. Reviews em lista, busca e detalhes guardam rascunho por conta/jogo na mesma aba por até 24h e o preservam se o envio falhar. Fechar a aba pode eliminar esse rascunho; não é backup permanente nem sincronização entre dispositivos.

```powershell
npm.cmd test -- --watch=false
npm.cmd run test:boundary
npm.cmd run build
```

30 testes e 12 verificações de destino do token passaram. Build passou com dois avisos de orçamento CSS. Revisão visual de celular e integração com backend real continuam pendentes; veja VALIDATION.md. O pacote do backend contém UPDATE.md com os comandos conjuntos e roteiro de conferência.
