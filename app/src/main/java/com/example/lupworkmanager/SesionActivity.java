package com.example.lupworkmanager;

import androidx.appcompat.app.AppCompatActivity;

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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SesionActivity extends AppCompatActivity implements Response.Listener<JSONObject> ,Response.ErrorListener{

    Button inicioSesion;
    EditText usuario,password;
    RequestQueue queue = Volley.newRequestQueue(SesionActivity.this);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sesion);
        inicioSesion = findViewById(R.id.inicioSesion);
        usuario = findViewById(R.id.usuario);
        password = findViewById(R.id.password);

        inicio();
    }

    private void inicio(){
        inicioSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               iniciarSesion();
            }
        });
    }

    @Override
    public void onResponse(JSONObject response) {

        User user = new User();
        JSONArray jsonArray = response.optJSONArray("datos");

        JSONObject jsonObject = null;

        try {
            jsonObject=jsonArray.getJSONObject(0);
            user.setUsuario(jsonObject.optString("user"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {

    }



    private void iniciarSesion(){
        // Instantiate the RequestQueue.
        String url ="http://ec2-18-132-60-229.eu-west-2.compute.amazonaws.com/ecalvo023/WEB/sesion.php?user="+usuario.getText().toString()+
                "&pwd="+password.getText().toString();

        JsonRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,this,this);

        queue.add(jsonRequest);
    }
}