CREATE TABLE servico (
    id           BIGSERIAL PRIMARY KEY,
    nome         VARCHAR(120)  NOT NULL,
    preco_padrao NUMERIC(12,2) NOT NULL CHECK (preco_padrao >= 0),
    ativo        BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em    TIMESTAMP     NOT NULL DEFAULT now()
);
-- nome único sem diferenciar maiúsculas: evita "Balanceamento" e "balanceamento" separados
CREATE UNIQUE INDEX servico_nome_uk ON servico (lower(nome));
