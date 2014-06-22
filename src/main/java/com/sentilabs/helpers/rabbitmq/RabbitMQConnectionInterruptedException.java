package com.sentilabs.helpers.rabbitmq;

public class RabbitMQConnectionInterruptedException extends Exception {

    public RabbitMQConnectionInterruptedException(String s){
        super(s);
    }
}
