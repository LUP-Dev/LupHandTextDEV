package com.example.lupworkmanager;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.widget.Toast;

import java.util.Objects;

public class Inicio extends AppCompatActivity {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_ADVERTISE",
            "android.permission.INTERNET",
            "android.permission.RECEIVE_BOOT_COMPLETED"
    };

    private final int REQUEST_CODE_PERMISSIONS = 1001;


    //CLASE DE PANTALLA INICIO + PERMISOS

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity created");
        setContentView(R.layout.activity_inicio);
        Log.d("InicioActivity", "Solicitando permisos...");
        requestPermissions();
    }

    private void requestPermissions() {
        if (!allPermissionsGranted()) {
            Log.d("InicioActivity", "No todos los permisos han sido otorgados, solicitando...");
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        } else {
            Log.d("InicioActivity", "Todos los permisos otorgados, lanzando CameraActivity...");
            launchCameraActivity();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d("InicioActivity", "Permiso no concedido: " + permission);
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
                Log.d("InicioActivity", "Permisos concedidos después de la solicitud.");
                launchCameraActivity();
            } else {
                Log.e("InicioActivity", "Permisos no concedidos por el usuario.");
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void launchCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }


}