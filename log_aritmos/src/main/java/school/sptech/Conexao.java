package school.sptech;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class Conexao {

    BasicDataSource basicDataSource = new BasicDataSource();
    JdbcTemplate jdbcTemplate = new JdbcTemplate(basicDataSource);

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
