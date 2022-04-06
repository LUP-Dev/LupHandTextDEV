package com.example.lupworkmanager;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class BDDActivity extends AppCompatActivity {

    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bddactivity2);
        Objects.requireNonNull(getSupportActionBar()).hide();


        RegistroBDHelper db = new RegistroBDHelper(this);
        ArrayList<HashMap<String, String>> lectureList = db.getLectures();

        //LIST VIEW DE LA BASE DE DATOS

        ListView lv = findViewById(R.id.user_list);

        ListAdapter adapter = new SimpleAdapter(BDDActivity.this, lectureList,
                R.layout.item,new String[]{"date","text","time"},
                new int[]{R.id.note_date,R.id.note_texto,R.id.note_time}){

        };

        lv.setAdapter(adapter);

        //PULSACION EN LA LISTA (REPRODUCIR ELEMENTO EN CONCRETO)

        //CREAR DIALOG
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setTitle("HISTORIAL");
        builder1.setMessage("¿Que deseas hacer?");


        lv.setClickable(true); //SETEAMOS QUE SEA CLICKEABLE
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Log.i("Click", "click en el elemento " + position + " de mi ListView");

                //Obtenemos el elemento pulsado
                HashMap<String, String> item = (HashMap<String, String>) adapterView.getAdapter().getItem(position);

                System.out.println(item);

                String texto = item.get("text");
                String idItem = item.get("id");

                //Opciones DIALOG
                builder1.setPositiveButton("Escuchar", (dialog, which) -> {
                    //VOLVER A REPRODUCIR

                    TextToSpeech tts = CameraActivity.getTTS();
                    tts.setLanguage(new Locale("es" ));
                    tts.speak(texto.toLowerCase(),TextToSpeech.QUEUE_FLUSH, null, null);
                    dialog.cancel();

                });
                builder1.setNegativeButton("Borrar Registro", (dialog, which) -> {
                    //BORRAR REGISTRO SELECCIONADO

                    db.deleteById(idItem);
                    Intent bd = new Intent(BDDActivity.this ,BDDActivity.class);
                    startActivity(bd);
                });

                AlertDialog dialog = builder1.create();
                dialog.show();

            }
        } );

        //BOTON VOLVER

        Button back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> {
            CameraActivity.getTTS().stop(); //AL DARLE AL BOTON BACK PARA EL TTS
            Intent intent = new Intent(BDDActivity.this, CameraActivity.class);
            startActivity(intent);
        });

        Button delete = findViewById(R.id.delete);

        //CREAR DIALOG
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("¡¡ALERTA!!");
        builder.setMessage("Si pulsas OK se borraran todos los registros.");

        //BOTON DELETE ALL

        delete.setOnClickListener(v -> {
            //DIALOG
            builder.setPositiveButton("OK", (dialog, which) -> {
                Intent intent = new Intent(BDDActivity.this, BDDActivity.class);
                db.deleteAll();
                Toast.makeText(BDDActivity.this,
                        "Base de datos borrada", Toast.LENGTH_LONG).show();
                startActivity(intent);
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();

        });
    }
}