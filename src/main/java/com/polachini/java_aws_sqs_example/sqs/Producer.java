package com.polachini.java_aws_sqs_example.sqs;

import com.polachini.java_aws_sqs_example.entity.Bill;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Producer {

	@Value("${queues.bills-to-pay}")
	private String queueName;
	
	@Autowired
	private QueueMessagingTemplate queueMessagingTemplate;
	
	public void produce(Bill bill) {
		queueMessagingTemplate.convertAndSend(queueName, bill);
	}
}
