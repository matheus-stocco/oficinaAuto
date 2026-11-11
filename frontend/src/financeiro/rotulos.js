export const FORMAS = { DINHEIRO: 'Dinheiro', PIX: 'Pix', CARTAO: 'Cartão' }

export const dataBr = (iso) => (iso ? iso.split('-').reverse().join('/') : '')
