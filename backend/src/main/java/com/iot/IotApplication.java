package com.iot;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IotApplication {
    public static void main(String[] args) {
        SpringApplication.run(IotApplication.class, args);
    }

    // Thời gian lưu và hiển thị theo múi giờ Việt Nam (UTC+7)
    @PostConstruct
    void timezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
}
