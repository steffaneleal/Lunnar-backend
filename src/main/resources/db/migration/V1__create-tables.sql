-- V1: Script de Criação das Tabelas Iniciais

-- Tabela de Usuários (para autenticação)
CREATE TABLE tb_user (
                         id UUID PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         email VARCHAR(255) NOT NULL UNIQUE,
                         password VARCHAR(255) NOT NULL,
                         provider VARCHAR(50),
                         birthdate DATE,
                         phone_number VARCHAR(50),
                         role VARCHAR(50) NOT NULL,
                         created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

-- Tabela de Clientes (perfil de negócio do usuário)
CREATE TABLE tb_customer (
                             id UUID PRIMARY KEY,
                             user_id UUID NOT NULL UNIQUE,
                             company_name VARCHAR(255),
                             notes TEXT,
                             last_contact_at TIMESTAMP WITHOUT TIME ZONE,
                             created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                             updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                             CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
);

-- Tabela de Endereços (pertence a um cliente)
CREATE TABLE tb_address (
                            id UUID PRIMARY KEY,
                            customer_id UUID,
                            street VARCHAR(255),
                            "number" VARCHAR(50),
                            complement VARCHAR(255),
                            neighborhood VARCHAR(255),
                            city VARCHAR(255),
                            state VARCHAR(100),
                            zip_code VARCHAR(20),
                            CONSTRAINT fk_address_customer FOREIGN KEY (customer_id) REFERENCES tb_customer(id)
);

-- Tabela de Categorias de Produtos
CREATE TABLE tb_category (
                             id UUID PRIMARY KEY,
                             name VARCHAR(255) NOT NULL UNIQUE
);

-- Tabela de Produtos
CREATE TABLE tb_product (
                            id UUID PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            description TEXT,
                            price NUMERIC(19, 2) NOT NULL,
                            stock_quantity INTEGER,
                            image_url VARCHAR(255),
                            category_id UUID,
                            CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES tb_category(id)
);

-- Tabela de Pedidos
CREATE TABLE tb_order (
                          id UUID PRIMARY KEY,
                          user_id UUID,
                          shipping_address_id UUID NOT NULL,
                          total_price NUMERIC(19, 2) NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                          CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES tb_user(id),
                          CONSTRAINT fk_order_shipping_address FOREIGN KEY (shipping_address_id) REFERENCES tb_address(id)
);

-- Tabela de Itens do Pedido (tabela de junção entre Pedido e Produto)
CREATE TABLE tb_order_item (
                               id UUID PRIMARY KEY,
                               order_id UUID NOT NULL,
                               product_id UUID NOT NULL,
                               quantity INTEGER NOT NULL,
                               price NUMERIC(19, 2) NOT NULL,
                               CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES tb_order(id),
                               CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES tb_product(id)
);
