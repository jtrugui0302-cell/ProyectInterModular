package com.example.proyectoaplicacinturismolocal.Models;

public class Sitio {
    private int id;
    private String nombre, descripcion, urlImagen, tipo;
    private double latitud, longitud;

    public Sitio(String nombre, String descripcion, String urlImagen, String tipo) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.urlImagen = urlImagen;
        this.tipo = tipo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getUrlImagen() { return urlImagen; }
    public String getTipo() { return tipo; }
    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }
    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
}