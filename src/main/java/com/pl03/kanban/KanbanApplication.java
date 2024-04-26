package com.pl03.kanban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@GetMapping("/v1/task")
@SpringBootApplication
@CrossOrigin (origins = "http://localhost:5173")
public class KanbanApplication {
	public static void main(String[] args) {
		SpringApplication.run(KanbanApplication.class, args);
	}

}
