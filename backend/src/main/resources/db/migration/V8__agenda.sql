CREATE TABLE agendamento (
    id               BIGSERIAL PRIMARY KEY,
    data             DATE        NOT NULL,
    hora             TIME,                              -- opcional
    status           VARCHAR(20) NOT NULL DEFAULT 'AGENDADO'
                     CHECK (status IN ('AGENDADO', 'REALIZADO', 'CANCELADO', 'NAO_COMPARECEU')),
    cliente_id       BIGINT REFERENCES cliente (id),
    cliente_nome     VARCHAR(120),                      -- cliente avulso (sem cadastro)
    cliente_telefone VARCHAR(30),
    veiculo_id       BIGINT REFERENCES veiculo (id),
    veiculo_placa    VARCHAR(10),                       -- veículo avulso
    veiculo_modelo   VARCHAR(120),
    funcionario_id   BIGINT REFERENCES funcionario (id),
    observacoes      VARCHAR(1000),
    os_id            BIGINT REFERENCES ordem_servico (id),   -- OS aberta a partir deste agendamento
    criado_em        TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em    TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX agendamento_data_idx ON agendamento (data);
CREATE INDEX agendamento_status_idx ON agendamento (status);
CREATE INDEX agendamento_cliente_idx ON agendamento (cliente_id);

CREATE TABLE agendamento_servico (
    id             BIGSERIAL PRIMARY KEY,
    agendamento_id BIGINT       NOT NULL REFERENCES agendamento (id) ON DELETE CASCADE,
    servico_id     BIGINT REFERENCES servico (id),     -- nulo = texto livre
    descricao      VARCHAR(160) NOT NULL,
    posicao        INTEGER      NOT NULL DEFAULT 0
);
CREATE INDEX agendamento_servico_idx ON agendamento_servico (agendamento_id);
