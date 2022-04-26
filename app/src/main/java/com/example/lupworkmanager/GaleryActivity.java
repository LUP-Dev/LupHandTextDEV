package com.example.lupworkmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
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

import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

public class GaleryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galery);

        Objects.requireNonNull(getSupportActionBar()).hide();

        getImagenes();


    }

    private void getImagenes() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                "http://ec2-52-56-170-196.eu-west-2.compute.amazonaws.com/ecalvo023/WEB/selectImagenes.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        JSONArray jsonObject = null;
                        try {
                            jsonObject = new JSONArray(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        System.out.println("OBJECT : "+jsonObject.toString());

                        /*try {
                            ObjectMapper mapper = new ObjectMapper();
                            String datos = jsonObject.getString("0");

                            try {
                                String[] pp1 = mapper.readValue(datos, String[].class);
                                System.out.println("--FOTOS: " + pp1[0]);
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }

                            //System.out.println("--FOTOS: " + datos);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }*/

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
}