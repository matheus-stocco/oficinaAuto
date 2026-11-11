CREATE TABLE orcamento (
    id                    BIGSERIAL PRIMARY KEY,
    cliente_id            BIGINT REFERENCES cliente (id),
    cliente_nome          VARCHAR(120),            -- cliente avulso (quem não está cadastrado)
    cliente_telefone      VARCHAR(30),
    veiculo_id            BIGINT REFERENCES veiculo (id),
    veiculo_placa         VARCHAR(10),             -- veículo avulso
    veiculo_modelo        VARCHAR(120),
    observacoes           VARCHAR(1000),
    mao_de_obra           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (mao_de_obra >= 0),
    mao_de_obra_descricao VARCHAR(250),
    pecas_valor           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (pecas_valor >= 0),
    pecas_descricao       VARCHAR(500),
    taxa_compra_tipo      VARCHAR(10)   NOT NULL DEFAULT 'VALOR' CHECK (taxa_compra_tipo IN ('VALOR', 'PERCENTUAL')),
    taxa_compra           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (taxa_compra >= 0),
    desconto_tipo         VARCHAR(10)   NOT NULL DEFAULT 'VALOR' CHECK (desconto_tipo IN ('VALOR', 'PERCENTUAL')),
    desconto              NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (desconto >= 0),
    subtotal_servicos     NUMERIC(12,2) NOT NULL DEFAULT 0,
    taxa_compra_valor     NUMERIC(12,2) NOT NULL DEFAULT 0,
    desconto_valor        NUMERIC(12,2) NOT NULL DEFAULT 0,
    total                 NUMERIC(12,2) NOT NULL DEFAULT 0,
    receita_oficina       NUMERIC(12,2) NOT NULL DEFAULT 0,
    gerado_em             TIMESTAMP,               -- preenchido ao abrir o PDF; começa o prazo de expiração
    criado_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX orcamento_gerado_idx ON orcamento (gerado_em);
CREATE INDEX orcamento_criado_idx ON orcamento (criado_em);

CREATE TABLE orcamento_item (
    id           BIGSERIAL PRIMARY KEY,
    orcamento_id BIGINT        NOT NULL REFERENCES orcamento (id) ON DELETE CASCADE,
    servico_id   BIGINT REFERENCES servico (id),
    descricao    VARCHAR(160)  NOT NULL,
    quantidade   NUMERIC(10,2) NOT NULL CHECK (quantidade > 0),
    preco_unit   NUMERIC(12,2) NOT NULL CHECK (preco_unit >= 0),
    posicao      INTEGER       NOT NULL DEFAULT 0
);
CREATE INDEX orcamento_item_idx ON orcamento_item (orcamento_id);
