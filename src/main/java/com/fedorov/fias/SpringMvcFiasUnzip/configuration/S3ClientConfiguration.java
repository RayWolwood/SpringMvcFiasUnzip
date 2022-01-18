package com.fedorov.fias.SpringMvcFiasUnzip.configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class S3ClientConfiguration {

    @Value("${cloud.aws_access_key_id}")
    private String access_key;

    @Value("${cloud.aws_secret_access_key}")
    private String secret_access_key;

    @Value("${cloud.endpiont}")
    private String endpoint;

    @Bean
    public AmazonS3 s3Client() {
//        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(getCredentials()))
                .withClientConfiguration(clientConfiguration())
                .withEndpointConfiguration(endpointConfiguration())
                .withPathStyleAccessEnabled(true)
                .build();
    }

    private AwsClientBuilder.EndpointConfiguration endpointConfiguration(){
        log.info("Используется s3 хранилище: {}", endpoint);
        return new AwsClientBuilder.EndpointConfiguration(endpoint, Regions.US_EAST_1.getName());
    }

    private ClientConfiguration clientConfiguration(){
        return new ClientConfiguration()
                .withConnectionTimeout(10_000_000)
                .withSocketTimeout(10_000_000);
    }

    private BasicAWSCredentials getCredentials() {
        return new BasicAWSCredentials(access_key, secret_access_key);
    }
}
