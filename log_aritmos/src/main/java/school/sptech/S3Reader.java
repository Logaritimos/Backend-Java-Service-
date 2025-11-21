package school.sptech;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;

public class S3Reader {

    public InputStream getFileFromS3(String bucketName, String key) {

        S3Client s3 = new S3Provider().getS3Client();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3.getObject(getObjectRequest);
    }
}