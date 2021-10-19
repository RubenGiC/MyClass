package org.rubengic.myclass;

public class Asignatura {
    private int id;
    private String nombre, aula, hora;

    public Asignatura(int id, String nombre, String aula, String hora){

        this.nombre = nombre;
        this.id = id;
        this.aula = aula;
        this.hora = hora;
    }

    public int getID() {
        return id;
    }

    public void setID(int ID) {
        this.id = ID;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAula() {
        return aula;
    }

    public void setAula(String aula) {
        this.aula = aula;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }
}
