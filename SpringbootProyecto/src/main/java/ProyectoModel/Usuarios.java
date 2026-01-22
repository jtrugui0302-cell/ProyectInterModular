package ProyectoModel;

import jakarta.persistence.*;
import org.springframework.data.annotation.Id;

public class Usuarios {

    @Entity
    @Table(name = "usuarios")
    public class Usuario {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        private String nombre;
        private String apellido;
        private int edad;

        @Column(unique = true, nullable = false)
        private String gmail;

        @Column(name = "numero_telefono")
        private String numeroTelefono;

        private int tipo; // 0=normal, 1=guía, 2=admin

        @Column(name = "password_hash")
        private String passwordHash;

        // Relación N:N con Rutas (Inscripciones)
        @ManyToMany
        @JoinTable(
                name = "usuarios_rutas",
                joinColumns = @JoinColumn(name = "usuario_id"),
                inverseJoinColumns = @JoinColumn(name = "ruta_id")
        )
        private List<Ruta> rutasInscritas;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getApellido() {
            return apellido;
        }

        public void setApellido(String apellido) {
            this.apellido = apellido;
        }

        public int getEdad() {
            return edad;
        }

        public void setEdad(int edad) {
            this.edad = edad;
        }

        public String getGmail() {
            return gmail;
        }

        public void setGmail(String gmail) {
            this.gmail = gmail;
        }

        public String getNumeroTelefono() {
            return numeroTelefono;
        }

        public void setNumeroTelefono(String numeroTelefono) {
            this.numeroTelefono = numeroTelefono;
        }

        public int getTipo() {
            return tipo;
        }

        public void setTipo(int tipo) {
            this.tipo = tipo;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        public List<Ruta> getRutasInscritas() {
            return rutasInscritas;
        }

        public void setRutasInscritas(List<Ruta> rutasInscritas) {
            this.rutasInscritas = rutasInscritas;
        }
    }

}
