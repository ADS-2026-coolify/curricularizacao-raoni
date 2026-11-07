# Curricularização: Sistema de Provas sobre Métodos Ágeis

Aplicação web usada no projeto de curricularização da FEMA: um minicurso sobre métodos ágeis em que os participantes fazem uma prova online ao final. O administrador cadastra as provas, perguntas e alternativas; os participantes fazem a prova e o sistema corrige as respostas automaticamente.

Este repositório também é a entrega da tarefa de **CI/CD com GitHub Actions e Coolify** da disciplina.

## Estrutura

```
.
├── backend/                  API REST em Spring Boot
├── frontend/                 Interface em Angular
└── .github/workflows/
    └── ci-cd.yml             Pipeline de CI/CD
```

O backend tem um README próprio, em [`backend/README.md`](backend/README.md).

## Tecnologias

| Parte | Tecnologias |
|---|---|
| Backend | Java 21, Spring Boot 4, Maven, Spring Security com JWT, JPA/Hibernate, JasperReports |
| Banco de dados | PostgreSQL 18 |
| Frontend | Angular 21, PrimeNG, Tailwind CSS |
| Testes | JUnit 5, Mockito |
| CI/CD | GitHub Actions, Coolify |

## Rodando localmente

### Backend

Pré-requisitos: Java 21 e um PostgreSQL rodando na máquina.

```bash
cd backend
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. As configurações são lidas de variáveis de ambiente, com valores padrão para desenvolvimento local (veja o [README do backend](backend/README.md)).

### Frontend

Pré-requisito: Node.js 20 ou superior.

```bash
cd frontend
npm ci
npx ng serve
```

O site abre em `http://localhost:4200` e, em desenvolvimento, chama a API em `http://localhost:8080/api` (definido em `src/environments/environment.development.ts`).

## CI/CD

O pipeline fica em `.github/workflows/ci-cd.yml` e roda a cada `git push`, em qualquer branch.

```
git push
   │
   ├── carregar_backend   Java 21 + PostgreSQL de teste + ./mvnw package (compila e roda os testes)
   └── carregar_frontend  Node 20 + npm ci + npm run build
          │
          ▼
       deploy             só na main e só se os dois jobs acima passarem
          │
          ├── chama o webhook de deploy do backend no Coolify
          └── chama o webhook de deploy do frontend no Coolify
```

**CI (integração contínua):** os jobs `carregar_backend` e `carregar_frontend` compilam o projeto e rodam os testes em todas as branches. O backend usa um PostgreSQL temporário criado pelo próprio GitHub Actions (service container).

**CD (implantação contínua):** o job `deploy` usa `needs` para esperar os dois jobs de CI e `if: github.ref == 'refs/heads/main'` para rodar apenas na `main`. Ele chama o webhook de deploy do Coolify com `curl --fail-with-body`, para que qualquer erro HTTP deixe o job vermelho e mostre o motivo no log.

O deploy automático do próprio Coolify fica **desligado** nos dois recursos. Como o Dockerfile do backend usa `-DskipTests`, é o CD que garante que só código com os testes passando chegue à produção.

### Secrets e variables do GitHub

| Nome | Tipo | Conteúdo |
|---|---|---|
| `COOLIFY_TOKEN` | Secret | Token da API do Coolify com permissão apenas de deploy, restrito aos recursos `raoni-backend` e `raoni-frontend` |
| `COOLIFY_WEBHOOK_BACKEND` | Variable | URL do webhook de deploy do `raoni-backend` |
| `COOLIFY_WEBHOOK_FRONTEND` | Variable | URL do webhook de deploy do `raoni-frontend` |

## Deploy no Coolify

Os recursos ficam no projeto **BCC**, ambiente **production**:

| Recurso | Tipo | Configuração principal |
|---|---|---|
| `raoni-postgres` | PostgreSQL 18 | Acesso privado (só pela rede interna), dados em volume |
| `raoni-backend` | Dockerfile | Base directory `/backend`, porta 8080, variáveis `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` e `CORS_ORIGINS` |
| `raoni-frontend` | Railpack, site estático | Base directory `/frontend`, publish directory `/dist/frontCurricularizacao/browser`, porta 80 |

Endereços:

- Backend: `https://raoni-backend.187.33.201.107.sslip.io`
- Frontend: `https://raoni-frontend.187.33.201.107.sslip.io`

O frontend chama a API pelo endereço definido em `frontend/src/environments/environment.ts`, que é usado no build de produção.

## Situação atual

- Banco, backend e frontend estão rodando no Coolify.
- O CI passa em todas as branches, e o `deploy` é pulado corretamente fora da `main`.
- O job `deploy` hoje retorna `403 API is disabled.`: o webhook de deploy depende da API do Coolify, que precisa ser ativada pelo administrador da instância.
- Os domínios `sslip.io` ainda não chegam aos containers: o nginx do servidor redireciona os acessos `https` para o painel do Coolify.
