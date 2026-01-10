# 🎮 MyGameList

O **MyGameList** é uma plataforma social fullstack para gerenciamento de bibliotecas de jogos. Permite aos usuários buscar jogos, criar listas personalizadas (jogando, zerado, planejo jogar), avaliar títulos e visualizar o perfil de outros jogadores.

Projeto desenvolvido para fins de estudo de arquitetura Fullstack Moderna.

## 🚀 Tecnologias Utilizadas

### Backend (API REST)
* **Java 17** & **Spring Boot 4**
* **Spring Security + JWT** (Autenticação e Autorização)
* **Spring Data JPA** & **PostgreSQL** (Persistência)
* **OpenFeign** (Integração com API externa RAWG)
* **Docker** (Containerização para Deploy)

### Frontend (SPA)
* **Angular 17+** (Standalone Components)
* **Angular Material** & **CSS3** (Interface Responsiva)
* **Integração de API** (Services, Interceptors)
* **Features Avançadas**: Infinite Scroll, Dashboards, Modais.

---

## ✨ Funcionalidades

* 🔐 **Autenticação Segura:** Login e Cadastro com criptografia e Tokens JWT.
* 🔎 **Busca Integrada:** Pesquisa em tempo real consumindo a API da RAWG (+800k jogos).
* 📜 **Infinity Scroll:** Carregamento dinâmico de resultados na busca (Paginação).
* 📋 **Gerenciamento de Lista:** Adicionar, Atualizar Status (Playing, Completed, etc), Dar Nota e Review.
* 📊 **Dashboard:** Estatísticas automáticas de quantos jogos o usuário zerou ou está jogando.
* 🌍 **Comunidade:** Listagem de usuários e visualização de perfis de amigos (Read-Only).

---

## 🛠️ Como rodar localmente

### Pré-requisitos
* Java 17+
* Node.js v18+
* PostgreSQL

### 1. Backend
1.  Configure o banco de dados PostgreSQL no arquivo `application.properties`.
2.  Adicione sua API Key da RAWG no arquivo `application.properties`.
3.  Execute o projeto Spring Boot.

```bash
cd mygamelist-backend
./mvnw spring-boot:run
```

### 2. Frontend
Instale as dependências e rode o servidor Angular.

```bash
cd mygamelist-frontend
npm install
ng serve
```

Acesse http://localhost:4200.
## 📝 Autor
Caio Cruz

[GitHub](https://github.com/caiotcruz/)

Projeto desenvolvido para fins de estudo em Spring Boot e Arquitetura de Software.
