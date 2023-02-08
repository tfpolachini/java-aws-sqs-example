package com.polachini.java_aws_sqs_example.sqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.awspring.cloud.messaging.listener.annotation.SqsListener;

@Component
public class Consumer {

	private static final Logger log = LoggerFactory.getLogger(Consumer.class);

	@SqsListener(value = "${queues.bills-to-pay}")
	public void listen(String message) {
		log.info("A bill of {} was received. I'll pay asap!", message);
	}
}
