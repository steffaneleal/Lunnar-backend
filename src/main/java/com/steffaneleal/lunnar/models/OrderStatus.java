// Definição dos status de pedido
package com.steffaneleal.lunnar.models;

public enum OrderStatus {
    PENDENTE,
    PAGO,
    ENVIADO,
    CONCLUIDO,
    CANCELADO,
    CANCELADO_PELO_CLIENTE
}