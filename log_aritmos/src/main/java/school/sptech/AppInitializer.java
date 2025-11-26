package school.sptech;

import java.io.InputStream;

public class AppInitializer {

    public static void main(String[] args) {

        String bucketName = "amzn-s3-bucket-logaritmos";
        String fileName = "dicionario_de_dadosV2.0.xlsx";

        if (bucketName == null || fileName == null) {
            System.err.println("ERRO: Variáveis AWS_BUCKET_NAME e AWS_FILE_KEY são obrigatórias.");
            return;
        }

        try (Conexao conexao = new Conexao()) {
            LogService logService = new LogService(conexao);

            try {
                logService.registrar("INFO", "Sistema de processamento iniciado. Inicializando serviços...");
                logService.registrar("INFO", "Iniciando download do S3:" + bucketName + "/" + fileName);

                S3Reader s3Reader = new S3Reader();

                InputStream s3Stream = s3Reader.getFileFromS3(bucketName, fileName);

                logService.registrar("INFO", "Arquivo baixado (stream aberto). Iniciando processamento...");

                LeitorArquivo leitor = new LeitorExcell();
                leitor.processar(s3Stream, conexao, logService);

                logService.registrar("INFO", "Fluxo principal concluído com sucesso.");
            } catch (Exception e) {
                logService.registrar("CRITICAL", "Falha na execução principal: " + e.getMessage());
                e.printStackTrace();
            } finally {
                logService.registrar("INFO", "Encerrando serviço.");
            }
        } catch (Exception e) {
            System.err.println("Falha ao iniciar/encerrar recursos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
