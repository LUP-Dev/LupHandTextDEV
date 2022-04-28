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

public class SesionActivity extends AppCompatActivity implements Response.Listener<JSONObject> ,Response.ErrorListener{

    Button inicioSesion,registro;
    EditText usuario,password;
    RequestQueue queue;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_sesion);
        inicioSesion = findViewById(R.id.inicioSesion);
        registro = findViewById(R.id.preRegistro);
        usuario = findViewById(R.id.usuarioR);
        password = findViewById(R.id.passwordR);

        queue = Volley.newRequestQueue(SesionActivity.this);

        inicio();
    }

    private void inicio(){
        inicioSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               iniciarSesion();
            }
        });
        registro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registro = new Intent(SesionActivity.this, registroActivity.class);
                startActivity(registro);
            }
        });

    }

    @Override
    public void onResponse(JSONObject response) {

        Toast.makeText(this,"Se ha encontrado el usuario " + usuario.getText().toString(), Toast.LENGTH_SHORT).show();


        JSONArray jsonArray = response.optJSONArray("datos");
        JSONObject jsonObject = null;

        try {
            jsonObject=jsonArray.getJSONObject(0);
            User.setUsuario(jsonObject.optString("user"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent inicioApp = new Intent(this,CameraActivity.class);
        startActivity(inicioApp);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        System.out.println("ERRORRR" + error.toString());
        Toast.makeText(this,"Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();

    }

    //METODO RESPONSABLE DE MANDAR LA INFORMACION AL SERVIDOR PARA PROCEDER AL LOGEO DEL USUARIO

    private void iniciarSesion(){
        // Instantiate the RequestQueue.
        String url ="http://ec2-52-56-170-196.eu-west-2.compute.amazonaws.com/ecalvo023/WEB/sesion.php?user="+usuario.getText().toString()+
                "&pwd="+password.getText().toString();

        JsonRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,this,this);

        queue.add(jsonRequest);
    }
}