package com.example.lupworkmanager;

public abstract class User {

    private static String usuario;
    private String password;

    public static String getUsuario() {
        return usuario;
    }

    public static void setUsuario(String usuarioS) {
        usuario = usuarioS;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
