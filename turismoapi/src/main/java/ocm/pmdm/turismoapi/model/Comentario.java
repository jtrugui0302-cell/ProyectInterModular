package ocm.pmdm.turismoapi.model;

import jakarta.persistence.*;

import java.util.Date;

@Entity
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String contenido;

    @Column(nullable = false)
    private String usuario;

    @Column(nullable = false)
    private String fecha;

    public Comentario() {

    }

    public Comentario(Long id, String contenido, String usuario, String fecha) {
        this.id = id;
        this.contenido = contenido;
        this.usuario = usuario;
        this.fecha = fecha;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}