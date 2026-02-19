# CRM-Inteligente-SpringBoot
Este é o servidor da plataforma Lunnar, CRM Inteligente desenvolvido durante a terceira fase do processo seletivo para a Astrocode, utilizando Spring Boot. O repositório da interface (Vue.js) pode ser encontrado [AQUI](https://github.com/steffaneleal/Lunnar-frontend).

## Como rodar o projeto localmente

### Pré-requisitos

1. Ter o **Java 17** (ou superior) instalado.
2. Ter o **PostgreSQL** instalado e rodando.
3. Ter o **Maven** instalado (ou usar o wrapper `./mvnw` incluso).

### Configuração do Banco de Dados

O projeto está configurado para usar um banco de dados chamado `lunnar_db`. O Spring Boot irá gerar as tabelas automaticamente, mas você precisa criar o banco de dados inicial.

No seu cliente SQL (pgAdmin, DBeaver ou terminal), execute:

```sql
CREATE DATABASE lunnar_db;
```

Certifique-se de que as credenciais no arquivo `.env` correspondem ao seu PostgreSQL local:
Copie o arquivo `.env.example` na raiz do projeto, o renomeie como `.env` e altere as credenciais com as suas próprias.

```dotenv
DB_HOST=host
DB_PORT=porta_do_banco
DB_NAME=nome_do_banco
DB_USERNAME=seu_usuario_do_postgres
DB_PASSWORD=sua_senha_do_postgres
JWT_SECRET=sua_chave_token_lunnar
```
