package org.rubengic.myclass;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import java.util.List;
import java.util.TimeZone;

public class PrincipalActivity extends AppCompatActivity {

    private TextView titulo;
    private int id_alumno;
    private ArrayList<Asignatura> list_asig;
    private RecyclerView rv_lista;
    private ListaHorarioNow adaptador;

    private ConstraintLayout c_layout;

    private float posX=-1, posY=-1;

    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener sensorEventListener;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int sacudida = 600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        id_alumno = getIntent().getExtras().getInt("id");

        titulo = (TextView) findViewById(R.id.tv_titulo);

        c_layout = (ConstraintLayout) findViewById(R.id.c_layout);

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

        //activa la realidad aumentada
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

        //acciones multitouch
        c_layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //compruebo que usa más de 1 dedo
                if(event.getPointerCount() > 1){

                    //obtengo la acción
                    int action = event.getActionMasked();

                    //tipos de acciones
                    switch (action){
                        //mantener pulsado la pantalla
                        case MotionEvent.ACTION_POINTER_DOWN:
                            //calculo las posiciones x e y
                            posX = event.getX();
                            posY = event.getY();
                            break;
                        //deja de pulsar la pantalla
                        case MotionEvent.ACTION_POINTER_UP:
                            //obtengo las ultimas posiciones de donde toco la pantalla y calcula sus diferencias x e y
                            float difX = posX-event.getX();
                            float difY = posY-event.getY();

                            /**
                             * si la diferencia de x es > a la de y y la diferencia de y en valor absoluto de y es > a 100
                             * esta deslizando los dedos de arriba a bajo.
                             * Lo de 100 es porque la pantalla de ancho no hay mas de 100 pixeles, pero de largo hay más de 100 pixeles
                             */
                            if(difX > difY && Math.abs(difY) > 100) {
                                obtenerListaJSON("http://192.168.1.42:8080/lista_asignaturas.php?id="+id_alumno+"&semana="+nd);
                                //obtenerListaJSON("http://192.168.47.2:8080/lista_asignaturas.php?id="+id_alumno+"&semana="+nd);
                                Toast.makeText(PrincipalActivity.this, "Lista actualizada", Toast.LENGTH_SHORT).show();
                                //Log.e("Hacia abajo","Funciona: x="+String.valueOf(difX)+", y="+String.valueOf(difY));

                            /**
                             * si la diferencia de x es < a la de y y la diferencia de y en valor absoluto de y es > a 100
                             * esta deslizando los dedos de arriba a bajo.
                             * Lo de 100 es porque la pantalla de ancho no hay mas de 100 pixeles, pero de largo hay más de 100 pixeles
                             */
                            }else if(difX < difY && Math.abs(difY) > 100) {
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

        //acciones acelerómetro
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                Sensor mySensor = sensorEvent.sensor;

                if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float x = sensorEvent.values[0];
                    float y = sensorEvent.values[1];
                    float z = sensorEvent.values[2];

                    long curTime = System.currentTimeMillis();

                    if ((curTime - lastUpdate) > 100) {
                        long diffTime = (curTime - lastUpdate);
                        lastUpdate = curTime;

                        float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

                        if (speed > sacudida) {
                            Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent1);
                        }

                        last_x = x;
                        last_y = y;
                        last_z = z;
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

//        if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null){
//            List<Sensor> gravSensors = sensorManager.getSensorList(Sensor.TYPE_GRAVITY);
//            for(int i=0; i<gravSensors.size(); i++) {
//                if ((gravSensors.get(i).getVendor().contains("Google LLC")) &&
//                        (gravSensors.get(i).getVersion() == 3)){
//                    // Use the version 3 gravity sensor.
//                    mSensor = gravSensors.get(i);
//                }
//            }
//        }
//        if (mSensor == null){
//            // Use the accelerometer.
//            if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
//                mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//            } else{
                // Sorry, there are no accelerometers on your device.
                // You can't play this game.
//            }
//        }
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

                //creo el objeto JSON donde guardara cada uno de los valores del array
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
                            //creo el objeto json con dichos datos
                            json_object = response.getJSONObject(i);

                            //creo el objeto asignatura a traves de los datos json
                            asignatura = new Asignatura(
                                    json_object.getInt("id"),
                                    json_object.getString("nombre"),
                                    json_object.getString("aula"),
                                    json_object.getString("hora"));

                            //y añado a la lista la nueva asignatura
                            list_asig.add(asignatura);
                        }catch (JSONException e){//en caso de que no funcione el JSON
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
}