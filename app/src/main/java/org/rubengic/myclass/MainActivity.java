package org.rubengic.myclass;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.Callable;

public class MainActivity extends AppCompatActivity {

    private BaseDatos db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new BaseDatos();

        Connection conexion = db.conexionDB();

        /*String p = null;
        try {
            //p = db.mostrarAlumnos(db.getConexion());
            Statement st = conexion.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM alumno");
            rs.next();
            System.out.println( rs.getString(1) );

            p=rs.getString(1);

            Toast.makeText(this, "--> "+p, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            System.out.println("ERROR");
            e.printStackTrace();
        }

        TextView tv = (TextView) findViewById(R.id.tv_p);
        tv.setText(p);*/

        //cierre de la conexión con la base de datos
        try {
            db.cerrarConexion(db.getConexion());
        } catch (Exception e) {
            System.out.println("Error NO SE HA PODIDO CERRAR LA CONEXIÓN: ");
            e.printStackTrace();
        }
    }
}