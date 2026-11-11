-- Uma única linha (id = 1): os dados da oficina usados no sistema e no cabeçalho dos PDFs.
CREATE TABLE empresa (
    id                       BIGINT PRIMARY KEY CHECK (id = 1),
    nome                     VARCHAR(120) NOT NULL,
    cnpj                     VARCHAR(14),
    telefone                 VARCHAR(30),
    endereco                 VARCHAR(250),
    logo_arquivo             VARCHAR(40),
    fundo_tipo               VARCHAR(10) CHECK (fundo_tipo IN ('COR', 'IMAGEM')),
    fundo_cor                VARCHAR(7),
    fundo_arquivo            VARCHAR(40),
    orcamento_rascunho_horas INTEGER NOT NULL DEFAULT 24 CHECK (orcamento_rascunho_horas BETWEEN 1 AND 168),
    atualizado_em            TIMESTAMP NOT NULL DEFAULT now()
);
INSERT INTO empresa (id, nome) VALUES (1, 'Minha Oficina');
