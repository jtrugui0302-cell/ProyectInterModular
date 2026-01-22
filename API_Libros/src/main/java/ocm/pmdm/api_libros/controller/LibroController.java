package ocm.pmdm.api_libros.controller;


import ocm.pmdm.api_libros.model.Libro;
import ocm.pmdm.api_libros.service.LibroServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/libros")
public class LibroController {

    @Autowired
    private LibroServicio libroServicio;

    @GetMapping
    public ArrayList<Libro> getLibros() {


        return libroServicio.getLibros();
    }

    @GetMapping("/{id}")
    public ResponseEntity getLibrosById(@PathVariable Long id) {

        Libro libro =  libroServicio.getLibrobyId(id);

        if(libro == null){

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El libro no existe");

        }else{
            return ResponseEntity.ok(libro);
        }

    }

    @PostMapping()
    public ResponseEntity crearLibro(@RequestBody Libro libro) {
        boolean b = libroServicio.crearLibro(libro);
        if(b){
            return ResponseEntity.status(HttpStatus.CREATED).body(libro);
        }else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se ha añadido el libro");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity actualizar(@PathVariable Long id, @RequestBody Libro libro) {
        boolean b = libroServicio.actualizarLibro(id,libro);
        if(b){
            libro.setId(id);
            return ResponseEntity.status(HttpStatus.OK).body(libro);
        }else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al actualizar el libro");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity eliminarLibro(@PathVariable Long id) {
        boolean b = libroServicio.eliminarLibro(id);

        if(b){
            return ResponseEntity.status(HttpStatus.OK).body("Libro eliminado");
        }else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al eliminar el libro");
        }
    }


    @DeleteMapping
    public ResponseEntity eliminarLibros(@RequestBody Libro libro) {
        boolean b = libroServicio.eliminarLibros();

        if(b){
            return ResponseEntity.status(HttpStatus.OK).body("Todos los libros se han eliminado");
        }else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al eliminar todos libro");
        }

    }


}
