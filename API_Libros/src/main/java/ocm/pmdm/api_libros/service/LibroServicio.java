package ocm.pmdm.api_libros.service;

import ocm.pmdm.api_libros.model.Libro;
import ocm.pmdm.api_libros.repository.LibroRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class LibroServicio {

    @Autowired
    private LibroRepositorio libroRepositorio;

    public ArrayList<Libro> getLibros() {
        return libroRepositorio.getLibros();
    }

    public Libro getLibrobyId(Long id) {

        return libroRepositorio.getLibroById(id);
    }

    public boolean crearLibro(Libro libro) {
        if (libro.getAutor().length() == 0 || libro.getTitulo().length() == 0) {
            return false;
        }else{
            libroRepositorio.crearLibro(libro);
            return true;
        }
    }

    public boolean actualizarLibro(Long id, Libro libro) {

        if (libroRepositorio.getLibroById(id) == null) {
            return false;
        }

        if (libro.getAutor().length() == 0 || libro.getTitulo().length() == 0) {
            return false;
        }

        return libroRepositorio.actualizarLibro(id,libro);

    }

    public boolean eliminarLibro(Long id) {
        return libroRepositorio.eliminarLibro(id);
    }

    public boolean eliminarLibros() {
        return libroRepositorio.eliminarLibros();
    }


}
