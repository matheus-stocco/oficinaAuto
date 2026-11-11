export const ROTULO_STATUS = {
  ABERTA: 'Aberta', EM_EXECUCAO: 'Em execução', PRONTA: 'Pronta', ENTREGUE: 'Entregue', CANCELADA: 'Cancelada',
}

// Ações disponíveis em cada estado. Espelha StatusOs do backend, que é quem valida.
export const ACOES = {
  ABERTA: [{ para: 'EM_EXECUCAO', rotulo: 'Iniciar execução' }, { para: 'CANCELADA', rotulo: 'Cancelar OS', confirmar: true }],
  EM_EXECUCAO: [{ para: 'PRONTA', rotulo: 'Marcar como pronta' }, { para: 'CANCELADA', rotulo: 'Cancelar OS', confirmar: true }],
  PRONTA: [{ para: 'ENTREGUE', rotulo: 'Registrar entrega' }, { para: 'CANCELADA', rotulo: 'Cancelar OS', confirmar: true }],
  ENTREGUE: [{ para: 'PRONTA', rotulo: 'Reabrir OS', confirmar: true }],
  CANCELADA: [],
}

export const EDITAVEL = new Set(['ABERTA', 'EM_EXECUCAO', 'PRONTA'])
