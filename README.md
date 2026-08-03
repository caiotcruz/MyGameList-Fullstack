# MyGameList

Uma plataforma social para catalogar, avaliar e discutir jogos — uma mistura de lista de jogos pessoal com rede social, no espírito de sites como MyAnimeList ou Letterboxd, mas voltada para games.

Full-stack: backend em **Spring Boot (Java)** com **PostgreSQL/JPA**, frontend em **Angular**, e integração com a **RAWG API** para dados de jogos (capas, descrições, DLCs, conquistas, screenshots, trailers, lojas).

---

## Visão geral

O MyGameList permite que cada usuário monte sua própria biblioteca de jogos, marcando status (planeja jogar, jogando, zerado, platinado, largado), dando notas e escrevendo reviews. Em cima disso, existe uma camada social: seguir outros usuários, ver um feed de atividades de quem você segue, curtir e comentar, e trocar mensagens diretas com pessoas que você segue ou que te seguem.

O projeto também tem um sistema de progressão (level/XP) que recompensa engajamento real com a plataforma — jogar, avaliar, interagir — e um design deliberado para resistir a formas óbvias de exploit desse sistema.

---

## Stack técnica

**Backend**
- Spring Boot (Java)
- Spring Data JPA / Hibernate
- Spring Security (autenticação via token)
- Feign Client para integração com a RAWG API
- PostgreSQL (via JPA)

**Frontend**
- Angular (standalone components)
- Font Awesome para iconografia
- Identidade visual própria: fundo escuro (`#050505`), acento roxo (`#6200ea`), cards em glass-morphism, tipografia Inter

**API externa**
- [RAWG Video Games Database API](https://rawg.io/apidocs) — dados de jogos, DLCs, jogos da mesma série, screenshots, trailers, lojas e conquistas

---

## Funcionalidades

### Biblioteca de jogos
Cada usuário mantém sua própria lista, com status (`PLAN_TO_PLAY`, `PLAYING`, `COMPLETED`, `PLATINUM`, `DROPPED`), nota (0–10), review em texto livre, e marcação de jogo favorito. Jogos são armazenados localmente após a primeira busca na RAWG (cache local), evitando chamadas repetidas à API externa para o mesmo jogo.

### Página de detalhes do jogo (hub)
Reúne, além dos dados básicos, estatísticas da comunidade (total de jogadores, quantos estão jogando/zeraram/platinaram), distribuição de notas em gráfico de barras, nota do Metacritic, e as reviews mais recentes com sistema de like/dislike (karma). Inclui também dados enriquecidos direto da RAWG: galeria de screenshots com lightbox navegável, trailers em player embutido, DLCs e edições especiais, jogos da mesma série, lojas onde comprar, e conquistas (ordenadas por raridade).

### Feed de atividades
Mostra o que o usuário e as pessoas que ele segue estão fazendo: adicionar um jogo, mudar de status, avaliar, escrever review. Ações relacionadas de uma mesma sessão (por exemplo, adicionar um jogo já zerado com nota e review de uma vez) são agrupadas em um único card, via um `groupId` gerado no momento da criação — não por heurística de proximidade de tempo, evitando falsos agrupamentos ou fragmentação por atraso de rede.

Cada card de atividade permite curtir e comentar.

### Sistema de spoiler
Reviews podem ser marcadas como contendo spoiler. O texto some por trás de um aviso, revelado só com um clique — tanto no feed quanto na página do jogo.

### Sistema de level e experiência (XP)
Usuários ganham experiência por ações de engajamento real: completar/platinar jogos, escrever reviews, curtir e comentar atividades de outras pessoas, seguir e ser seguido. A conversão de XP para nível é constante (mesma quantidade de XP para qualquer transição de nível), e cada ação tem um valor fixo e simétrico — ganhar uma ação concede X de XP, desfazer essa mesma ação revoga exatamente X, nunca mais nem menos. Isso vale inclusive para mudanças de status "compostas" (por exemplo, pular direto de "jogando" para "platinado" concede de uma vez o XP equivalente a todas as etapas puladas).

O sistema tem proteções explícitas contra exploit:
- XP de like/comentário não é concedido em interações com a própria atividade
- Curtir/descurtir e postar/apagar sempre revertem exatamente o que concederam
- Recompensas de ações sociais (seguir, ser seguido) são calibradas propositalmente mais baixas que ações de conteúdo (jogar, revisar), para que "farmar" rede social nunca seja mais eficiente que engajar de verdade com jogos

### Rede social
Seguir/deixar de seguir outros usuários (relação não necessariamente mútua). Perfil de cada usuário mostra estatísticas de carreira (total de jogos, zerados, platinas, média de notas), contagem de seguidores/seguindo com listas navegáveis, e a coleção completa de jogos.

### Mensagens diretas ("correio")
Sistema de mensagens assíncronas (não é chat em tempo real) entre usuários que têm uma relação de "seguir" em qualquer direção. A tela de mensagens combina conversas existentes com sugestões de pessoas para iniciar uma conversa (extraídas da lista de seguidores/seguindo), evitando uma tela vazia para quem ainda não trocou nenhuma mensagem.

### Jogos em destaque (trending)
Ranking de jogos baseado em atividade real da própria plataforma no mês corrente (adições à lista + reviews escritas), não em métricas externas da RAWG — reflete o que a comunidade do site está jogando agora.

---

## Decisões de arquitetura e lições aprendidas

Alguns padrões e decisões que guiaram o desenvolvimento e vale documentar para manutenção futura:

**Disciplina de DTOs.** Nenhum endpoint serializa entities JPA diretamente. Isso começou como correção de um vazamento de dados sensíveis (senha e outros campos de `User` sendo expostos em respostas que sequer precisavam desses dados) e virou padrão em todo o projeto — cada endpoint expõe exatamente os campos que o consumidor precisa, nunca a entity crua.

**Espelhamento de ações reversíveis.** Sempre que uma ação concede algo (XP, uma atividade, um contador), a ação inversa precisa desfazer exatamente o que foi concedido — nunca uma lógica separada e potencialmente dessincronizada para "desfazer".

**Matemática de delta em vez de eventos isolados.** Para valores que dependem de estado (como XP por status de jogo), o cálculo compara o estado antes/depois e aplica a diferença, em vez de reagir a cada clique isoladamente — isso evita exploits óbvios como clicar "salvar" repetidamente sem mudar nada.

**Guarda de autointeração.** Ações que geram recompensa (XP, notificação) verificam explicitamente se o ator não é o mesmo usuário-alvo, evitando ganhos artificiais via interação consigo mesmo.

**Cuidado com dados legados.** Mudanças de schema que adicionam significado a dados já existentes (como o `groupId` de atividades ou o `metacritic` de jogos já cadastrados) não retroagem automaticamente — dados antigos exigem backfill explícito, tratado como uma decisão separada e deliberada, não como efeito colateral silencioso.

---

## Estrutura de pacotes (backend)

```
model/           entities JPA
model/enums/     enums de domínio (status de jogo, tipo de atividade, fonte de XP...)
dto/             DTOs de saída da API
dto/rawg/        DTOs de desserialização das respostas da RAWG (isolados dos DTOs públicos)
repository/      interfaces Spring Data JPA
service/         regras de negócio
controller/      endpoints REST
client/          cliente Feign para a RAWG API
```
