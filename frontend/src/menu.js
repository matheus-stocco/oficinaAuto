import { ArrowLeftRight, BookOpen, ChartColumn, Users } from 'lucide-react'

// Menu único do sistema: 4 grupos que expandem. `pronto` = tela já implementada.
export const GRUPOS = [
  {
    nome: 'Cadastros', icone: Users, itens: [
      { nome: 'Clientes', rota: '/clientes', pronto: true },
      { nome: 'Veículos', rota: '/veiculos', pronto: true },
      { nome: 'Funcionários', rota: '/funcionarios', pronto: true },
      { nome: 'Serviços', rota: '/servicos', pronto: true },
    ],
  },
  {
    nome: 'Movimentações', icone: ArrowLeftRight, itens: [
      { nome: 'Ordens de Serviço', rota: '/ordens', pronto: true },
      { nome: 'Orçamentos', rota: '/orcamentos', pronto: true },
      { nome: 'Agenda', rota: '/agenda', pronto: true },
      { nome: 'Financeiro', rota: '/financeiro' },
    ],
  },
  {
    nome: 'Relatórios', icone: ChartColumn, itens: [
      { nome: 'Financeiro por período', rota: '/relatorios/financeiro' },
      { nome: 'Por funcionário', rota: '/relatorios/funcionarios' },
      { nome: 'Por serviço executado', rota: '/relatorios/servicos' },
      { nome: 'Formas de pagamento', rota: '/relatorios/pagamentos' },
      { nome: 'Contas a receber', rota: '/relatorios/receber' },
      { nome: 'Despesas por categoria', rota: '/relatorios/despesas' },
      { nome: 'OS por status', rota: '/relatorios/status' },
      { nome: 'Histórico de OS', rota: '/relatorios/historico' },
    ],
  },
  {
    nome: 'Ajuda', icone: BookOpen, itens: [
      { nome: 'Manual de uso', rota: '/ajuda/manual', pronto: true },
      { nome: 'Atalhos e dicas', rota: '/ajuda/atalhos', pronto: true },
      { nome: 'Sobre', rota: '/ajuda/sobre', pronto: true },
    ],
  },
]
