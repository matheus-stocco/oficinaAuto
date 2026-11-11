-- boleto passa a existir como forma de pagamento (só chega pelo Asaas)
ALTER TABLE pagamento DROP CONSTRAINT pagamento_forma_check;
ALTER TABLE pagamento ADD CONSTRAINT pagamento_forma_check CHECK (forma IN ('DINHEIRO', 'PIX', 'CARTAO', 'BOLETO'));

-- id do cliente no Asaas, para não criar o mesmo cliente a cada cobrança
ALTER TABLE cliente ADD COLUMN asaas_id VARCHAR(40);

-- cobrança online gerada para uma OS (o pagamento só nasce quando o webhook avisar que foi pago)
CREATE TABLE cobranca (
    id             BIGSERIAL PRIMARY KEY,
    os_id          BIGINT        NOT NULL REFERENCES ordem_servico (id),
    asaas_id       VARCHAR(80)   NOT NULL,
    tipo           VARCHAR(12)   NOT NULL CHECK (tipo IN ('PIX', 'BOLETO', 'CREDIT_CARD', 'UNDEFINED')),
    valor          NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    vencimento     DATE          NOT NULL,
    link           VARCHAR(500),
    status         VARCHAR(10)   NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'PAGA', 'CANCELADA', 'ESTORNADA')),
    criado_em      TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX cobranca_asaas_id_uk ON cobranca (asaas_id);
CREATE INDEX cobranca_os_idx ON cobranca (os_id);
