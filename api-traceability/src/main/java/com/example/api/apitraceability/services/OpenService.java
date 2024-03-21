package com.example.api.apitraceability.services;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.api.apitraceability.models.OpenModel;

@Service
public class OpenService {
    
    private OpenModel openModel = new OpenModel();

    public void executeWindow(){
        try {
            openModel.getBuilder().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}