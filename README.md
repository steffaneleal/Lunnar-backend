# Lunnar Backend - CRM Inteligente

Este é o servidor da plataforma Lunnar, um CRM Inteligente desenvolvido durante a terceira fase do processo seletivo para a Astrocode, utilizando Spring Boot. O repositório da interface (Vue.js) pode ser encontrado [AQUI](https://github.com/steffaneleal/Lunnar-frontend).

## Sobre o Projeto

O Lunnar Backend é a espinha dorsal da aplicação CRM, responsável por gerenciar dados de usuários, clientes, produtos, categorias, pedidos e endereços. Ele fornece uma API RESTful segura e escalável para o frontend, garantindo a integridade e a consistência das informações.

## Tecnologias Utilizadas

*   **Spring Boot**: Framework para construção de aplicações Java robustas.
*   **Spring Security**: Para autenticação (JWT) e autorização baseada em roles (ADMIN, USER).
*   **Spring Data JPA / Hibernate**: Para persistência de dados e mapeamento objeto-relacional.
*   **PostgreSQL**: Banco de dados relacional.
*   **Springdoc OpenAPI (Swagger UI)**: Geração automática de documentação interativa da API.
*   **Lombok**: Para reduzir o código boilerplate.
*   **Java JWT**: Para manipulação de JSON Web Tokens.
*   **BCrypt**: Para criptografia de senhas.
*   **Maven**: Gerenciador de dependências e build.

## Funcionalidades Principais

*   **Autenticação e Autorização**: Login de usuários, registro, geração de JWTs, e controle de acesso baseado em roles (ADMIN, USER).
*   **Gestão de Usuários e Clientes**: Criação e atualização de perfis de usuário e cliente, com distinção entre usuários de autenticação e perfis de cliente CRM.
*   **Gestão de Endereços**: Adição, listagem e remoção de múltiplos endereços por cliente, com proteção para não excluir endereços que estão em uso por pedidos.
*   **Gestão de Produtos e Categorias**: CRUD completo para produtos e categorias, com proteção para não excluir produtos ou categorias que estão em uso por pedidos.
*   **Gestão de Pedidos**:
    *   Criação, listagem e busca de pedidos com cálculo automático de totais.
    *   Atualização de status de pedidos por administradores.
    *   Cancelamento de pedidos pelo próprio cliente (com devolução de estoque).
    *   Lógica de reativação de pedidos cancelados (com re-subtração de estoque).
*   **Upload de Imagens**: Suporte para upload de imagens e serviço de arquivos estáticos.
*   **Relatórios de Clientes**: Geração de relatórios detalhados para administradores.

## Como Rodar o Projeto Localmente

### Pré-requisitos

1.  Ter o **Java 17** (ou superior) instalado.
2.  Ter o **PostgreSQL** (versão 16 ou inferior) instalado e rodando.
3.  Ter o **Maven** instalado (ou usar o wrapper `./mvnw` incluso).

### Configuração do Ambiente

#### 1. Banco de Dados PostgreSQL

O projeto utiliza o PostgreSQL. Você precisa ter uma instância rodando e um banco de dados criado.

*   **Crie o banco de dados:** No seu cliente SQL (pgAdmin, DBeaver ou terminal), execute:
    ```sql
    CREATE DATABASE lunnar_db;
    ```
    *   **Importante:** O Hibernate gerenciará as tabelas automaticamente, mas o banco de dados `lunnar_db` precisa existir antes da aplicação iniciar.

#### 2. Variáveis de Ambiente (`.env`)

Para segurança e flexibilidade, as configurações sensíveis são carregadas de um arquivo `.env`.

*   Copie o arquivo `.env.example` na raiz do projeto, renomeie-o como `.env` e altere as credenciais com as suas próprias.

    ```dotenv
    DB_HOST=localhost
    DB_PORT=5432
    DB_NAME=lunnar_db
    DB_USERNAME=postgres
    DB_PASSWORD=sua_senha_do_postgres
    JWT_SECRET=sua_chave_secreta_para_jwt
    CORS_ALLOWED_ORIGINS=url_do_frontend (http://localhost:3000, por exemplo)
    BASE_URL=url_do_backend (http://localhost:3000, por exemplo)
    ```
    *   **`JWT_SECRET`**: Use uma string longa e complexa para a chave secreta do JWT.
    *   **`DB_PASSWORD`**: A senha do seu usuário `postgres` no PostgreSQL.

#### 3. Pasta de Uploads

*   Crie uma pasta chamada `uploads` na raiz do projeto. É aqui que as imagens enviadas serão armazenadas e servidas como recursos estáticos.

### Executando a Aplicação

#### Com Maven (diretamente)

1.  Abra o terminal na raiz do projeto (`Lunnar-backend`).
2.  Execute o comando:
    ```bash
    ./mvnw spring-boot:run
    ```

### Documentação da API (Swagger UI)

Após a aplicação estar rodando, você pode acessar a documentação interativa da API através do Swagger UI:

*   **URL:** `http://localhost:8080/swagger-ui.html`

Para testar endpoints protegidos:
1.  Faça login em `/auth/login` (pode ser pelo próprio Swagger UI).
2.  Copie o token JWT da resposta.
3.  Clique no botão "Authorize" (canto superior direito), cole o token no campo (no formato `Bearer SEU_TOKEN_AQUI`) e clique em "Authorize".

### Recursos Estáticos (Uploads)

As imagens enviadas para a API são armazenadas na pasta `uploads/` e podem ser acessadas diretamente via URL:

*   **URL:** `http://localhost:8080/uploads/nome_da_imagem.jpg`

### APIs Externas

O projeto utiliza a seguinte API externa:

*   **ViaCEP**: Para consulta de informações de endereço a partir de um CEP. Embora a integração direta não esteja visível nos controllers atuais, a estrutura de dados e a intenção de uso para preenchimento automático de endereços estão presentes.
    *   **URL**: `https://viacep.com.br`

---
