import pkg from '../../package.json'

export function Manual() {
  return (
    <section className="texto">
      <h1>Manual de uso</h1>
      <h2>Menu</h2>
      <p>O menu à esquerda tem quatro grupos: Cadastros, Movimentações, Relatórios e Ajuda. Clique em um grupo para ver as telas dele. No celular, abra o menu pelo botão no topo.</p>
      <h2>Sua conta</h2>
      <p>No canto inferior esquerdo ficam o seu nome e a engrenagem. Pelo seu nome você troca a senha, alterna entre modo claro e escuro e sai da conta. A engrenagem abre as Configurações da oficina.</p>
      <h2>Configurações</h2>
      <p>Informe o nome, o CNPJ, o telefone e o endereço da oficina, e envie o logo. Esses dados aparecem no cabeçalho de todos os PDFs.</p>
    </section>
  )
}

export function Atalhos() {
  return (
    <section className="texto">
      <h1>Atalhos e dicas</h1>
      <ul>
        <li>Clique no logo, no topo do menu, para voltar ao início.</li>
        <li>Em listas, use o campo de busca no topo: ele aceita parte do nome ou do telefone.</li>
        <li>Imagens de logo e de fundo devem ser PNG ou JPG.</li>
      </ul>
    </section>
  )
}

export function Sobre() {
  return (
    <section className="texto">
      <h1>Sobre</h1>
      <p>Sistema de gestão para oficina mecânica.</p>
      <p className="suave">Versão {pkg.version}</p>
    </section>
  )
}
