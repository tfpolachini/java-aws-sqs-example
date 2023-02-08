package com.polachini.java_aws_sqs_example.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;

@Configuration
public class LocalStackConfig {
	
	@Value("${localstack.serviceUrl}")
	private String serviceEndpoint;
	
	@Value("${cloud.aws.region.static}")
	private String region;

    @Primary
    @Bean
    AmazonSQSAsync getAmazonSqsAsync() {
        return AmazonSQSAsyncClientBuilder.standard()
        		.withCredentials(new ProfileCredentialsProvider("localstack"))
                .withEndpointConfiguration(new EndpointConfiguration(this.serviceEndpoint, this.region))
                .build();
    }
}
