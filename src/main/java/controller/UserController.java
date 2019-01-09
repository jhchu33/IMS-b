package controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping("/signin")
    publc int signin(){

        return 0;
    }

    @PostMapping("/signup")
    public int signup(){

       return 1;
    }

    @GetMapping("/profile")
    public int getUser(){

        return 2;
    }

}
