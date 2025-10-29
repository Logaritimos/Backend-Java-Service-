package school.sptech;

import java.util.List;
import java.util.Locale;

public class VooService {

    private final Conexao conexao;
    private final LogService log;

    public VooService(Conexao conexao, LogService log) {
        this.conexao = conexao;
        this.log = log;
    }

    public void carregarDadosVoos(List<Voo> voos) {
        if (voos == null || voos.isEmpty()) {
            log.registrar("WARN", "Lista de voos vazia ou nula. Nenhum dado processado.");
            return;
        }

        int ok = 0, puladas = 0, erros = 0;
        log.registrar("INFO", String.format("Processando %d voos (linha-a-linha, sem batch)...", voos.size()));

        for (Voo v : voos) {
            try {
                if (!normalizarEAvaliar(v)) { // inválida -> pula
                    puladas++;
                    continue;
                }
                conexao.inserirVoo(v);
                ok++;
            } catch (Exception e) {
                erros++;
                log.registrar("ERROR", chave(v) + "Falha ao inserir: " + e.getMessage());
            }
        }
        log.registrar("INFO", String.format("Concluído. Inseridos=%d | Pulados=%d | Erros=%d", ok, puladas, erros));
    }

    public void carregarDado(Voo v) {
        if (!normalizarEAvaliar(v)) {
            log.registrar("WARN", chave(v) + "Linha inválida. Pulando.");
            return;
        }
        try {
            conexao.inserirVoo(v);
        } catch (Exception e) {
            log.registrar("ERROR", chave(v) + "Falha ao inserir: " + e.getMessage());
        }
    }

    private boolean normalizarEAvaliar(Voo v) {
        // Ajuste pedido: se total == 1 e houver reg/irr, usa soma
        if (v.getNumVoosTotais() != null && v.getNumVoosTotais() == 1
                && v.getNumVoosRegulares() != null && v.getNumVoosIrregulares() != null) {
            v.setNumVoosTotais(v.getNumVoosRegulares() + v.getNumVoosIrregulares());
        }

        // Críticos: estado, mês, ano
        if (isBlank(v.getEstado()) || isBlank(v.getMes()) || v.getAno() == null) return false;

        // Valores negativos → pula
        if (neg(v.getQtdAeroportos()) || neg(v.getNumVoosRegulares()) || neg(v.getNumVoosIrregulares())
                || neg(v.getNumEmbarques()) || neg(v.getNumDesembarques()) || neg(v.getNumVoosTotais())) {
            return false;
        }
        return true;
    }

    private boolean neg(Integer n) { return n != null && n < 0; }
    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private String chave(Voo v) {
        String uf = v.getEstado() == null ? "?" : v.getEstado().toUpperCase(Locale.ROOT);
        String mes = v.getMes() == null ? "?" : v.getMes();
        String ano = v.getAno() == null ? "?" : String.valueOf(v.getAno());
        return String.format("Voo[%s-%s/%s] ", uf, mes, ano);
    }
}
