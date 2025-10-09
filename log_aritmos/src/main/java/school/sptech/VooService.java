package school.sptech;

import java.util.List;

public class VooService {

    private final Conexao conexao;
    private final LogService logService;

    public VooService(Conexao conexao, LogService logService) {
        this.conexao = conexao;
        this.logService = logService;
    }

    public void carregarDadosVoos(List<Voo> voos) {

        logService.registrar("INFO", String.format("Iniciando validação e carregamento de %d registros de voos.", voos.size()));

        if (voos == null || voos.isEmpty()) {
            logService.registrar("WARN", "Lista de voos vazia, encerrando carregamento.");
            return;
        }

        if (voosContemDadosInvalidos(voos)) {
            logService.registrar("ERROR", "Falha na validação: dados de entrada do bucket estão inconsistentes. A inserção foi abortada."
            );
            return;
        }
        try {
            conexao.inserirDadosVoo(voos);

            logService.registrar("SUCCESS",
                    String.format("Carregamento concluído: %d voos inseridos no DB.", voos.size())
            );

        } catch (Exception e) {
            logService.registrar("ERROR",
                    "Falha crítica na Conexão durante o carregamento de voos. Detalhe: " + e.getMessage()
            );
            throw new RuntimeException("Falha no carregamento de dados de Voo.", e);
        }
    }

    private boolean voosContemDadosInvalidos(List<Voo> voos) {
        // ... sua lógica de verificação de campos obrigatórios, formatos, etc. ...
        return voos.stream().anyMatch(v -> v.getMes() == null || v.getMes().trim().isEmpty());
    }
}