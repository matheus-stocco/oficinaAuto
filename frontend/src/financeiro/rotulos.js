// Boleto só chega pelo Asaas, mas pode ser lançado à mão se o cliente pagou um boleto fora do sistema.
export const FORMAS = { DINHEIRO: 'Dinheiro', PIX: 'Pix', CARTAO: 'Cartão', BOLETO: 'Boleto' }

// Formas oferecidas nos formulários de pagamento manual.
export const FORMAS_MANUAIS = { DINHEIRO: 'Dinheiro', PIX: 'Pix', CARTAO: 'Cartão' }

export const TIPOS_COBRANCA = { PIX: 'Pix', BOLETO: 'Boleto', CREDIT_CARD: 'Cartão de crédito', UNDEFINED: 'O cliente escolhe' }

export const STATUS_COBRANCA = { PENDENTE: 'Pendente', PAGA: 'Paga', CANCELADA: 'Cancelada', ESTORNADA: 'Estornada' }

export const dataBr = (iso) => (iso ? iso.split('-').reverse().join('/') : '')
