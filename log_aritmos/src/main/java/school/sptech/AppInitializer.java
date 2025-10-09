package school.sptech;

public class AppInitializer {

    public static void main(String[] args) {
        Conexao conexao = new Conexao();
        LogService logService = new LogService(conexao);

        logService.registrar("INFO", "Sistema de processamento iniciado. Inicializando serviços...");

        // Inicializa Componentes; Cada serviço recebe a Conexao e o LogService.
        //VooService vooService = new VooService(conexao, logService);

        logService.registrar("INFO", "Todos os serviços foram carregados com sucesso. Iniciando fluxo de trabalho.");

        try {
            //Quando tiver todas as classes vou fazer assim:
            // --- Exemplo 1: Cadastro de nova empresa ---
            // Empresa novaEmpresa = new Empresa("AZUL Linhas Aéreas", "09249764000100");
            // empresaService.cadastrarEmpresa(novaEmpresa);

            // --- Exemplo 2: Dados em lote ---
            //List<Voo> voosDoAno = buscarDadosExternosDeVoo();
            //vooService.inserirVoos(voosDoAno);


        } catch (Exception e) {
            logService.registrar("CRITICAL", "Falha na execução principal. Encerrando de forma anormal. Detalhe: " + e.getMessage());
            e.printStackTrace();
        }finally {
            // Este bloco é executado SEMPRE, com ou sem erro.
            logService.registrar("INFO", "Execução principal concluída. Solicitando encerramento do serviço de log.");
            logService.encerrar();
        }

    }
}