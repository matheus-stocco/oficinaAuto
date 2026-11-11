CREATE TABLE funcionario (
    id                BIGSERIAL PRIMARY KEY,
    nome              VARCHAR(120) NOT NULL,
    telefone          VARCHAR(30),
    email             VARCHAR(160),
    cpf               VARCHAR(14),
    ativo             BOOLEAN      NOT NULL DEFAULT TRUE,
    login             VARCHAR(60),
    senha_hash        VARCHAR(100),
    tentativas_falhas INTEGER      NOT NULL DEFAULT 0,
    bloqueado_ate     TIMESTAMP,
    criado_em         TIMESTAMP    NOT NULL DEFAULT now(),
    -- sem acesso: login e senha nulos; com acesso: os dois preenchidos
    CONSTRAINT funcionario_acesso_ck CHECK ((login IS NULL) = (senha_hash IS NULL))
);
CREATE UNIQUE INDEX funcionario_login_uk ON funcionario (lower(login));
CREATE INDEX funcionario_nome_idx ON funcionario (lower(nome));
