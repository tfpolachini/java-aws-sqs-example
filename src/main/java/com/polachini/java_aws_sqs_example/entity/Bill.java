package com.polachini.java_aws_sqs_example.entity;

import java.math.BigDecimal;

public class Bill {

    private String id;
    private String payee;
    private BigDecimal value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPayee() {
        return payee;
    }

    public void setPayee(String payee) {
        this.payee = payee;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Bill{" +
                "id='" + id + '\'' +
                ", payee='" + payee + '\'' +
                ", value=" + value +
                '}';
    }
}
