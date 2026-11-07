# Backend: API do Sistema de Provas

API REST em Spring Boot que gerencia usuários, provas, perguntas, alternativas e tentativas, com autenticação por JWT e geração de relatórios em PDF com JasperReports.

## Tecnologias

- Java 21
- Spring Boot 4 (Web, Data JPA, Security, Actuator)
- PostgreSQL 18
- JWT (biblioteca `com.auth0:java-jwt`)
- JasperReports
- JUnit 5 e Mockito
- Maven (wrapper `./mvnw` incluído)

## Variáveis de ambiente

Todas as configurações são lidas de variáveis de ambiente. Quando uma variável não existe, o `application.properties` usa um valor padrão pensado para desenvolvimento local.

| Variável | Para que serve |
|---|---|
| `DB_URL` | URL JDBC do banco, por exemplo `jdbc:postgresql://localhost:5432/nome_do_banco` |
| `DB_USERNAME` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave usada para assinar e validar os tokens JWT |
| `CORS_ORIGINS` | Endereço do frontend que pode chamar a API |
| `PORT` | Porta da aplicação (padrão `8080`) |

> **Em produção, defina sempre o `JWT_SECRET`** com uma chave longa e aleatória, por exemplo gerada com `openssl rand -base64 32`. Nunca use o valor padrão do repositório, que é público.

## Rodando localmente

Com um PostgreSQL rodando na máquina:

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. As tabelas são criadas e atualizadas automaticamente pelo Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

Para verificar se a aplicação está no ar:

```bash
curl http://localhost:8080/actuator/health
```

## Testes

```bash
./mvnw test
```

Os testes unitários usam JUnit 5 e Mockito e ficam em `src/test/java`, no mesmo pacote das classes testadas. Os Services são testados de forma isolada: os repositórios são substituídos por mocks, e o usuário logado é simulado pelo `SecurityContextHolder`.

O `./mvnw package` também roda os testes antes de gerar o `.jar`, e é esse comando que o CI executa.

## Docker

O `Dockerfile` usa **multi-stage build**: o primeiro estágio compila o projeto com Maven, e o segundo copia apenas o `.jar` para uma imagem menor, só com o Java necessário para rodar.

```bash
docker build -t curricularizacao-backend .
docker run -p 8080:8080 \
  -e DB_URL=... \
  -e DB_USERNAME=... \
  -e DB_PASSWORD=... \
  -e JWT_SECRET=... \
  curricularizacao-backend
```

O build da imagem usa `-DskipTests` por dois motivos: os testes já rodam no CI, e durante o `docker build` não existe banco disponível, então o teste `contextLoads` falharia. Por isso o deploy é disparado pelo job de CD do GitHub Actions, que só roda depois que os testes passam.
