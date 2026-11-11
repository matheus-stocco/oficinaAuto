CREATE TABLE pagamento (
    id           BIGSERIAL PRIMARY KEY,
    os_id        BIGINT        NOT NULL REFERENCES ordem_servico (id),
    valor        NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    forma        VARCHAR(10)   NOT NULL CHECK (forma IN ('DINHEIRO', 'PIX', 'CARTAO')),
    data         DATE          NOT NULL,                  -- dia do recebimento (entra no caixa deste dia)
    origem       VARCHAR(10)   NOT NULL DEFAULT 'MANUAL' CHECK (origem IN ('MANUAL', 'ASAAS')),
    id_externo   VARCHAR(80),                             -- id do pagamento no Asaas, para não registrar duas vezes
    estornado_em TIMESTAMP,                               -- estornado deixa de contar no caixa e no saldo
    criado_em    TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX pagamento_id_externo_uk ON pagamento (id_externo) WHERE id_externo IS NOT NULL;
CREATE INDEX pagamento_os_idx ON pagamento (os_id);
CREATE INDEX pagamento_data_idx ON pagamento (data);

-- fiado: valor que o cliente ficou devendo, com vencimento
CREATE TABLE receber (
    id           BIGSERIAL PRIMARY KEY,
    os_id        BIGINT        NOT NULL REFERENCES ordem_servico (id),
    valor        NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    vencimento   DATE          NOT NULL,
    status       VARCHAR(10)   NOT NULL DEFAULT 'ABERTO' CHECK (status IN ('ABERTO', 'PAGO', 'CANCELADO')),
    pago_em      DATE,
    pagamento_id BIGINT REFERENCES pagamento (id),        -- pagamento gerado pela baixa
    criado_em    TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX receber_os_idx ON receber (os_id);
CREATE INDEX receber_status_venc_idx ON receber (status, vencimento);

CREATE TABLE despesa (
    id        BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(160)  NOT NULL,
    valor     NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    data      DATE          NOT NULL,
    categoria VARCHAR(60),
    criado_em TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX despesa_data_idx ON despesa (data);
CREATE INDEX despesa_categoria_idx ON despesa (categoria);
