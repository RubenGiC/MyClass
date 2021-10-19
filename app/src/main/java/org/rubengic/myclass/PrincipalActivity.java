package org.rubengic.myclass;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class PrincipalActivity extends AppCompatActivity {

    private TextView titulo;
    private int id_alumno;
    private ArrayList<Asignatura> list_asig;
    private RecyclerView rv_lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        id_alumno = getIntent().getExtras().getInt("id");

        titulo = (TextView) findViewById(R.id.tv_titulo);

        Calendar calendar = new GregorianCalendar(TimeZone.getDefault());

        int nd = calendar.get(Calendar.DAY_OF_WEEK);

        String titulo_s = (String) titulo.getText();

        switch (nd){
            case 2:
                titulo_s = titulo_s + " Lunes";
                break;
            case 3:
                titulo_s = titulo_s + " Lunes";
                break;
            case 4:
                titulo_s = titulo_s + " Lunes";
                break;
            case 5:
                titulo_s = titulo_s + " Lunes";
                break;
            case 6:
                titulo_s = titulo_s + " Lunes";
                break;
            default:
                titulo_s = titulo_s + " Fin de Semana";
                break;
        }

        titulo.setText(titulo_s);

        list_asig = new ArrayList<>();//inicializamos la lista donde contendra los valores
        rv_lista = (RecyclerView) findViewById(R.id.rv_horario);//accedemos al reciclerview
        //administramos el diseño
        rv_lista.setLayoutManager(new LinearLayoutManager(this));

        //tipo get "192.168.1.1:8080/lista_asig.php?id="+id
        //obtenerListaJSON("");
    }

    //para validar el usuario y contraseña
    private void obtenerListaJSON(String URL){
        //genero el request de tipo POST
        JsonArrayRequest sr = new JsonArrayRequest(URL, new Response.Listener<JSONArray>() {
            /**
             * metodo para recibir la respuesta del servidor PHP
             * @param response (recibe el mensaje del servidor)
             */
            @Override
            public void onResponse(JSONArray response) {

                JSONObject json_object = null;

                //si no esta vacia, tiene asignaturas
                if(response.length()>0){

                    //si la lista no está limpia la limpiamos
                    if(!list_asig.isEmpty()){ list_asig.clear();}

                    //creamos el objeto Asignatura
                    Asignatura asignatura = null;

                    //recorro los valores del json
                    for(int i = 0; i<response.length(); ++i){
                        try {
                            json_object = response.getJSONObject(i);
                            /*
                            * json_object.getInt("id")
                            * json_object.getString("nombre")
                            * json_object.getString("aula")
                            * json_object.getString("hora")
                            * */

                            asignatura = new Asignatura(
                                    json_object.getInt("id"),
                                    json_object.getString("nombre"),
                                    json_object.getString("aula"),
                                    json_object.getString("hora"));

                            list_asig.add(asignatura);

                            Toast.makeText(PrincipalActivity.this, json_object.getString("id"), Toast.LENGTH_SHORT).show();
                        }catch (JSONException e){
                            Toast.makeText(PrincipalActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }



                }else{
                    //si no devuelve nada el servidor es que no tiene asignaturas ese dia
                    Toast.makeText(PrincipalActivity.this, "Hoy no tiene clase", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                //en caso de error en la respuesta muestro un toast del error
                Toast.makeText(PrincipalActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        //creo la instancia del request para procesar las peticiones a traves de aqui
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(sr);
    }
}