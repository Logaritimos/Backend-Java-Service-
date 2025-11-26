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

            Throwable root = e.getRootCause();
            if (root instanceof java.sql.SQLException sqlEx) {
                System.err.println("Mensagem JDBC: " + sqlEx.getMessage());
                System.err.println("SQLState: " + sqlEx.getSQLState());
                System.err.println("Código: " + sqlEx.getErrorCode());
            }

        } catch (Exception e) {
            System.err.println("Erro inesperado ao salvar log: " + e.getMessage());
        }
    }
}
