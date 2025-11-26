package school.sptech;

import org.springframework.dao.DataAccessException;
import java.time.LocalDateTime;

public class LogService {

    private final Conexao conexao;
    private static final int LIMITE_DESCRICAO = 200;

    public LogService(Conexao conexao) {
        this.conexao = conexao;
    }

    public void registrar(String categoria, String mensagem) {
        try {
            String desc = String.format("[%s] %s", "SistemaDeVoos", mensagem);
            if (desc.length() > LIMITE_DESCRICAO) {
                desc = desc.substring(0, LIMITE_DESCRICAO);
            }

            RegistroLogs log = new RegistroLogs(categoria, desc);
            log.setDtHora(LocalDateTime.now());
            conexao.inserirDadosLogs(log);

        } catch (DataAccessException e) {
        System.err.println("Falha ao salvar log (Spring): " + e.getMessage());

        // percorre toda a cadeia de causas
        Throwable cause = e;
        int nivel = 1;
        while (cause != null) {
            System.err.println("ERROR [ " + nivel + " ]: " + cause.getClass().getName() +
                    " - " + cause.getMessage());
            cause = cause.getCause();
            nivel++;
        }
    }
 catch (Exception e) {
            System.err.println("Erro inesperado ao salvar log: " + e.getMessage());
        }
    }
}
