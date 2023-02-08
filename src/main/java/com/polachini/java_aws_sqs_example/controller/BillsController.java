package com.polachini.java_aws_sqs_example.controller;

import com.polachini.java_aws_sqs_example.sqs.Producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bills")
public class BillsController {
	
	private static final Logger log = LoggerFactory.getLogger(BillsController.class); 

	@Autowired
	private Producer sqsProducer;
	
	@PostMapping("/pay")
	public void pay(@RequestBody String bill) {
		
		sqsProducer.produce(bill);
		
		log.info("A bill of {} was post to be payed!", bill);
	}
}
