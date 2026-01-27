package ocm.pmdm.turismoapi.controller;

import ocm.pmdm.turismoapi.model.Comentario;
import ocm.pmdm.turismoapi.service.ComentarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comentarios")
public class ComentarioController {

    @Autowired
    private ComentarioService comentarioService;

    @GetMapping
    public List<Comentario> findAll(){
        return comentarioService.findAll();
    }

    @GetMapping("/{id}")
    public Comentario findById(Long id){
        return comentarioService.findById(id);
    }

    @PostMapping
    public Comentario save(@RequestBody Comentario comentario){
        return comentarioService.save(comentario);
    }

    @PutMapping("/{id}")
    public Comentario update(@PathVariable Long id, @RequestBody Comentario comentario){
        comentario.setId(id);
        return comentarioService.save(comentario);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        comentarioService.deleteById(id);
    }

    @DeleteMapping
    public void deleteAll(){
        comentarioService.deleteAll();
    }

}
