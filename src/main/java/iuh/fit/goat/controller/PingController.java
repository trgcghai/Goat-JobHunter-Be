package iuh.fit.goat.controller;

import iuh.fit.goat.exception.InvalidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ping")
public class PingController {

    @GetMapping()
    public String Ping() {
        return "Hello World";
    }

}
