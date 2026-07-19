package com.demo.producer;

public record KafkaSendResult(
    String messageId,
    String topic,
    int partition,
    long offset
) {
}
