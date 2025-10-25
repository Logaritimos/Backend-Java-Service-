package school.sptech;

import java.nio.file.Path;
import java.util.List;

public class AppInitializer {

    public static void main(String[] args) {
        // Caminho do XLSX por variável de ambiente (fallback para arquivo no diretório atual)
        String excelPath = System.getenv("EXCEL_PATH");
        Path arquivo = Path.of(excelPath != null ? excelPath : "dicionario_de_dadosV2.0.xlsx");

        try (Conexao conexao = new Conexao()) {
            LogService logService = new LogService(conexao);
            try {
                logService.registrar("INFO", "Sistema de processamento iniciado. Inicializando serviços...");

                VooService vooService = new VooService(conexao, logService);
                LeitorExcell reader = new LeitorExcell();

                // Processa a planilha em lotes de 1000 linhas
                reader.processar(arquivo, 1000, (List<Voo> lote) -> {
                    vooService.carregarDadosVoos(lote);
                }, logService);

                logService.registrar("INFO", "Fluxo principal concluído com sucesso.");
            } catch (Exception e) {
                logService.registrar("CRITICAL",
                        "Falha na execução principal. Encerrando de forma anormal. Detalhe: " + e.getMessage());
                e.printStackTrace();
            } finally {
                logService.registrar("INFO", "Encerrando serviço de log.");
                logService.encerrar();
            }
        } catch (Exception e) {
            System.err.println("Falha ao iniciar/encerrar recursos: " + e.getMessage());
        }
    }
}
