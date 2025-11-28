package school.sptech;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.List;

public class S3Reader {

    public InputStream getFileFromS3(String bucketName, String key) {

        S3Client s3 = new S3Provider().getS3Client();
        List<Bucket> buckets = s3.listBuckets().buckets();
        for (Bucket bucketDaVez : buckets) {
            if(bucketDaVez.name().contains(bucketName)){
                bucketName = bucketDaVez.name();
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

                return s3.getObject(getObjectRequest);
            }
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3.getObject(getObjectRequest);
    }
}