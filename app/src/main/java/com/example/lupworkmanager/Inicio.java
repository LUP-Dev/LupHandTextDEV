package com.example.lupworkmanager;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Objects;

public class Inicio extends AppCompatActivity {

    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    private final int REQUEST_CODE_PERMISSIONS = 1001;

    RegistroBDHelper dbHelper = new RegistroBDHelper(this);

    //CLASE DE PANTALLA INICIO + PERMISOS

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_inicio);

        borrarBaseDeDatos(); //BORRAR BASE DE DATOS AL INICIAR LA APP

        //PEDIR PERMISOS
        while(!allPermissionsGranted()){

            System.out.println("PIDIENDO PERMISOS ");
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);

        }

        System.out.println("PERMISOS DADOS");

        Intent intent = new Intent(this, SesionActivity.class);
        startActivity(intent);

    }

    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {

            } else {
                //Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                //this.finish();
            }
        }
    }

    private void borrarBaseDeDatos(){
        dbHelper.deleteAll();
    }


}