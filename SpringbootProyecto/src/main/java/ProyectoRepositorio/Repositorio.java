package ProyectoRepositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public class Repositorio implements JpaRepository {

    // Repositorios
    @Repository
    public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
        Optional<Usuario> findByGmail(String gmail);
    }

    @Repository public interface RutaRepository extends JpaRepository<Ruta, Integer> {}

    @Repository public interface ComentarioRepository extends JpaRepository<Comentario, Integer> {
        List<Comentario> findByRutaId(Integer rutaId);
    }

    @Repository public interface SitioRepository extends JpaRepository<Sitio, Integer> {}

    @Repository public interface ValoracionRepository extends JpaRepository<Valoracion, Integer> {}

}
