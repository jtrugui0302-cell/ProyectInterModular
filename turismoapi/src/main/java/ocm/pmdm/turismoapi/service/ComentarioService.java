package ocm.pmdm.turismoapi.service;

import ocm.pmdm.turismoapi.model.Comentario;
import ocm.pmdm.turismoapi.repository.ComentarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComentarioService {

    @Autowired
    private ComentarioRepository comentarioRepository;

    public Comentario save(Comentario comentario) {
        return comentarioRepository.save(comentario);
    }

    public Comentario findById(Long id) {
        return comentarioRepository.findById(id).orElse(null);
    }
    public List<Comentario> findAll() {
        return comentarioRepository.findAll();
    }

    public void deleteById(Long id) {
        comentarioRepository.deleteById(id);
    }

    public void deleteAll() {
        comentarioRepository.deleteAll();
    }

}
