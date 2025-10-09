package school.sptech;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogService {

    private final Conexao conexao;
    private String ORIGEM_APLICACAO = "SistemaDeVoos";
    //threads para executar comandos em segundo plano (async)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LogService(Conexao conexao) {
        this.conexao = conexao;
    }

    public void registrar(String categoria, String mensagem) {
        // Tarefa a ser executada em segundo plano
        Runnable tarefaDeLog = () -> {
            try {
                RegistroLogs log = new RegistroLogs(categoria, String.format("[%s] %s", ORIGEM_APLICACAO, mensagem));
                conexao.inserirDadosLogs(log);
            } catch (Exception e) {
                System.err.println("!!! ERRO FATAL no LogService: Não foi possível salvar o log no banco.");
                System.err.println("Mensagem original: " + mensagem);
                System.err.println("Erro técnico: " + e.getMessage());
            }
        };
        // Envia a tarefa para ser executada pela thread do executor
        executor.submit(tarefaDeLog);
    }
    //encerrar o executor!
    public void encerrar() {
        executor.shutdown();
    }
}