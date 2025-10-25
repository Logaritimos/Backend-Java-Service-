package school.sptech;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;

public class Conexao implements AutoCloseable {

    private final BasicDataSource dataSource = new BasicDataSource();
    private final JdbcTemplate jdbcTemplate;

    public Conexao() {
        // IMPORTANTE: sem espaços no nome da variável!
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("ERRO: Variáveis de ambiente (DB_URL, DB_USER, DB_PASSWORD) não configuradas.");
            throw new IllegalStateException("Configuração de banco de dados ausente.");
        }

        // Driver MySQL
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);

        // Pool — ajuste conforme a carga
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

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void inserirDadosVooBatch(List<Voo> voos) {
        final String sql = """
            INSERT INTO voo (
                estado, mes, ano, qtdAeroportos, numVoosRegulares, numVoosIrregulares,
                numEmbarques, numDesembarques, numVoosTotais
            ) VALUES (?,?,?,?,?,?,?,?,?)
            """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                Voo v = voos.get(i);
                ps.setString(1, v.getEstado());
                ps.setString(2, v.getMes());
                ps.setInt(3, v.getAno());
                ps.setInt(4, v.getQtdAeroportos());
                ps.setInt(5, v.getNumVoosRegulares());
                ps.setInt(6, v.getNumVoosIrregulares());
                ps.setInt(7, v.getNumEmbarques());
                ps.setInt(8, v.getNumDesembarques());
                ps.setInt(9, v.getNumVoosTotais());
            }

            @Override
            public int getBatchSize() {
                return voos.size();
            }
        });
    }

    public void inserirDadosLogs(RegistroLogs registroLogs) {
        final String sql = """
            INSERT INTO registroLogs (categoria, descricao, dtHora)
            VALUES (?,?,?)
            """;
        // Converter para Timestamp garante compatibilidade com DATETIME
        Timestamp ts = Timestamp.valueOf(registroLogs.getDtHora());
        jdbcTemplate.update(sql, registroLogs.getCategoria(), registroLogs.getDescricao(), ts);
    }

    @Override
    public void close() throws Exception {
        dataSource.close();
    }
}