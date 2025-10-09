package school.sptech;

import org.apache.commons.dbcp2.BasicDataSource;

import java.util.Scanner;

public class Logs {
    public static void main(String[] args) {
        Scanner leitor = new Scanner(System.in);
        LogUtils logUtils = new LogUtils();
        RegistroLogs registroLogs = new RegistroLogs();
        boolean repetir = true;

        // Conexão com o BD//
            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setUrl("jdbc:mysql:mem:logaritmos");
            basicDataSource.setUsername("");
            basicDataSource.setPassword("");
        //Conexão com o BD//

        logUtils.exibirCabecalho();
        registroLogs.exibirCabecalhos();

        while (repetir) {
            System.out.print("\nDigite o CNPJ (apenas números): ");
            String cnpj = leitor.nextLine().replaceAll("\\D", "");

            if (cnpj.replaceAll("\\s+", "").isEmpty()) {
                logUtils.simularLogs("Nenhum valor informado. Por favor, digite novamente.", "WARN");
                continue; //continue serve para continuar o código fora do loop
            }

            logUtils.simularLogs("Iniciando verificação do CNPJ...", "INFO");
            logUtils.simularLogs("Consultando base de dados da Receita Federal...", "INFO");
            logUtils.simularLogs("Verificando integridade do número informado...", "INFO");
            logUtils.simularLogs("Executando validação de dígitos verificadores...", "INFO");

            if (logUtils.validarCNPJ(cnpj)) {
                logUtils.simularLogs("CNPJ " + logUtils.formatarCNPJ(cnpj) + " validado com sucesso.", "SUCCESS");
                logUtils.simularLogs("Processo concluído com ÊXITO.", "SUCCESS");
                repetir = false;
            } else {
                logUtils.simularLogs("Erro: CNPJ inválido!", "ERROR");
                System.out.print("\nDeseja digitar novamente? (s/n): ");
                String resp = leitor.nextLine().trim().toLowerCase();
                if (!resp.equals("s")) {
                    repetir = false;
                }
            }
        }

        logUtils.simularLogs("Encerrando sistema de verificação...", "INFO");
        System.out.println("\nObrigado por utilizar o sistema de validação de CNPJs da Logaritmos.");
    }
}
