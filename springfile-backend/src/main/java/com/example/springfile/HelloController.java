package com.example.springfile;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
public class HelloController {

    @CrossOrigin(origins = "*") // Allow requests from any origin (adjust for production)
    @GetMapping("/api/hello")
    public String sayHello() {
        return "Hello Yan from Spring Boot!";
    }
}
