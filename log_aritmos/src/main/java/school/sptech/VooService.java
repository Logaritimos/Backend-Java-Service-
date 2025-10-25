package school.sptech;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class VooService {

    private final Conexao conexao;
    private final LogService logService;

    public VooService(Conexao conexao, LogService logService) {
        this.conexao = conexao;
        this.logService = logService;
    }

    public void carregarDadosVoos(List<Voo> voos) {
        if (voos == null || voos.isEmpty()) {
            logService.registrar("WARN", "Lista de voos vazia ou nula. Nenhum dado processado.");
            return;
        }

        logService.registrar("INFO",
                String.format("Iniciando validação e carregamento de %d registros de voos.", voos.size()));

        List<String> erros = validar(voos);
        if (!erros.isEmpty()) {
            String resumo = erros.stream().limit(5).collect(Collectors.joining(" | "));
            logService.registrar("ERROR",
                    "Falha na validação: " + erros.size() + " inconsistências. Ex.: " + resumo);
            return;
        }

        try {
            conexao.inserirDadosVooBatch(voos);
            logService.registrar("INFO",
                    String.format("Carregamento concluído: %d voos inseridos no DB.", voos.size()));
        } catch (Exception e) {
            logService.registrar("CRITICAL",
                    "Falha crítica na Conexão durante o carregamento de voos. Detalhe: " + e.getMessage());
            throw new RuntimeException("Falha no carregamento de dados de Voo.", e);
        }
    }

    private List<String> validar(List<Voo> voos) {
        List<String> erros = new ArrayList<>();
        for (Voo v : voos) {
            if (v.getEstado() == null || v.getEstado().trim().isEmpty()) {
                erros.add(chave(v) + "Estado ausente");
            }
            if (v.getMes() == null || v.getMes().trim().isEmpty()) {
                erros.add(chave(v) + "Mês ausente");
            }
            if (v.getAno() == null || v.getAno() < 1900 || v.getAno() > 2100) {
                erros.add(chave(v) + "Ano fora do intervalo 1900..2100");
            }
            if (neg(v.getQtdAeroportos())) erros.add(chave(v) + "Qtd. aeroportos negativa");
            if (neg(v.getNumVoosRegulares())) erros.add(chave(v) + "Voos regulares negativos");
            if (neg(v.getNumVoosIrregulares())) erros.add(chave(v) + "Voos irregulares negativos");
            if (neg(v.getNumEmbarques())) erros.add(chave(v) + "Embarques negativos");
            if (neg(v.getNumDesembarques())) erros.add(chave(v) + "Desembarques negativos");
            if (neg(v.getNumVoosTotais())) erros.add(chave(v) + "Total de voos negativo");
            if (v.getNumVoosTotais() != null
                    && v.getNumVoosRegulares() != null
                    && v.getNumVoosIrregulares() != null
                    && v.getNumVoosTotais() != (v.getNumVoosRegulares() + v.getNumVoosIrregulares())) {
                erros.add(chave(v) + "Total != regulares + irregulares");
            }
        }
        return erros;
    }

    private boolean neg(Integer n) { return n != null && n < 0; }

    private String chave(Voo v) {
        String uf = v.getEstado() == null ? "?" : v.getEstado().toUpperCase(Locale.ROOT);
        String mes = v.getMes() == null ? "?" : v.getMes();
        String ano = v.getAno() == null ? "?" : String.valueOf(v.getAno());
        return String.format("Registro[%s-%s/%s]: ", uf, mes, ano);
    }
}