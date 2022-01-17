package com.fedorov.fias.SpringMvcFiasUnzip.configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class S3ClientConfiguration {

    @Bean
    public AmazonS3 s3Client() {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(getCredentials()))
                .withClientConfiguration(clientConfiguration())
                .withEndpointConfiguration(endpointConfiguration())
                .build();
    }

    private AwsClientBuilder.EndpointConfiguration endpointConfiguration(){
        return new AwsClientBuilder.EndpointConfiguration("http://s3.amazonaws.com", Regions.US_EAST_1.getName());
    }

    private ClientConfiguration clientConfiguration(){
        return new ClientConfiguration()
                .withConnectionTimeout(10_000_000)
                .withSocketTimeout(10_000_000);
    }

    private BasicAWSCredentials getCredentials() {
        return new BasicAWSCredentials("", "");
    }
}
