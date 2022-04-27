package com.example.lupworkmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

public class GaleryActivity extends AppCompatActivity {

    ArrayList<Bitmap> imagenes = new ArrayList<Bitmap>();

    ImageView foto1;
    ImageView foto2;
    ImageView foto3;
    ImageView foto4;
    ImageView foto5;
    ImageView foto6;

    Button back;
    Button more;

    int numeroDeToques;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galery);

        Objects.requireNonNull(getSupportActionBar()).hide();

        foto1 = findViewById(R.id.foto1);
        foto2 = findViewById(R.id.foto2);
        foto3 = findViewById(R.id.foto3);
        foto4 = findViewById(R.id.foto4);
        foto5 = findViewById(R.id.foto5);
        foto6 = findViewById(R.id.foto6);

        back = findViewById(R.id.btnBackGallery);
        more = findViewById(R.id.cargarMas);

        numeroDeToques=0;

        getImagenes();

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent main= new Intent(GaleryActivity.this,CameraActivity.class);
                startActivity(main);
            }
        });

        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                numeroDeToques++;
                mostrarFotos(numeroDeToques);
            }
        });


    }

    private void getImagenes() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                "http://ec2-52-56-170-196.eu-west-2.compute.amazonaws.com/ecalvo023/WEB/selectImagenes.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        JSONArray array64 = null;

                        try {
                            array64 = new JSONArray(response);
                            System.out.println("OBJECT : "+array64.toString());

                            int i = 0;
                            while (i<array64.length()){
                                JSONObject object = array64.getJSONObject(i);
                                String imagen = object.getString("imagen");

                                byte [] encodeByte = Base64.decode(imagen,Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);

                                imagenes.add(bitmap);

                                i++;

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        mostrarFotos(0);

                        Toast.makeText(getApplicationContext(), "Imagenes obtenidas", Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "ERROR EN LA CONEXION", Toast.LENGTH_LONG).show();
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parametros = new Hashtable<String, String>();
                parametros.put("User", User.getUsuario());

                return parametros;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private void mostrarFotos(int i){
        System.out.println(imagenes);

        if (imagenes.size()>i){
            foto1.setImageBitmap(imagenes.get(i));}
        if (imagenes.size()>i+1){
            foto2.setImageBitmap(imagenes.get(i+1));}
        if (imagenes.size()>i+2){
            foto3.setImageBitmap(imagenes.get(i+2));}
        if (imagenes.size()>i+3){
            foto4.setImageBitmap(imagenes.get(i+3));}
        if (imagenes.size()>i+4){
            foto5.setImageBitmap(imagenes.get(i+4));}
        if (imagenes.size()>i+5){
            foto6.setImageBitmap(imagenes.get(i+5));}

    }
}