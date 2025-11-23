package school.sptech;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

import static school.sptech.LeitorArquivo.semAcento;

public class Conexao implements AutoCloseable {

    private final BasicDataSource dataSource = new BasicDataSource();
    private final JdbcTemplate jdbcTemplate;

    public Conexao() {
        // Variáveis de ambiente (SEM espaços nos nomes)
        String dbUrl = "jdbc:mysql://127.0.0.1:3306/logaritmos?useSSL=false&serverTimezone=UTC";
        String dbUser = "root";
        String dbPassword = "urubu100";

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("ERRO: Variáveis de ambiente (DB_URL, DB_USER, DB_PASSWORD) não configuradas.");
            throw new IllegalStateException("Configuração de banco de dados ausente.");
        }

        // Driver e credenciais
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);

        // Pool (mantém estável mesmo inserindo linha a linha)
        dataSource.setInitialSize(2);
        dataSource.setMaxTotal(10);
        dataSource.setMaxIdle(5);
        dataSource.setMinIdle(1);
        dataSource.setMaxWaitMillis(15000);

        // Saúde das conexões
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestOnBorrow(true);
        dataSource.setTestWhileIdle(true);
        dataSource.setTimeBetweenEvictionRunsMillis(300_000);

        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public int inserirVoo(Voo v) {
        final String sql = """
        INSERT INTO voo (
          estado, mes, ano, qtdAeroportos, numVoosRegulares, numVoosIrregulares,
          numEmbarques, numDesembarques, numVoosTotais
        ) VALUES (?,?,?,?,?,?,?,?,?)
        """;
        return jdbcTemplate.update(sql,
                semAcento(v.getEstado()),
                semAcento(v.getMes()),
                v.getAno(),
                v.getQtdAeroportos(),
                v.getNumVoosRegulares(), v.getNumVoosIrregulares(),
                v.getNumEmbarques(), v.getNumDesembarques(),
                v.getNumVoosTotais());
    }

    /** Mantido para logs. */
    public void inserirDadosLogs(RegistroLogs registroLogs) {
        final String sql = """
            INSERT INTO registroLogs (categoria, descricao, dtHora)
            VALUES (?,?,?)
            """;
        Timestamp ts = Timestamp.valueOf(registroLogs.getDtHora());
        jdbcTemplate.update(sql, registroLogs.getCategoria(), registroLogs.getDescricao(), ts);
    }

    @Override
    public void close() throws Exception {
        dataSource.close();
    }

}