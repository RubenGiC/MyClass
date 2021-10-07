package org.rubengic.myclass;

public class Alumnos {
    private int ID;
    private String nombre;
    private String apellidos;
    private int curso ;

    public Alumnos(String n, String a, int c){
        nombre = n;
        apellidos = a;
        curso = c;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public int getCurso() {
        return curso;
    }

    public void setCurso(int curso) {
        this.curso = curso;
    }
}
