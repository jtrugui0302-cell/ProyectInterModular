package com.example.proyectoaplicacinturismolocal;

// Clase para representar un lugar de la base de datos
public class Sitio {
    private String nombre;
    private String descripcion;
    private String urlImagen;

    public Sitio(String nombre, String descripcion, String urlImagen) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.urlImagen = urlImagen;
    }

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getUrlImagen() { return urlImagen; }
}