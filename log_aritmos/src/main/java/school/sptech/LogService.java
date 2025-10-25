package school.sptech;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogService implements AutoCloseable {

    private final Conexao conexao;
    private static final String ORIGEM_APLICACAO = "SistemaDeVoos";
    private static final int LIMITE_DESCRICAO_DB = 200; // conforme DDL

    private final BlockingQueue<Runnable> fila = new LinkedBlockingQueue<>(10_000);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1,
            30, TimeUnit.SECONDS,
            fila,
            r -> {
                Thread t = new Thread(r, "log-writer-1");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
    private final AtomicBoolean encerrado = new AtomicBoolean(false);

    public LogService(Conexao conexao) {
        this.conexao = conexao;
    }

    public void registrar(String categoria, String mensagem) {
        if (encerrado.get()) {
            System.err.println("[LogService] Ignorando log após encerramento: " + mensagem);
            return;
        }
        executor.submit(() -> {
            try {
                String desc = String.format("[%s] %s", ORIGEM_APLICACAO, mensagem);
                desc = truncar(desc, LIMITE_DESCRICAO_DB);

                RegistroLogs log = new RegistroLogs(categoria, desc);
                conexao.inserirDadosLogs(log);
            } catch (Exception e) {
                System.err.println("!!! ERRO FATAL no LogService: não foi possível salvar o log no banco.");
                System.err.println("Mensagem original: " + mensagem);
                System.err.println("Erro técnico: " + e.getMessage());
            }
        });
    }

    private String truncar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    public void encerrar() {
        if (encerrado.compareAndSet(false, true)) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void close() {
        encerrar();
    }
}