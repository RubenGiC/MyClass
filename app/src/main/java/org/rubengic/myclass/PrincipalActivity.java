package org.rubengic.myclass;

import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class PrincipalActivity extends AppCompatActivity {

    private TextView titulo;
    private int id_alumno;
    private ArrayList<Asignatura> list_asig;
    private RecyclerView rv_lista;
    private ListaHorarioNow adaptador;

    //acción del multitouch
    private int mActivePointId;

    private FrameLayout m_layout;

    private float posX=-1, posY=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        id_alumno = getIntent().getExtras().getInt("id");

        titulo = (TextView) findViewById(R.id.tv_titulo);

        m_layout = (FrameLayout) findViewById(R.id.f_layout);

        Calendar calendar = new GregorianCalendar(TimeZone.getDefault());

        int nd = calendar.get(Calendar.DAY_OF_WEEK);

        String titulo_s = (String) titulo.getText();

        switch (nd){
            case 2:
                titulo_s = titulo_s + " Lunes";
                break;
            case 3:
                titulo_s = titulo_s + " Martes";
                break;
            case 4:
                titulo_s = titulo_s + " Miercoles";
                break;
            case 5:
                titulo_s = titulo_s + " Jueves";
                break;
            case 6:
                titulo_s = titulo_s + " Viernes";
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
        //http://localhost:8080/lista_asignaturas.php?id=1&semana=1
        obtenerListaJSON("http://192.168.1.42:8080/lista_asignaturas.php?id="+id_alumno+"&semana="+nd);
        //obtenerListaJSON("http://192.168.47.2:8080/lista_asignaturas.php?id="+id_alumno+"&semana="+nd);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floatbotton);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(list_asig.size()>0) {
                    Intent intent = new Intent(getApplicationContext(), ArCoreClass.class);
                    intent.putExtra("asignatura", list_asig.get(0).getNombre());
                    intent.putExtra("aula", list_asig.get(0).getAula());
                    startActivity(intent);
                }else{
                    Toast.makeText(PrincipalActivity.this, "No tiene asignaturas hoy", Toast.LENGTH_SHORT).show();
                }
            }
        });


        m_layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //Toast.makeText(PrincipalActivity.this, "Has tocado con "+String.valueOf(event.getPointerCount()), Toast.LENGTH_SHORT).show();
                if(event.getPointerCount() > 1){
                    //Toast.makeText(PrincipalActivity.this, "Has tocado con 2 dedos o mas", Toast.LENGTH_SHORT).show();
                    int action = event.getActionMasked();

                    switch (action){
                        case MotionEvent.ACTION_POINTER_DOWN:
                            posX = event.getX();
                            posY = event.getY();
                            //Toast.makeText(PrincipalActivity.this, "ENTRAAAAAA", Toast.LENGTH_SHORT).show();
                            break;
                        case MotionEvent.ACTION_POINTER_UP:
                            float difX = posX-event.getX();
                            float difY = posY-event.getY();
                            //Toast.makeText(PrincipalActivity.this, "-->X=" + String.valueOf(difX) + ", Y=" + String.valueOf(difY), Toast.LENGTH_SHORT).show();
                            if(difX > difY && Math.abs(difY) > 100) {
                                obtenerListaJSON("http://192.168.1.42:8080/lista_asignaturas.php?id="+id_alumno+"&semana="+nd);
                                //obtenerListaJSON("http://192.168.47.2:8080/lista_asignaturas.php?id="+id_alumno+"&semana="+nd);
                                Toast.makeText(PrincipalActivity.this, "Lista actualizada", Toast.LENGTH_SHORT).show();
                                //Log.e("Hacia abajo","Funciona: x="+String.valueOf(difX)+", y="+String.valueOf(difY));
                            }
                            if(difX < difY && Math.abs(difY) > 100) {
                                if(list_asig.size()>0) {
                                    Intent intent = new Intent(getApplicationContext(), ArCoreClass.class);
                                    intent.putExtra("asignatura", list_asig.get(0).getNombre());
                                    intent.putExtra("aula", list_asig.get(0).getAula());
                                    startActivity(intent);
                                }else{
                                    Toast.makeText(PrincipalActivity.this, "No tiene asignaturas hoy", Toast.LENGTH_SHORT).show();
                                }
                            }
                            break;
                    }
                }

                return true;
            }
        });

        /*rv_lista.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(PrincipalActivity.this, "Has tocado con "+String.valueOf(event.getPointerCount()+"(2)"), Toast.LENGTH_SHORT).show();
                if(event.getPointerCount() > 1){
                    Toast.makeText(PrincipalActivity.this, "Has tocado con 2 dedos o mas (2)", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });*/


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
                        }catch (JSONException e){
                            Toast.makeText(PrincipalActivity.this, "ERRROR1: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    //create the adapter
                    adaptador = new ListaHorarioNow(list_asig);
                    //and add the adapter to the recycler view
                    rv_lista.setAdapter(adaptador);

                }else{
                    //si no devuelve nada el servidor es que no tiene asignaturas ese dia
                    Toast.makeText(PrincipalActivity.this, "Hoy no tiene clase", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                //en caso de error en la respuesta muestro un toast del error
                Toast.makeText(PrincipalActivity.this, "ERRROR2: "+error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        //creo la instancia del request para procesar las peticiones a traves de aqui
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(sr);
    }

    //función que recibe las acciones multitouch

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //obtengo el punto ID
        mActivePointId = event.getPointerId(0);

        int pointerIndex = event.findPointerIndex(mActivePointId);

        int action = MotionEventCompat.getActionIndex(event);

        return super.onTouchEvent(event);
    }
}