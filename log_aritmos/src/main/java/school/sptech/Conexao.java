package school.sptech;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class Conexao {

    BasicDataSource basicDataSource = new BasicDataSource();
    JdbcTemplate jdbcTemplate;

    public Conexao(){
        String dbUrl = System.getenv("jdbc:mysql:mem:logaritmos"); // Ex: jdbc:mysql://seu-host-publico.amazonaws.com:3306/logaritmos
        String dbUser = System.getenv("DB_USER"); // Ex: root
        String dbPassword = System.getenv("DB_PASSWORD"); // Ex: urubu100

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("ERRO: Variáveis de ambiente do banco de dados (DB_URL, DB_USER, DB_PASSWORD) não foram configuradas.");
            throw new IllegalStateException("Configuração de banco de dados ausente.");
        }

        basicDataSource.setUrl(dbUrl);
        basicDataSource.setUsername(dbUser);
        basicDataSource.setPassword(dbPassword);

        this.jdbcTemplate = new JdbcTemplate(basicDataSource);
    }

    public void inserirDadosVoo(List<Voo> voos){
        for (Voo vooDaVez : voos) {
            jdbcTemplate.update("INSERT INTO voo (idVoo, estado, mes, ano, qtdAeroportos, numVoosRegulares, numVoosIrregulares," +
                            "numEmbarques, numDesembarques, numVoosTotais) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    vooDaVez.getEstado(), vooDaVez.getMes(), vooDaVez.getAno(), vooDaVez.getQtdAeroportos(), vooDaVez.getNumVoosRegulares(),
                    vooDaVez.getNumVoosIrregulares(), vooDaVez.getNumEmbarques(), vooDaVez.getNumDesembarques(), vooDaVez.getNumVoosTotais());

        }
    }

    public void inserirDadosLogs(RegistroLogs registroLogs){

        jdbcTemplate.update("INSERT INTO registrologs (idLogs, categoria, descricao, dtHora)"+
                        "VALUES (DEFAULT, ?, ?, ? )", registroLogs.getCategoria(), registroLogs.getDescricao(), registroLogs.getDtHora());

    }
}
