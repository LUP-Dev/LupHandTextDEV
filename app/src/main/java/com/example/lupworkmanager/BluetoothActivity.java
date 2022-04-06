package com.example.lupworkmanager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    Button close;
    Button start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_bluetooth);

        close = findViewById(R.id.close);
        start = findViewById(R.id.recieve);

        // Initializes Bluetooth adapter.
        bluetoothAdapter =  BluetoothAdapter.getDefaultAdapter();


        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothAdapter.isEnabled()){
                    bluetoothAdapter.disable();
                }
                Intent intent = new Intent(BluetoothActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });


        //SI EL TELEFONO / EMULADOR NO SOPORTA BLUETOOTH

        if (bluetoothAdapter == null) {
            Toast.makeText(BluetoothActivity.this,
                    "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
            Intent back = new Intent(BluetoothActivity.this, CameraActivity.class);
            startActivity(back);
        }else {
            //Reunimos los requisitos y podemos empezar

            // VISIBLES DURANTE X TIEMPO
            Intent discoverableIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);

            //ARRANCAMOS BLUETOOTH
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            }

            start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    start();
                }
            });
        }


    }

    public void start(){
        // EMPEZAMOS LA CONEXION COMO SERVIDOR
        if (bluetoothAdapter.isEnabled()) {
            AcceptThread conexion = new AcceptThread();
            conexion.start();
        }
        else{
            Toast.makeText(BluetoothActivity.this,
                    "Permission denied", Toast.LENGTH_LONG).show();
            Intent again = new Intent(BluetoothActivity.this, BluetoothActivity.class);
            startActivity(again);
        }
    }

    //TRABAJANDO COMO SERVIDOR

    private class AcceptThread extends Thread {

        private static final String TAG = "ConexionAceptacion";
        private static final String NAME = "lup" ;

        private BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                mmServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, UUID.fromString("766982ea-a06a-11ec-b909-0242ac120002"));
                Log.i(TAG,"Socket's listen()");

            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (socket == null) {
                Log.i(TAG,"esperando");
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket);
                    System.out.println("Conexion ACEPTADA");

                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        private void manageMyConnectedSocket(BluetoothSocket socket) {
            Log.i(TAG,"CONTROLANDO CONEXION");

            BluetoothService servicios = new BluetoothService();



        }

    }
}