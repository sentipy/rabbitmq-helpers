package com.sentilabs.helpers.rabbitmq;

public class RabbitMQConnectionShutdownSignalException extends Exception {

    public RabbitMQConnectionShutdownSignalException(String s){
        super(s);
    }
}
