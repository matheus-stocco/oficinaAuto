CREATE TABLE ordem_servico (
    id                    BIGSERIAL PRIMARY KEY,   -- o id é o número da OS
    status                VARCHAR(20)   NOT NULL DEFAULT 'ABERTA',
    cliente_id            BIGINT REFERENCES cliente (id),
    veiculo_id            BIGINT REFERENCES veiculo (id),
    funcionario_id        BIGINT REFERENCES funcionario (id),   -- mecânico responsável
    km_entrada            INTEGER CHECK (km_entrada >= 0),
    defeito_relatado      VARCHAR(1000),
    observacoes           VARCHAR(1000),
    previsao_entrega      DATE,
    mao_de_obra           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (mao_de_obra >= 0),
    mao_de_obra_descricao VARCHAR(250),
    pecas_valor           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (pecas_valor >= 0),
    pecas_descricao       VARCHAR(500),
    taxa_compra_tipo      VARCHAR(10)   NOT NULL DEFAULT 'VALOR' CHECK (taxa_compra_tipo IN ('VALOR', 'PERCENTUAL')),
    taxa_compra           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (taxa_compra >= 0),
    desconto_tipo         VARCHAR(10)   NOT NULL DEFAULT 'VALOR' CHECK (desconto_tipo IN ('VALOR', 'PERCENTUAL')),
    desconto              NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (desconto >= 0),
    -- totais calculados no backend a cada gravação (servem às listas e aos relatórios)
    subtotal_servicos     NUMERIC(12,2) NOT NULL DEFAULT 0,
    taxa_compra_valor     NUMERIC(12,2) NOT NULL DEFAULT 0,
    desconto_valor        NUMERIC(12,2) NOT NULL DEFAULT 0,
    total                 NUMERIC(12,2) NOT NULL DEFAULT 0,
    receita_oficina       NUMERIC(12,2) NOT NULL DEFAULT 0,
    criado_em             TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX os_status_idx ON ordem_servico (status);
CREATE INDEX os_criado_idx ON ordem_servico (criado_em);
CREATE INDEX os_cliente_idx ON ordem_servico (cliente_id);
CREATE INDEX os_veiculo_idx ON ordem_servico (veiculo_id);
CREATE INDEX os_funcionario_idx ON ordem_servico (funcionario_id);

CREATE TABLE os_item (
    id          BIGSERIAL PRIMARY KEY,
    os_id       BIGINT        NOT NULL REFERENCES ordem_servico (id) ON DELETE CASCADE,
    servico_id  BIGINT REFERENCES servico (id),   -- nulo = serviço avulso
    descricao   VARCHAR(160)  NOT NULL,           -- copiada na criação: o histórico não muda se o catálogo mudar
    quantidade  NUMERIC(10,2) NOT NULL CHECK (quantidade > 0),
    preco_unit  NUMERIC(12,2) NOT NULL CHECK (preco_unit >= 0),
    posicao     INTEGER       NOT NULL DEFAULT 0
);
CREATE INDEX os_item_os_idx ON os_item (os_id);
