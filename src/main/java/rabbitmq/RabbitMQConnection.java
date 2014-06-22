package com.sentilabs.helpers.rabbitmq;

import com.rabbitmq.client.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.util.Properties;

public class RabbitMQConnection {

    private Properties rabbitmqProperties = new Properties();
    private ConnectionFactory connectionFactory;
    private Channel rmqChannel;
    private Connection rmqConnection;
    private QueueingConsumer consumer;

    public RabbitMQConnection(String rabbitmqFileParams) throws Exception{
        this.init(rabbitmqFileParams);
    }

    public void init(String rabbitmqFileParams) throws Exception{
        if (rabbitmqFileParams == null){
            throw new RabbitMQConnectionInitializationException("No file with parameters specified");
        }

        FileInputStream fisRmqParams; // input streams with params for RabbitMQ
        try {
            fisRmqParams = new FileInputStream(rabbitmqFileParams);
        } catch (FileNotFoundException e) {
            throw new RabbitMQConnectionInitializationException("Unable to find specified file with parameters ("
                    + rabbitmqFileParams + ")");
        }

        this.init(fisRmqParams);
    }

    public void init(InputStream is) throws Exception{
        this.rabbitmqProperties.load(is);

        if (!this.rabbitmqProperties.containsKey("queueName")){
            throw new RabbitMQConnectionInitializationException("Queue name not specified in the supplied file");
        }

        SSLContext sslContext = null;

        if (this.rabbitmqProperties.getProperty("SSL", "0").equals("1") ){
            if (!this.rabbitmqProperties.containsKey("clientKeyCert")){
                throw new RabbitMQConnectionInitializationException("With ssl enabled you must specify property clientKeyCert" +
                        " which is the path to the client certificate in p12 format");
            }
            if (!this.rabbitmqProperties.containsKey("trustKeystore")){
                throw new RabbitMQConnectionInitializationException("With ssl enabled you must specify property trustKeystore" +
                        " which is the path to the trust store");
            }

            char[] keyPassphrase = this.rabbitmqProperties.getProperty("clientKeyPassphrase", "").toCharArray();

            KeyStore ks;
            ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(this.rabbitmqProperties.getProperty("clientKeyCert")), keyPassphrase);

            KeyManagerFactory kmf;
            kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keyPassphrase);
            char[] trustPassphrase = this.rabbitmqProperties.getProperty("trustKeystorePassphrase").toCharArray();

            KeyStore tks;
            tks = KeyStore.getInstance("JKS");
            tks.load(new FileInputStream(this.rabbitmqProperties.getProperty("trustKeystore")), trustPassphrase);

            TrustManagerFactory tmf;
            tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(tks);

            sslContext = SSLContext.getInstance("SSLv3");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        }

        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(this.rabbitmqProperties.getProperty("host", "localhost"));
        this.connectionFactory.setPort(Integer.valueOf(this.rabbitmqProperties.getProperty("port", "5672")));
        this.connectionFactory.setUsername(this.rabbitmqProperties.getProperty("username", "guest"));
        this.connectionFactory.setPassword(this.rabbitmqProperties.getProperty("password", "guest"));
        this.connectionFactory.setVirtualHost(this.rabbitmqProperties.getProperty("vhost", "/"));

        if (sslContext != null){
            this.connectionFactory.useSslProtocol(sslContext);
        }
    }

    public void connect(boolean autoAck) throws Exception{
        this.rmqConnection = connectionFactory.newConnection();
        this.rmqChannel = this.rmqConnection.createChannel();
        this.consumer = new QueueingConsumer(this.rmqChannel);
        this.rmqChannel.basicConsume(this.rabbitmqProperties.getProperty("queueName"), autoAck, this.consumer);
    }

    public void close() throws RabbitMQConnectionException{
        StringBuilder sb = new StringBuilder();
        String delim = "";
        try {
            this.rmqChannel.close();
        } catch (IOException e) {
            sb.append("There was an error while trying to close the channel");
            delim = System.lineSeparator();
        }
        try {
            this.rmqConnection.close();
        } catch (IOException e) {
            sb.append(delim).append("There was an error while trying to close connection");
        }
        if (sb.length() > 0){
            throw new RabbitMQConnectionException(sb.toString());
        }
    }

    public String getHost(){
        return this.connectionFactory.getHost();
    }

    public int getPort(){
        return this.connectionFactory.getPort();
    }

    public String getUsername(){
        return this.connectionFactory.getUsername();
    }

    public String getVirtualHost(){
        return this.connectionFactory.getVirtualHost();
    }

    public void nextData(RabbitMQMessageData data) throws InterruptedException {
        QueueingConsumer.Delivery delivery;
        delivery = this.consumer.nextDelivery();
        data.setBytes(delivery.getBody());
        data.setDeliveryTag(delivery.getEnvelope().getDeliveryTag());
    }

    public void basicAck(RabbitMQMessageData data) throws IOException {
        this.rmqChannel.basicAck(data.getDeliveryTag(), false);
    }
}
