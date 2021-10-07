package org.rubengic.myclass;

public class Profesor {
    private int ID ;
    private String nombre ;
    private String apellidos ;

    public Profesor(String n , String a){
        nombre = n ;
        apellidos = a;
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
}
