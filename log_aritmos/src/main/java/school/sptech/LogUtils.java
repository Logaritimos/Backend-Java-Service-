package school.sptech;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogUtils {

    void simularLogs(String mensagem, String tipo) {
        try {
            String cor;

            if ("ERROR".equals(tipo)) {
                cor = "\u001B[31m"; // vermelho
            } else if ("WARN".equals(tipo)) {
                cor = "\u001B[33m"; // amarelo
            } else if ("SUCCESS".equals(tipo)) {
                cor = "\u001B[32m"; // verde
            } else {
                cor = "\u001B[36m"; // ciano
            }

            LocalDateTime agora = LocalDateTime.now();
            String data = agora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            System.out.println(cor + "LOG [" + data + "] [" + tipo + "] " + mensagem + "\u001B[0m");


            Thread.sleep(500);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    String formatarCNPJ(String cnpj) {
        return cnpj.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})",
                "$1.$2.$3/$4-$5");
    }

     boolean validarCNPJ(String cnpj) {
        if (cnpj.length() != 14 || cnpj.matches("(\\d)\\1{13}")) return false;

        int[] peso1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] peso2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        try {
            int digito1 = calcularDigito(cnpj.substring(0, 12), peso1);
            int digito2 = calcularDigito(cnpj.substring(0, 12) + digito1, peso2);
            return cnpj.equals(cnpj.substring(0, 12) + digito1 + digito2);
        } catch (Exception e) {
            return false;
        }
    }

    int calcularDigito(String str, int[] peso) {
        int soma = 0;
        for (int i = 0; i < str.length(); i++) {
            soma += Character.getNumericValue(str.charAt(i)) * peso[i];
        }
        int resto = soma % 11;
        return (resto < 2) ? 0 : 11 - resto;
    }

    // Exibe cabeçalho inicial
    void exibirCabecalho() {
        System.out.println("██╗      ██████╗  ██████╗  █████╗ ██████╗ ██╗████████╗███╗   ███╗ ██████╗ ███████╗");
        System.out.println("██║     ██╔═══██╗██╔════╝ ██╔══██╗██╔══██╗██║╚══██╔══╝████╗ ████║██╔═══██╗██╔════╝");
        System.out.println("██║     ██║   ██║██║  ███╗███████║██████╔╝██║   ██║   ██╔████╔██║██║   ██║███████╗");
        System.out.println("██║     ██║   ██║██║   ██║██╔══██║██╔██╔╝ ██║   ██║   ██║╚██╔╝██║██║   ██║╚════██║");
        System.out.println("███████╗╚██████╔╝╚██████╔╝██║  ██║██║╚██╗ ██║   ██║   ██║ ╚═╝ ██║╚██████╔╝███████║");
        System.out.println("╚══════╝ ╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝ ╚═╝ ╚═╝   ╚═╝   ╚═╝     ╚═╝ ╚═════╝ ╚══════╝");
    }
}
