package com.polachini.java_aws_sqs_example.sqs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;

@Component
public class Producer {

	@Value("${queues.bills-to-pay}")
	private String queueName;
	
	@Autowired
	private QueueMessagingTemplate queueMessagingTemplate;
	
	public void produce(String message) {
		queueMessagingTemplate.send(queueName, createMessage(message));
	}
	
	private Message<String> createMessage(String payload) {
		return MessageBuilder.withPayload(payload).build();
	}
}
