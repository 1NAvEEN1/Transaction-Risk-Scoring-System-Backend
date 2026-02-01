package com.app.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class SystemApplication {

    @PostConstruct
    public void init() {
        // Set default timezone to Sri Lanka (UTC+5:30)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Colombo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }

}
