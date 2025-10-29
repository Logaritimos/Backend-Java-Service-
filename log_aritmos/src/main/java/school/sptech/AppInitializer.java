package school.sptech;

import java.nio.file.Path;

public class AppInitializer {

    public static void main(String[] args) {
        String excelPath = System.getenv("EXCEL_PATH");
        if ((excelPath == null || excelPath.isBlank()) && args.length > 0) {
            excelPath = args[0];
        }

        try (Conexao conexao = new Conexao()) {
            LogService logService = new LogService(conexao);

            try {
                logService.registrar("INFO", "Sistema de processamento iniciado. Inicializando serviços...");

                LeitorExcell leitor = new LeitorExcell();
                leitor.processar(Path.of(excelPath), conexao, logService);

                logService.registrar("INFO", "Fluxo principal concluído com sucesso.");
            } catch (Exception e) {
                logService.registrar("CRITICAL", "Falha na execução principal: " + e.getMessage());
                e.printStackTrace();
            } finally {
                logService.registrar("INFO", "Encerrando serviço de log.");
                logService.encerrar();
            }
        } catch (Exception e) {
            System.err.println("Falha ao iniciar/encerrar recursos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}