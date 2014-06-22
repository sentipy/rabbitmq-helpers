package com.sentilabs.helpers.rabbitmq;

public class RabbitMQMessageData {

    private byte[] bytes;
    private long deliveryTag;

    public byte[] getBytes() {
        return this.bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public long getDeliveryTag() {
        return this.deliveryTag;
    }

    public void setDeliveryTag(long deliveryTag) {
        this.deliveryTag = deliveryTag;
    }
}
