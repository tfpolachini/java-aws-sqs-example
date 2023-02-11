package com.polachini.java_aws_sqs_example.configs;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.core.env.ResourceIdResolver;
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.config.SimpleMessageListenerContainerFactory;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import java.util.List;

@Configuration
public class SqsConfig {

	@Bean
	QueueMessagingTemplate getQueueMessagingTemplate(AmazonSQSAsync amazonSQSAsync) {
		return new QueueMessagingTemplate(amazonSQSAsync, getResourceIdResolver(amazonSQSAsync));
	}

	/**
	 * Bean de configuração do consumer. Está sendo inicializado aqui de forma customizada.
	 * @param amazonSQSAsync
	 * @return
	 */
	@Bean
	SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory(AmazonSQSAsync amazonSQSAsync) {
		SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
		factory.setAmazonSqs(amazonSQSAsync);
		factory.setAutoStartup(true);
		factory.setMaxNumberOfMessages(1);
		factory.setResourceIdResolver(getResourceIdResolver(amazonSQSAsync));

		return factory;
	}

	/**
	 * Configura um conversor de mensagens, o Jackson
	 * @param amazonSQSAsync
	 * @return
	 */
	@Bean
	QueueMessageHandlerFactory getQueueMessageHandlerFactory(AmazonSQSAsync amazonSQSAsync) {
		var queueMessageHandlerFactory = new QueueMessageHandlerFactory();

		queueMessageHandlerFactory.setAmazonSqs(amazonSQSAsync);
		queueMessageHandlerFactory.setMessageConverters(List.of(new MappingJackson2MessageConverter()));

		return queueMessageHandlerFactory;
	}

	/**
	 * Resolve o nome lógico da fila SQS. Ou seja, dado o nome da fila, retorna sua URL.
	 * @param amazonSQSAsync
	 * @return
	 */
	private ResourceIdResolver getResourceIdResolver(AmazonSQSAsync amazonSQSAsync) {
		return new ResourceIdResolver() {
			@Override
			public String resolveToPhysicalResourceId(String logicalResourceId) {
				return amazonSQSAsync.getQueueUrl(logicalResourceId).getQueueUrl();
			}
		};
	}
}
