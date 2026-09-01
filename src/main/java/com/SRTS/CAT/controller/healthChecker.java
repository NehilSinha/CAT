package com.SRTS.CAT.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class healthChecker {
    @GetMapping("/h")
    public boolean health() {
        return true;
    }
    

}
