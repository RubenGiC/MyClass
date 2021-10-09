package org.rubengic.myclass;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*
    iNFO: https://www.programacion.com.py/moviles/android/utilizar-postgresql-en-android-con-jdbc
 */

public class BaseDatos {

    private Connection conexion=null;
    private Statement st;
    BaseDatos(){}
    //conexion a la base de datos
    public Connection conexionDB(){
        try {
            Class.forName("org.postgresql.Driver");
            //CONEXION BASE DE DATOS <base de datos del servidor>, <usuario>, <contraseña>
            conexion = DriverManager.getConnection("jdbc:postgresql://localhost:5432/myclass", "postgres", "root");

            System.out.println("Conexion EXITOSA");
        }catch (SQLException se){
            System.out.println("Error NO SE HA PODIDO CONECTAR: "+se.toString());
        }catch (ClassNotFoundException e){
            System.out.println("Error NO SE HA ENCONTRADO LA CLASE: "+e.getMessage());
        }
        return conexion;
    }

    String mostrarAlumnos(Connection conexion)throws Exception{
        //try {
            //inicializo las declaraciones
            st = conexion.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM alumno");
            rs.next();
            return rs.getString(1);
        /*}catch(SQLException se){
            System.out.println("Error ACCESO TABLA ALUMNO: "+se.toString());
        }
        return "-1";*/
    }
    //cierre de la conexión con la base de datos
    protected void cerrarConexion(Connection conexion)throws Exception{
        conexion.close();
    }

    public Connection getConexion() {
        return conexion;
    }
}
