package ProyectoControlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public class Controladores {

    // CONTROLADOR USUARIOS
    @RestController
    @RequestMapping("/api/usuarios")
    public class UsuarioController {
        @Autowired
        private TurisService service;

        @GetMapping
        @pList<Usuario> getAll() { return service.listarUsuarios(); }
        @PostMapping
        @pUsuario create(@RequestBody Usuario u) { return service.guardarUsuario(u); }

        @PostMapping("/{uId}/unirse/{rId}")
        public ResponseEntity<?> unirseARuta(@PathVariable Integer uId, @PathVariable Integer rId) {
            service.inscribirUsuarioEnRuta(uId, rId);
            return ResponseEntity.ok().build();
        }
    }

    // CONTROLADOR RUTAS
    @RestController
    @RequestMapping("/api/rutas")
    public class RutaController {
        @Autowired private TurisService service;

        @GetMapping public List<Ruta> getAll() { return service.listarRutas(); }
        @PostMapping public Ruta create(@RequestBody Ruta r) { return service.crearRuta(r); }
    }

    // CONTROLADOR SITIOS
    @RestController
    @RequestMapping("/api/sitios")
    public class SitioController {
        @Autowired private SitioService service;

        @GetMapping public List<Sitio> getAll() { return service.listarSitios(); }
        @PostMapping public Sitio create(@RequestBody Sitio s) { return service.guardarSitio(s); }
    }

    // CONTROLADOR FEEDBACK (Comentarios y Valoraciones)
    @RestController
    @RequestMapping("/api/feedback")
    public class FeedbackController {
        @Autowired private InteraccionService service;

        @PostMapping("/comentario") public Comentario addComentario(@RequestBody Comentario c) { return service.publicarComentario(c); }
        @GetMapping("/comentario/ruta/{id}") public List<Comentario> getByRuta(@PathVariable Integer id) { return service.obtenerComentariosPorRuta(id); }
        @PostMapping("/valorar") public Valoracion addValoracion(@RequestBody Valoracion v) { return service.puntuar(v); }
    }
}
