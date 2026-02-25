package com.example.proyectoaplicacinturismolocal.Models;

public class Comentarios {
    private int id; // Campo 'id' de la tabla 'comentarios'
    private String autor, texto, fecha;

    public Comentarios(int id, String autor, String texto, String fecha) {
        this.id = id;
        this.autor = autor;
        this.texto = texto;
        this.fecha = fecha;
    }

    public int getId() { return id; }
    public String getAutor() { return autor; }
    public String getTexto() { return texto; }
    public String getFecha() { return fecha; }
}