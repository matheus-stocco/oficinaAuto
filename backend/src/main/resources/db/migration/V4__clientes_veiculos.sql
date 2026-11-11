CREATE TABLE cliente (
    id               BIGSERIAL PRIMARY KEY,
    nome             VARCHAR(120) NOT NULL,
    telefone         VARCHAR(30),
    telefone_digitos VARCHAR(30),
    email            VARCHAR(160),
    documento        VARCHAR(18),
    endereco         VARCHAR(250),
    observacoes      VARCHAR(500),
    anonimizado_em   TIMESTAMP,
    criado_em        TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX cliente_nome_idx ON cliente (lower(nome));
CREATE INDEX cliente_telefone_idx ON cliente (telefone_digitos);

CREATE TABLE veiculo (
    id         BIGSERIAL PRIMARY KEY,
    placa      VARCHAR(7),
    marca      VARCHAR(60),
    modelo     VARCHAR(120) NOT NULL,
    ano        VARCHAR(30),
    cor        VARCHAR(30),
    cliente_id BIGINT REFERENCES cliente (id),
    criado_em  TIMESTAMP NOT NULL DEFAULT now()
);
-- placa única quando informada (vários veículos "sem placa" são permitidos)
CREATE UNIQUE INDEX veiculo_placa_uk ON veiculo (placa) WHERE placa IS NOT NULL;
CREATE INDEX veiculo_cliente_idx ON veiculo (cliente_id);

CREATE TABLE auditoria (
    id             BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT,
    acao           VARCHAR(60) NOT NULL,
    detalhe        VARCHAR(300),
    criado_em      TIMESTAMP NOT NULL DEFAULT now()
);
