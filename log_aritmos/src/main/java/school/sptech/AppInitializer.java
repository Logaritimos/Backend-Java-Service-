package school.sptech;

import java.io.InputStream;
import java.nio.file.Path;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class AppInitializer {

    public static void main(String[] args) {

        String s3Bucket = System.getenv("S3_BUCKET");
        String s3Key = System.getenv("S3_KEY");

        if (s3Bucket == null || s3Bucket.isBlank() || s3Key == null || s3Key.isBlank()) {
            System.err.println("As variáveis de ambiente S3_BUCKET e S3_KEY devem ser definidas");
            return;
        }

        try (Conexao conexao = new Conexao()) {
            LogService logService = new LogService(conexao);

            try {
                logService.registrar("INFO", "Sistema de processamento iniciado. Inicializando serviços...");

                // Create S3 client
                S3Client s3 = S3Client.builder()
                        .region(Region.of(System.getenv("AWS_REGION")))
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .build();

                // Get the Excel file from S3 as InputStream
                try (InputStream excelStream = s3.getObject(
                        GetObjectRequest.builder()
                                .bucket(s3Bucket)
                                .key(s3Key)
                                .build()
                )) {
                    // Process the Excel file from the InputStream
                    LeitorArquivo leitor = new LeitorExcell();
                    leitor.processar((Path) excelStream, conexao, logService); // assumes processar supports InputStream
                }

                logService.registrar("INFO", "Fluxo principal concluído com sucesso.");
                s3.close();

            } catch (Exception e) {
                logService.registrar("CRITICAL", "Falha na execução principal: " + e.getMessage());
                e.printStackTrace();
            } finally {
                logService.registrar("INFO", "Encerrando serviço de log.");
            }

        } catch (Exception e) {
            System.err.println("Falha ao iniciar/encerrar recursos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
