package ocm.pmdm.api_libros.repository;

import ocm.pmdm.api_libros.model.Libro;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public class LibroRepositorio {

    ArrayList<Libro> libros = new ArrayList<Libro>();

    public LibroRepositorio(){
        Libro l1 = new Libro(1L,"La odisea","Homero");
        Libro l2 = new Libro(2L,"El hobbit","J.R.R Tolkien");
        libros.add(l1);
        libros.add(l2);
    }

    public ArrayList<Libro> getLibros(){
        return libros;
    }

    public Libro getLibroById(Long id) {
        for(Libro l : libros){
            if (l.getId().equals(id)) {
                return l;
            }
        }

        return null;
    }

    public void crearLibro(Libro libro){
        libros.add(libro);
    }

    public boolean actualizarLibro(Long id,Libro libro){
        Libro l = getLibroById(id);

        if(l!=null){
            l.setTitulo(libro.getTitulo());
            l.setAutor(libro.getAutor());
            return true;
        }
        return false;
    }

    public boolean eliminarLibro(Long id){
        Libro l = getLibroById(id);
        if(l!=null){
            libros.remove(l);
            return true;
        }
        return false;
    }

    public boolean eliminarLibros(){

        if(libros.size()>0){
            libros.remove(0);
            return true;
        }
        return false;
    }

}
