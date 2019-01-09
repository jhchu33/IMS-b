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
        456789
        return 4;
    }

    @PostMapping("/signup")
    public int signup(){

       return 5;
    }

    @GetMapping("/profile")
    public int getUser(){

        return 6;
    }
    
    
    
    
    
    //ppppppp
    //123123123

}
