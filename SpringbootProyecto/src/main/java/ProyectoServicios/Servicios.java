package ProyectoServicios;

import ProyectoRepositorio.Repositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

public class Servicios {

    @Autowired private UsuarioRepository userRepo;
    @Autowired private RutaRepository rutaRepo;
    @Autowired private SitioRepository sitioRepo;

    // Usuarios
    public Usuario guardarUsuario(Usuario u) { return userRepo.save(u); }
    public List<Usuario> listarUsuarios() { return userRepo.findAll(); }

    // Rutas
    public List<Ruta> listarRutas() { return rutaRepo.findAll(); }
    public Ruta crearRuta(Ruta r) { return rutaRepo.save(r); }

    // Lógica Especial: Unir Usuario a Ruta (Tabla usuarios_rutas)
    public void inscribirUsuarioEnRuta(Integer usuarioId, Integer rutaId) {
        Usuario u = userRepo.findById(usuarioId).orElseThrow();
        Ruta r = rutaRepo.findById(rutaId).orElseThrow();
        u.getRutasInscritas().add(r);
        userRepo.save(u);
    }
}

// --- SERVICIO DE INTERACCIONES (Comentarios y Valoraciones) ---
@Service
public class InteraccionService {
    @Autowired
    private Repositorio.ComentarioRepository comRepo;
    @Autowired private Repositorio.ValoracionRepository valRepo;

    public Comentario publicarComentario(Comentario c) { return comRepo.save(c); }
    public List<Comentario> obtenerComentariosPorRuta(Integer rutaId) { return comRepo.findByRutaId(rutaId); }

    public Valoracion puntuar(Valoracion v) { return valRepo.save(v); }
}

// --- SERVICIO DE SITIOS ---
@Service
public class SitioService {
    @Autowired private SitioRepository sitioRepo;
    public List<Sitio> listarSitios() { return sitioRepo.findAll(); }
    public Sitio guardarSitio(Sitio s) { return sitioRepo.save(s); }
}

}
