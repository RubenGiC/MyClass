package org.rubengic.myclass;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class MainActivity extends AppCompatActivity {

    private EditText ed_usuario, ed_password;
    private Button b_login;
    TextView tv_error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //change toolbar by my personalizate
        /*Toolbar my_toolbar = findViewById(R.id.custom_toolbar_login);
        setSupportActionBar(my_toolbar);
        getSupportActionBar().setTitle("My Class");*/

        //acceso a los componentes graficos
        ed_usuario = (EditText) findViewById(R.id.ed_usuario);
        ed_password = (EditText) findViewById(R.id.ed_password);
        b_login = (Button) findViewById(R.id.b_login);
        tv_error = (TextView) findViewById(R.id.tv_errors);

        //acción cuando pulsa el boton iniciar sesión
        b_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //valido usuario y contraseña
                //validarUsuario("http://192.168.1.42:8080/validar_login.php");
                validarUsuario("http://192.168.104.2:8080/validar_login.php");
            }
        });
    }

    //para validar el usuario y contraseña
    private void validarUsuario(String URL){
        //genero el request de tipo POST
        StringRequest sr = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            /**
             * metodo para recibir la respuesta del servidor PHP
             * @param response (recibe el mensaje del servidor)
             */
            @Override
            public void onResponse(String response) {
                //si no esta vacia, el usuario y contraseña son correctos y va a la pagina principal
                if(!response.isEmpty()){
                    Intent intent = new Intent(getApplicationContext(),PrincipalActivity.class);
                    startActivity(intent);
                }else{
                    //si no devuelve nada el servidor es que el usuario o contraseña estan mal
                    tv_error.setText("Error usuario y/o contraseña son incorrectos");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                //en caso de error en la respuesta muestro un toast del error
                //Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
                tv_error.setText(error.toString());
            }
        }){
            /**
             * Aqui generamos el JSON para enviarlo al server PHP
             * @return devuelve el Map
             * @throws AuthFailureError
             */
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                //creo el mapa de tipo string
                Map<String,String> parametros = new HashMap<String,String>();
                //e introduzco los parametros usuario y contraseña
                parametros.put("usuario",ed_usuario.getText().toString());
                parametros.put("password",ed_password.getText().toString());
                return parametros;
            }
        };
        //creo la instancia del request para procesar las peticiones a traves de aqui
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(sr);
    }
}