package org.rubengic.myclass;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class PrincipalActivity extends AppCompatActivity {

    TextView p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        int id_alumno = getIntent().getExtras().getInt("id");

        p = (TextView) findViewById(R.id.tv_prueba);

        Calendar calendar = new GregorianCalendar(TimeZone.getDefault());

        int nd = calendar.get(Calendar.DAY_OF_WEEK);

        p.setText(String.valueOf(nd+id_alumno));
    }
}