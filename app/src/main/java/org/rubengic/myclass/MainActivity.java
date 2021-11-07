package org.rubengic.myclass;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity{

    private EditText ed_usuario, ed_password;
    private Button b_login;
    TextView tv_error;

    /**
     * para la autentificación por huella, necesitamos 3 variables
     * - executor: ejecuta la acción
     * - biometricPrompt: usa el sensor de huella dactilar
     * - promptInfo: Que activa el entorno grafico a mostrar al usuario
     */
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

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
                validarUsuario("http://192.168.1.42:8080/validar_login.php");
                //validarUsuario("http://192.168.47.2:8080/validar_login.php");
            }
        });

        //inicializo la ejecución para el sensor biometrico
        executor = ContextCompat.getMainExecutor(this);

        //creo la funcionalidad biometrica
        biometricPrompt = new BiometricPrompt(
                MainActivity.this,
                executor,
                new BiometricPrompt.AuthenticationCallback(){//función de autentificación
                    /**
                     * entra en esta función si tiene un error de autentificación
                     * (que no tenga el sensor de huellas, no tenga registrada una huella, etc)
                     */

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);

                        Toast.makeText(getApplicationContext(),
                                "Authentication error: " + errString, Toast.LENGTH_SHORT)
                                .show();

                    }

                    /**
                     * En caso de que lea mal la huella
                     */
                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(getApplicationContext(), "Authentication failed",
                                Toast.LENGTH_SHORT)
                                .show();
                    }

                    /**
                     * En caso de que lea bien la huella
                     * @param result es el resultado de leer bien la huella
                     */
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Toast.makeText(getApplicationContext(),
                                "Authentication succeeded!", Toast.LENGTH_SHORT).show();

                        //si lee bien la huella se autentifica
                        Intent intent = new Intent(getApplicationContext(),PrincipalActivity.class);
                        intent.putExtra("id",1);
                        startActivity(intent);
                    }
                }
        );

        /**
         * inicializo el entorno grafico de la huella dactilar, pasandole el titulo
         * , el subtitulo y un boton para cancelar.
         */
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Login Huella dactilar")
                .setSubtitle("Inicia sesión usando tu huella dactilar")
                .setNegativeButtonText("Usar contraseña de cuenta")
                .build();

        /*ImageButton ib_huella = (ImageButton) findViewById(R.id.ib_huella);

        ib_huella.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                biometricPrompt.authenticate(promptInfo);
            }
        });*/

        biometricPrompt.authenticate(promptInfo);

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
                    try {
                        JSONObject log_user = new JSONObject(response);
                        intent.putExtra("id",log_user.getInt("id_alumno"));
                        //Toast.makeText(MainActivity.this, String.valueOf(log_user.getInt("id_alumno")), Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }


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