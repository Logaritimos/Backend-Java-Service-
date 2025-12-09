package school.sptech;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

public class MonitoramentoService {

    private final JdbcTemplate db;
    private final SlackService slackService;
    private final LogService logService;

    private static final int LIMITE_ALTA = 500;
    private static final int LIMITE_BAIXA = 100;

    public MonitoramentoService(Conexao conexao, LogService logService) {
        this.db = conexao.getJdbcTemplate();
        this.logService = logService;
        this.slackService = new SlackService();
    }

    public void checarAlertas() {
        logService.registrar("INFO", "Iniciando verificação de regras de alerta (Slack)...");

        String sqlCanais = """
            SELECT sc.idSlackCanal, sc.canal, sc.paramEstado, sc.paramDemanda, 
                   sc.fkEmpresa, sb.token, emp.razaoSocial
            FROM slackCanal sc
            JOIN slackBot sb ON sc.fkSlackBot = sb.idSlackBot
            JOIN empresa emp ON sc.fkEmpresa = emp.idEmpresa
            WHERE sc.status = 'Ativo'
        """;

        List<Map<String, Object>> canais = db.queryForList(sqlCanais);

        if (canais.isEmpty()) {
            logService.registrar("INFO", "Nenhum canal ativo encontrado para notificação.");
            return;
        }

        for (Map<String, Object> canal : canais) {
            processarCanal(canal);
        }

        logService.registrar("INFO", "Verificação de alertas concluída.");
    }

    private void processarCanal(Map<String, Object> canal) {
        String estado = (String) canal.get("paramEstado");
        String demandaDesejada = (String) canal.get("paramDemanda");
        String token = (String) canal.get("token");
        String nomeCanal = (String) canal.get("canal");
        Integer idCanalDb = (Integer) canal.get("idSlackCanal");
        Integer idEmpresa = (Integer) canal.get("fkEmpresa");
        String nomeEmpresa = (String) canal.get("razaoSocial");

        String sqlVoo = """
            SELECT * FROM voo 
            WHERE estado = ? 
            ORDER BY ano DESC, 
            CASE mes 
                WHEN 'Dezembro' THEN 12 WHEN 'Novembro' THEN 11 WHEN 'Outubro' THEN 10 
                WHEN 'Setembro' THEN 9 WHEN 'Agosto' THEN 8 WHEN 'Julho' THEN 7 
                WHEN 'Junho' THEN 6 WHEN 'Maio' THEN 5 WHEN 'Abril' THEN 4 
                WHEN 'Março' THEN 3 WHEN 'Fevereiro' THEN 2 WHEN 'Janeiro' THEN 1 
            END DESC
            LIMIT 1
        """;

        try {
            List<Map<String, Object>> listaVoos = db.queryForList(sqlVoo, estado);

            if (listaVoos.isEmpty()) {
                return;
            }

            Map<String, Object> voo = listaVoos.get(0);
            Integer totalVoos = (Integer) voo.get("numVoosTotais");
            String mesRef = (String) voo.get("mes");
            Integer anoRef = (Integer) voo.get("ano");

            boolean deveNotificar = false;
            String tipoDetectado = "";

            boolean isAlta = totalVoos > LIMITE_ALTA;
            boolean isBaixa = totalVoos < LIMITE_BAIXA;

            if ("Alta".equalsIgnoreCase(demandaDesejada) && isAlta) {
                deveNotificar = true;
                tipoDetectado = "ALTA";
            }
            else if ("Baixa".equalsIgnoreCase(demandaDesejada) && isBaixa) {
                deveNotificar = true;
                tipoDetectado = "BAIXA";
            }
            else if ("Ambas".equalsIgnoreCase(demandaDesejada)) {
                if (isAlta) {
                    deveNotificar = true;
                    tipoDetectado = "ALTA";
                } else if (isBaixa) {
                    deveNotificar = true;
                    tipoDetectado = "BAIXA";
                }
            }

            if (deveNotificar) {
                String identificadorTempo = "%" + mesRef + "/" + anoRef + "%";

                String sqlAntiSpam = """
                    SELECT count(*) FROM slackNotificacao 
                    WHERE fkSlackCanal = ? 
                    AND mensagem LIKE ?
                """;

                Integer jaEnviou = db.queryForObject(sqlAntiSpam, Integer.class, idCanalDb, identificadorTempo);

                if (jaEnviou != null && jaEnviou == 0) {
                    String msg = String.format(
                            "Olá %s! Alerta de Demanda *%s* detectada em %s.\nReferência: %s/%d\nTotal de Voos: %d",
                            nomeEmpresa, tipoDetectado, estado, mesRef, anoRef, totalVoos
                    );

                    slackService.enviarMensagem(token, nomeCanal, msg);

                    db.update("INSERT INTO slackNotificacao (mensagem, fkSlackCanal, fkEmpresa) VALUES (?, ?, ?)",
                            msg, idCanalDb, idEmpresa);

                    logService.registrar("INFO", "Notificação enviada para " + nomeEmpresa + " (" + nomeCanal + ")");
                }
            }

        } catch (Exception e) {
            logService.registrar("ERROR", "Erro ao processar regra do estado " + estado + ": " + e.getMessage());
        }
    }
    public void notificarInicioMonitoramento() {
        logService.registrar("INFO", "Enviando mensagens de boas-vindas aos canais ativos...");

        String sql = """
            SELECT sc.canal, sc.paramEstado, sc.paramDemanda, sb.token, emp.razaoSocial
            FROM slackCanal sc
            JOIN slackBot sb ON sc.fkSlackBot = sb.idSlackBot
            JOIN empresa emp ON sc.fkEmpresa = emp.idEmpresa
            WHERE sc.status = 'Ativo'
        """;

        try {
            List<Map<String, Object>> canaisAtivos = db.queryForList(sql);

            if (canaisAtivos.isEmpty()) {
                logService.registrar("INFO", "Nenhum canal ativo para notificar inicialização.");
                return;
            }

            for (Map<String, Object> canal : canaisAtivos) {
                String token = (String) canal.get("token");
                String nomeCanal = (String) canal.get("canal");
                String estado = (String) canal.get("paramEstado");
                String demanda = (String) canal.get("paramDemanda");

                String msg = String.format(
                        "*Sistema Iniciado!*\nO monitoramento de *%s* (%s Demanda) está ativo e operante.",
                        estado, demanda
                );

                slackService.enviarMensagem(token, nomeCanal, msg);
                logService.registrar("INFO", "Boas-vindas enviada para " + nomeCanal);

                Thread.sleep(500);
            }

        } catch (Exception e) {
            logService.registrar("ERROR", "Erro ao notificar início: " + e.getMessage());
        }
    }
}