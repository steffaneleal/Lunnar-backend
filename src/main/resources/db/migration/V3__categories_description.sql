-- V3: Adiciona a coluna de endereço de entrega à tabela de pedidos
ALTER TABLE tb_order ADD COLUMN shipping_address_id UUID;

-- Adiciona a restrição de chave estrangeira
ALTER TABLE tb_order ADD CONSTRAINT fk_order_shipping_address FOREIGN KEY (shipping_address_id) REFERENCES tb_address(id);