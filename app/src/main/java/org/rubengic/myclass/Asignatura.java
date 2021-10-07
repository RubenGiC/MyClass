package org.rubengic.myclass;

public class Asignatura {
    private int ID ;
    private String nombre;

    public Asignatura(String n){
        nombre = n;
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
}
