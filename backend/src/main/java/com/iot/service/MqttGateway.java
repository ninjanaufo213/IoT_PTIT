package com.iot.service;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Kết nối tới MQTT broker: publish lệnh, nhận bản tin theo topic đã đăng ký. */
@Component
public class MqttGateway implements MqttCallbackExtended {
    private static final Logger log = LoggerFactory.getLogger(MqttGateway.class);

    private final MqttClient client;
    private final MqttConnectOptions options = new MqttConnectOptions();
    private final Map<String, Consumer<String>> handlers = new ConcurrentHashMap<>();

    public MqttGateway(@Value("${mqtt.broker-url}") String url,
                       @Value("${mqtt.username}") String username,
                       @Value("${mqtt.password}") String password) throws MqttException {
        client = new MqttClient(url, "iot-backend-" + System.nanoTime(), new MemoryPersistence());
        client.setCallback(this);
        options.setCleanSession(true);
        options.setConnectionTimeout(5);
        if (!username.isEmpty()) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
        }
    }

    public void subscribe(String topic, Consumer<String> handler) {
        handlers.put(topic, handler);
        if (client.isConnected()) subscribeTopic(topic);
    }

    public void publish(String topic, String payload) throws MqttException {
        if (!client.isConnected()) throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        client.publish(topic, new MqttMessage(payload.getBytes(StandardCharsets.UTF_8)));
    }

    /** Tự kết nối lại mỗi 5 giây nếu broker chưa sẵn sàng hoặc bị ngắt. */
    @Scheduled(initialDelay = 0, fixedDelay = 5000)
    void ensureConnected() {
        if (client.isConnected()) return;
        try {
            client.connect(options);
        } catch (MqttException e) {
            log.warn("Chưa kết nối được MQTT broker: {}", e.getMessage());
        }
    }

    private void subscribeTopic(String topic) {
        try {
            client.subscribe(topic, 0);
            log.info("Đã subscribe {}", topic);
        } catch (MqttException e) {
            log.error("Subscribe {} thất bại", topic, e);
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("Đã kết nối MQTT broker {}", serverURI);
        handlers.keySet().forEach(this::subscribeTopic);
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Mất kết nối MQTT: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        Consumer<String> handler = handlers.get(topic);
        if (handler == null) return;
        try {
            handler.accept(new String(message.getPayload(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Lỗi xử lý bản tin {}", topic, e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

    @PreDestroy
    void close() throws MqttException {
        if (client.isConnected()) client.disconnect();
        client.close();
    }
}
