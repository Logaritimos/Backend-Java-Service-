package school.sptech;

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
        } catch (Exception e) {
            System.err.println("Falha ao salvar log: " + e.getMessage());
        }
    }
}