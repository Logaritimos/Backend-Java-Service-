package school.sptech;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3Provider {

    private final AwsCredentialsProvider credentials;

    public S3Provider() {
        this.credentials = DefaultCredentialsProvider.create();
    }

    public S3Client getS3Client() {
        String regionEnv = System.getenv("AWS_REGION");

        // Se não houver variável configurada usa US_EAST_1
        Region region = (regionEnv == null || regionEnv.isBlank())
                ? Region.US_EAST_1
                : Region.of(regionEnv);

        return S3Client.builder()
                .region(region)
                .credentialsProvider(credentials)
                .build();
    }
}