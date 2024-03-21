package com.example.api.apitraceability.controllers;

import com.example.api.apitraceability.services.OpenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/open")
public class OpenController {

    @Autowired
    private OpenService openService;
    
    @GetMapping("/traceability")
    public void openTraceability(){
        openService.executeWindow();
    }
}