package com.example.proyectoaplicacinturismolocal.Models;

    public class Comentarios {
    private String autor;
    private String texto;
    private String fecha;

    public Comentarios(String autor, String texto, String fecha) {
        this.autor = autor; this.texto = texto; this.fecha = fecha;
    }
    public String getAutor() { return autor; }
    public String getTexto() { return texto; }
    public String getFecha() { return fecha; }
}