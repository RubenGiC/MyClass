package org.rubengic.myclass;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.Layout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import java.util.Locale;
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
    private static final int sacudida = 3000;

    private SensorManager sensorManager_;
    private Sensor sensor_;
    private SensorEventListener sensorEventListener_;
    int movimientos = 0;
    //variable para reconocer la voz para transformarla a texto
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeechEngine;
    FloatingActionButton fab_mic;
    EditText ed_test;
    private ImageView micButton;

    public static final Integer RecordAudioRequestCode = 1;

    public String server = "http://192.168.8.2:8080";//"http://192.168.1.42:8080";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        ed_test = findViewById(R.id.ed_test);

        //para los permisos
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

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
        obtenerListaJSON(server+"/lista_asignaturas.php?id="+id_alumno+"&semana="+nd);
        //obtenerListaJSON("http://192.168.47.2:8080/lista_asignaturas.php?id="+id_alumno+"&semana="+nd);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floatbotton);
        fab_mic = (FloatingActionButton) findViewById(R.id.floatbotton_mic);

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

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        textToSpeechEngine = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.SUCCESS) {
                    Log.e("TTS", "Inicio de la síntesis fallido");
                }
            }
        });


        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                ed_test.setText("");
                ed_test.setHint("Listening...");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {

                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                ed_test.setText(data.get(0));
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        fab_mic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        fab_mic.setImageResource(R.drawable.ic_baseline_mic_24);
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Toast.makeText(PrincipalActivity.this, "DOWN", Toast.LENGTH_SHORT).show();
                        return false;
                    case MotionEvent.ACTION_UP:
                        fab_mic.setImageResource(R.drawable.ic_baseline_mic_none_24);
                        speechRecognizer.stopListening();
                        Toast.makeText(PrincipalActivity.this, "UP", Toast.LENGTH_SHORT).show();
                        return false;
                    default:
                        return false;
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
                                obtenerListaJSON(server+"/lista_asignaturas.php?id="+id_alumno+"&semana="+nd);
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

                        float speed = Math.abs(x - last_x)/ diffTime * 10000;

                        if (speed > sacudida) {
                            //vuelve atras
                            PrincipalActivity.super.onBackPressed();
                        }

                        last_x = x;

                    }

                    //Toast.makeText(PrincipalActivity.this, "-->"+String.valueOf(curTime - lastUpdate), Toast.LENGTH_SHORT).show();

                    /*
                    if ((curTime - lastUpdate) > 50) {

                        long diffTime = (curTime - lastUpdate);

                        float speed = Math.abs((y) - (last_y));

                        //Toast.makeText(PrincipalActivity.this, String.valueOf(speed), Toast.LENGTH_SHORT).show();

                        //Primer movimiento hacia abajo
                        if (y<1 && movimientos ==0) {
                            System.out.println("Primer movimiento para actualizar");
                            //Falta actualizar la pagina
                            Actualizar(nd);
                            //movimientos += 1;
                            //Segundo movimiento para arriba
                        } else if (y < 20 && movimientos == 1) {
                            System.out.println("Segundo movimiento para actualizar");
                            movimientos += 1;
                        }

                        if (movimientos == 2) {
                            movimientos = 0;
                            Actualizar(nd);
                        }

                        last_y = y;
                        last_z = z;
                    }*/
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

        this.StartAcelerometro();
    }

    private void StartAcelerometro(){
        sensorManager.registerListener(sensorEventListener,sensor,SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void StopAcelerometro(){
        sensorManager.unregisterListener(sensorEventListener);
    }

    public void Actualizar(int nd){
        obtenerListaJSON(server+"/lista_asignaturas.php?id="+id_alumno+"&semana="+nd);
        //obtenerListaJSON("http://192.168.47.2:8080/lista_asignaturas.php?id="+id_alumno+"&semana="+nd);
        Toast.makeText(PrincipalActivity.this, "Lista actualizada", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        this.StopAcelerometro();
        super.onPause();
    }

    @Override
    protected void onResume() {
        this.StartAcelerometro();
        super.onResume();
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
                    Toast.makeText(PrincipalActivity.this, "Hoy no tienes clase", Toast.LENGTH_SHORT).show();
                    list_asig.clear();
                    //create the adapter
                    adaptador = new ListaHorarioNow(list_asig);
                    //and add the adapter to the recycler view
                    rv_lista.setAdapter(adaptador);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                //en caso de error en la respuesta muestro un toast del error
                //Toast.makeText(PrincipalActivity.this, "ERRROR2: "+error.toString(), Toast.LENGTH_SHORT).show();
                Toast.makeText(PrincipalActivity.this, "Hoy no tienes clase", Toast.LENGTH_SHORT).show();
            }
        });
        //creo la instancia del request para procesar las peticiones a traves de aqui
        RequestQueue rq = Volley.newRequestQueue(this);
        rq.add(sr);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.RECORD_AUDIO},
                    RecordAudioRequestCode
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0 ){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();
        }
    }
}