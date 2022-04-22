package com.example.lupworkmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class registroActivity extends AppCompatActivity implements Response.Listener<JSONObject> ,Response.ErrorListener {

    Button registro;
    EditText usuario,password,names;
    RequestQueue queue;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_registro);
        registro = findViewById(R.id.preRegistro);
        names = findViewById(R.id.namesR);
        usuario = findViewById(R.id.usuarioR);
        password = findViewById(R.id.passwordR);

        queue = Volley.newRequestQueue(registroActivity.this);

        inicio();
    }

    private void inicio(){
        registro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                completarRegistro();
            }
        });
    }

    @Override
    public void onResponse(JSONObject response) {

        Toast.makeText(this,"Se ha registrado el usuario " + usuario.getText().toString(), Toast.LENGTH_SHORT).show();

        Intent inicioApp = new Intent(this,SesionActivity.class);
        startActivity(inicioApp);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        System.out.println("ERRORRR" + error.toString());
        Toast.makeText(this,"Error en el registro", Toast.LENGTH_SHORT).show();

    }


    private void completarRegistro(){
        // Instantiate the RequestQueue.
        String url ="http://ec2-52-56-170-196.eu-west-2.compute.amazonaws.com/ecalvo023/WEB/registrar.php?names="+names.getText().toString()+"&user="+usuario.getText().toString()+
                "&pwd="+password.getText().toString();

        JsonRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,this,this);

        queue.add(jsonRequest);
    }
}