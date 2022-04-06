package com.example.lupworkmanager;

import java.util.UUID;

public class RegistroLecturas {

    private String id;
    private String hora;
    private String texto;
    private double tiempoEjecucion;


    public RegistroLecturas(String hora,
                  String texto, double tiempoEjecucion) {
        this.id = UUID.randomUUID().toString();
        this.hora = hora;
        this.texto = texto;
        this.tiempoEjecucion = tiempoEjecucion;
    }

    public String getId() {
        return id;
    }

    public String getTexto() {
        return texto;
    }

    public String getHora() {
        return hora;
    }

    public double getTiempoEjecucionr() {
        return tiempoEjecucion;
    }


}
