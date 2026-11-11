package com.oficina.ordens;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.oficina.erro.NegocioException;
import com.oficina.pdf.CabecalhoPdf;
import com.oficina.pdf.Formato;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PDF da OS: cabeçalho padrão, serviços, mão de obra, peças com taxa de compra e total. */
@Service
public class OsPdfService {
    private static final Font TITULO = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font SECAO = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font TEXTO = new Font(Font.HELVETICA, 9);
    private static final Font NEGRITO = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font TOTAL = new Font(Font.HELVETICA, 12, Font.BOLD);

    private final OrdemServicoRepository repo;
    private final CabecalhoPdf cabecalho;

    public OsPdfService(OrdemServicoRepository repo, CabecalhoPdf cabecalho) {
        this.repo = repo;
        this.cabecalho = cabecalho;
    }

    @Transactional(readOnly = true)
    public byte[] gerar(Long id) {
        OrdemServico os = repo.findById(id).orElseThrow(
                () -> new NegocioException(HttpStatus.NOT_FOUND, "Ordem de serviço não encontrada."));
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 36, 40);
        try {
            PdfWriter.getInstance(doc, saida);
            doc.open();
            cabecalho.escrever(doc);
            escrever(doc, os);
            doc.close();
        } catch (DocumentException e) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível gerar o PDF.");
        }
        return saida.toByteArray();
    }

    private void escrever(Document doc, OrdemServico os) throws DocumentException {
        doc.add(new Paragraph("Ordem de Serviço nº " + os.getId(), TITULO));
        doc.add(linha("Situação: " + OrdemServicoService.rotulo(os.statusAtual())
                + "   |   Emissão: " + Formato.data(os.getCriadoEm().toLocalDate())
                + (os.getPrevisaoEntrega() == null ? "" : "   |   Previsão de entrega: " + Formato.data(os.getPrevisaoEntrega()))));
        doc.add(Chunk.NEWLINE);

        if (os.getCliente() != null) {
            var c = os.getCliente().getContato();
            doc.add(linha("Cliente: " + c.getNome() + (c.getTelefone() == null ? "" : "   |   Tel. " + c.getTelefone())));
        }
        if (os.getVeiculo() != null) {
            var v = os.getVeiculo();
            doc.add(linha("Veículo: " + ((v.getMarca() == null ? "" : v.getMarca() + " ") + v.getModelo()).trim()
                    + (v.getPlaca() == null ? "" : "   |   Placa " + v.getPlaca())
                    + (os.getKmEntrada() == null ? "" : "   |   Km " + os.getKmEntrada())));
        }
        if (os.getMecanico() != null) doc.add(linha("Mecânico: " + os.getMecanico().getContato().getNome()));
        if (os.getDefeitoRelatado() != null) doc.add(linha("Defeito relatado: " + os.getDefeitoRelatado()));
        doc.add(Chunk.NEWLINE);

        // 1) serviços
        doc.add(new Paragraph("Serviços", SECAO));
        PdfPTable itens = new PdfPTable(new float[]{5f, 1f, 1.6f, 1.6f});
        itens.setWidthPercentage(100);
        itens.setSpacingBefore(3);
        for (String h : new String[]{"Descrição", "Qtd.", "Preço unit.", "Subtotal"}) itens.addCell(celula(h, NEGRITO, true));
        if (os.getItens().isEmpty()) {
            PdfPCell vazio = celula("Nenhum serviço lançado.", TEXTO, false);
            vazio.setColspan(4);
            itens.addCell(vazio);
        }
        for (OsItem i : os.getItens()) {
            itens.addCell(celula(i.getDescricao(), TEXTO, false));
            itens.addCell(celula(i.getQuantidade().stripTrailingZeros().toPlainString(), TEXTO, false));
            itens.addCell(celula(Formato.moeda(i.getPrecoUnit()), TEXTO, false));
            itens.addCell(celula(Formato.moeda(i.getQuantidade().multiply(i.getPrecoUnit()).setScale(2, RoundingMode.HALF_UP)), TEXTO, false));
        }
        doc.add(itens);
        doc.add(Chunk.NEWLINE);

        // 2) mão de obra  3) peças e taxa
        PdfPTable valores = new PdfPTable(new float[]{5f, 2f});
        valores.setWidthPercentage(100);
        valores.addCell(celula("Mão de obra" + (os.getMaoDeObraDescricao() == null ? "" : " - " + os.getMaoDeObraDescricao()), TEXTO, false));
        valores.addCell(direita(Formato.moeda(os.getMaoDeObra()), TEXTO));
        valores.addCell(celula("Peças" + (os.getPecasDescricao() == null ? "" : " - " + os.getPecasDescricao()), TEXTO, false));
        valores.addCell(direita(Formato.moeda(os.getPecasValor()), TEXTO));
        if (os.getTaxaCompraValor().signum() > 0) {
            valores.addCell(celula("Taxa de compra das peças"
                    + (os.getTaxaCompraTipo() == TipoValor.PERCENTUAL ? " (" + os.getTaxaCompra().stripTrailingZeros().toPlainString() + "%)" : ""), TEXTO, false));
            valores.addCell(direita(Formato.moeda(os.getTaxaCompraValor()), TEXTO));
        }
        if (os.getDescontoValor().signum() > 0) {
            valores.addCell(celula("Desconto"
                    + (os.getDescontoTipo() == TipoValor.PERCENTUAL ? " (" + os.getDesconto().stripTrailingZeros().toPlainString() + "%)" : ""), TEXTO, false));
            valores.addCell(direita("- " + Formato.moeda(os.getDescontoValor()), TEXTO));
        }
        doc.add(valores);

        Paragraph total = new Paragraph("TOTAL: " + Formato.moeda(os.getTotal()), TOTAL);
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(10);
        doc.add(total);

        if (os.getObservacoes() != null) {
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Observações", SECAO));
            doc.add(new Paragraph(os.getObservacoes(), TEXTO));
        }
    }

    private static Paragraph linha(String texto) {
        return new Paragraph(texto, TEXTO);
    }

    private static PdfPCell celula(String texto, Font fonte, boolean cabecalho) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte));
        c.setPadding(4);
        c.setBorderWidth(0.4f);
        c.setBorderColor(java.awt.Color.LIGHT_GRAY);
        if (cabecalho) c.setBackgroundColor(new java.awt.Color(240, 240, 242));
        return c;
    }

    private static PdfPCell direita(String texto, Font fonte) {
        PdfPCell c = celula(texto, fonte, false);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }
}
