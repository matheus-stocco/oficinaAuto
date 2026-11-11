package com.oficina.ordens;

import static com.oficina.pdf.BlocoValoresPdf.SECAO;
import static com.oficina.pdf.BlocoValoresPdf.TEXTO;

import com.lowagie.text.*;
import com.oficina.erro.NegocioException;
import com.oficina.pdf.BlocoValoresPdf;
import com.oficina.pdf.Formato;
import com.oficina.pdf.PdfGerador;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PDF da OS: cabeçalho padrão, dados do atendimento, serviços, mão de obra, peças com taxa de compra e total. */
@Service
public class OsPdfService {
    private static final Font TITULO = new Font(Font.HELVETICA, 13, Font.BOLD);

    private final OrdemServicoRepository repo;
    private final PdfGerador gerador;

    public OsPdfService(OrdemServicoRepository repo, PdfGerador gerador) {
        this.repo = repo;
        this.gerador = gerador;
    }

    @Transactional(readOnly = true)
    public byte[] gerar(Long id) {
        OrdemServico os = repo.findById(id).orElseThrow(
                () -> new NegocioException(HttpStatus.NOT_FOUND, "Ordem de serviço não encontrada."));
        return gerador.gerar(doc -> escrever(doc, os));
    }

    private void escrever(Document doc, OrdemServico os) throws DocumentException {
        doc.add(new Paragraph("Ordem de Serviço nº " + os.getId(), TITULO));
        doc.add(new Paragraph("Situação: " + OrdemServicoService.rotulo(os.statusAtual())
                + "   |   Emissão: " + Formato.data(os.getCriadoEm().toLocalDate())
                + (os.getPrevisaoEntrega() == null ? "" : "   |   Previsão de entrega: " + Formato.data(os.getPrevisaoEntrega())), TEXTO));
        doc.add(Chunk.NEWLINE);

        if (os.getCliente() != null) {
            var c = os.getCliente().getContato();
            doc.add(new Paragraph("Cliente: " + c.getNome() + (c.getTelefone() == null ? "" : "   |   Tel. " + c.getTelefone()), TEXTO));
        }
        if (os.getVeiculo() != null) {
            var v = os.getVeiculo();
            doc.add(new Paragraph("Veículo: " + ((v.getMarca() == null ? "" : v.getMarca() + " ") + v.getModelo()).trim()
                    + (v.getPlaca() == null ? "" : "   |   Placa " + v.getPlaca())
                    + (os.getKmEntrada() == null ? "" : "   |   Km " + os.getKmEntrada()), TEXTO));
        }
        if (os.getMecanico() != null) doc.add(new Paragraph("Mecânico: " + os.getMecanico().getContato().getNome(), TEXTO));
        if (os.getDefeitoRelatado() != null) doc.add(new Paragraph("Defeito relatado: " + os.getDefeitoRelatado(), TEXTO));
        doc.add(Chunk.NEWLINE);

        BlocoValoresPdf.escrever(doc, os.getItens(), os);

        if (os.getObservacoes() != null) {
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Observações", SECAO));
            doc.add(new Paragraph(os.getObservacoes(), TEXTO));
        }
    }
}
