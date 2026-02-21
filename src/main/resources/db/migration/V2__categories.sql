-- V2: Migra produto para múltiplas categorias

-- 1. Criar tabela de junção
CREATE TABLE tb_product_category (
                                     product_id UUID NOT NULL,
                                     category_id UUID NOT NULL,
                                     PRIMARY KEY (product_id, category_id),
                                     CONSTRAINT fk_pc_product FOREIGN KEY (product_id) REFERENCES tb_product(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_pc_category FOREIGN KEY (category_id) REFERENCES tb_category(id) ON DELETE CASCADE
);

-- 2. Migrar dados existentes (produtos que já tinham category_id)
INSERT INTO tb_product_category (product_id, category_id)
SELECT id, category_id FROM tb_product WHERE category_id IS NOT NULL;

-- 3. Remover coluna antiga
ALTER TABLE tb_product DROP COLUMN category_id;