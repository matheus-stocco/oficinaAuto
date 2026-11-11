package com.oficina.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.oficina.ordens.ItemDocumento;
import com.oficina.ordens.TipoValor;
import com.oficina.ordens.ValoresDocumento;
import java.math.RoundingMode;
import java.util.List;

/** Tabela de serviços, mão de obra, peças, taxa, desconto e total: igual em OS e orçamento. */
public final class BlocoValoresPdf {
    public static final Font SECAO = new Font(Font.HELVETICA, 10, Font.BOLD);
    public static final Font TEXTO = new Font(Font.HELVETICA, 9);
    private static final Font NEGRITO = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font TOTAL = new Font(Font.HELVETICA, 12, Font.BOLD);

    private BlocoValoresPdf() {}

    public static void escrever(Document doc, List<? extends ItemDocumento> itens, ValoresDocumento v) throws DocumentException {
        doc.add(new Paragraph("Serviços", SECAO));
        PdfPTable tabela = new PdfPTable(new float[]{5f, 1f, 1.6f, 1.6f});
        tabela.setWidthPercentage(100);
        tabela.setSpacingBefore(3);
        for (String h : new String[]{"Descrição", "Qtd.", "Preço unit.", "Subtotal"}) tabela.addCell(celula(h, NEGRITO, true));
        if (itens.isEmpty()) {
            PdfPCell vazio = celula("Nenhum serviço lançado.", TEXTO, false);
            vazio.setColspan(4);
            tabela.addCell(vazio);
        }
        for (ItemDocumento i : itens) {
            tabela.addCell(celula(i.getDescricao(), TEXTO, false));
            tabela.addCell(celula(i.getQuantidade().stripTrailingZeros().toPlainString(), TEXTO, false));
            tabela.addCell(celula(Formato.moeda(i.getPrecoUnit()), TEXTO, false));
            tabela.addCell(celula(Formato.moeda(i.getQuantidade().multiply(i.getPrecoUnit()).setScale(2, RoundingMode.HALF_UP)), TEXTO, false));
        }
        doc.add(tabela);
        doc.add(Chunk.NEWLINE);

        PdfPTable valores = new PdfPTable(new float[]{5f, 2f});
        valores.setWidthPercentage(100);
        valores.addCell(celula("Mão de obra" + (v.getMaoDeObraDescricao() == null ? "" : " - " + v.getMaoDeObraDescricao()), TEXTO, false));
        valores.addCell(direita(Formato.moeda(v.getMaoDeObra())));
        valores.addCell(celula("Peças" + (v.getPecasDescricao() == null ? "" : " - " + v.getPecasDescricao()), TEXTO, false));
        valores.addCell(direita(Formato.moeda(v.getPecasValor())));
        if (v.getTaxaCompraValor().signum() > 0) {
            valores.addCell(celula("Taxa de compra das peças" + percentual(v.getTaxaCompraTipo(), v.getTaxaCompra()), TEXTO, false));
            valores.addCell(direita(Formato.moeda(v.getTaxaCompraValor())));
        }
        if (v.getDescontoValor().signum() > 0) {
            valores.addCell(celula("Desconto" + percentual(v.getDescontoTipo(), v.getDesconto()), TEXTO, false));
            valores.addCell(direita("- " + Formato.moeda(v.getDescontoValor())));
        }
        doc.add(valores);

        Paragraph total = new Paragraph("TOTAL: " + Formato.moeda(v.getTotal()), TOTAL);
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(10);
        doc.add(total);
    }

    private static String percentual(TipoValor tipo, java.math.BigDecimal valor) {
        return tipo == TipoValor.PERCENTUAL ? " (" + valor.stripTrailingZeros().toPlainString() + "%)" : "";
    }

    private static PdfPCell celula(String texto, Font fonte, boolean cabecalho) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte));
        c.setPadding(4);
        c.setBorderWidth(0.4f);
        c.setBorderColor(java.awt.Color.LIGHT_GRAY);
        if (cabecalho) c.setBackgroundColor(new java.awt.Color(240, 240, 242));
        return c;
    }

    private static PdfPCell direita(String texto) {
        PdfPCell c = celula(texto, TEXTO, false);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }
}
