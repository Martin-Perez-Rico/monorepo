package com.example.api.apitraceability.models;

import java.io.File;
import javax.persistence.Entity;

@Entity
public class OpenModel {
    
    ProcessBuilder builder;

    public ProcessBuilder getBuilder() {
        // Define la ruta relativa al ejecutable
        String rutaRelativa = "module\\api-traceability\\src\\main\\resources\\Traceability.exe";
            
        // Obtiene el directorio actual de trabajo
        String directorioActual = System.getProperty("user.dir");
        System.out.println("Directorio actual: " + directorioActual);
        
        // Une la ruta relativa con el directorio actual para obtener la ruta absoluta
        String rutaAbsoluta = directorioActual + File.separator + rutaRelativa;

        this.builder = new ProcessBuilder(rutaAbsoluta);
        return builder;
    }
}
