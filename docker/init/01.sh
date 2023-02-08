#!/bin/bash

awslocal sqs create-queue --queue-name "bills-to-pay" --region "us-east-1"
