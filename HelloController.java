package com.example.demo;

import org.springframework.web.bind.bind.annotation.GetMapping;
import org.springframework.web.bind.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String index() {
        return "Hello World! Version 1.0"; 
    }
}
