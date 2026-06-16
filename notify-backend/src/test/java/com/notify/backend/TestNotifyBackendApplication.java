package com.notify.backend;

import org.springframework.boot.SpringApplication;

public class TestNotifyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(NotifyBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
