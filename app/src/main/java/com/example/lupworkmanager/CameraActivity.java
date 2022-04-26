package com.example.lupworkmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;

import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class CameraActivity extends AppCompatActivity{

    private final Executor executor = Executors.newSingleThreadExecutor();
    Camera camera;

    Button captura;
    Button barcode;
    Button atras;
    Button next;
    Button pause;
    @SuppressLint("StaticFieldLeak")
    static Button stop;
    Button historial;
    ImageView imageView;
    @SuppressLint("StaticFieldLeak")
    static View progressBar;
    PreviewView mPreviewView;
    Switch bluetooth;
    Intent reinicio;

    //TEXTO OCR
    String texto;

    //TIEMPO EJECUCION
    long initialTime;

    //GUARDADO SERVIDOR
    String fotoen64;


    MediaPlayer mp ; //Sonido captura

    private TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private InputImage imagen;

    LanguageIdentifier languageIdentifier;
    BarcodeScanner scanner;
    Set<Voice> mainList ;
    List<Voice> auxList = new ArrayList<>();


    //private LupViewModel mViewModel;
    private static final String TAG = CameraActivity.class.getSimpleName();
    private static TextToSpeech textToSpeech;
    private static TextToSpeech ahoTTS;

    //BDD
    RegistroBDHelper dbHelper;
    DateTimeFormatter dtf4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();

        //INICIO BASE DE DATOS
        dbHelper = new RegistroBDHelper(getApplicationContext());
        dtf4 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        //Inicializar objeto reinicio por si las moscas
        reinicio = new Intent(CameraActivity.this,Inicio.class);

        //GIRO DE PANTALLA
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        //SE SETEA EL CONTENT VIEW HASTA QUE SE CARGEN LOS COMPONENTES NECESARIOS
        setContentView(R.layout.activity_inicio);

        //SONIDO DE CAPTURA INICIALIZACION
        mp = MediaPlayer.create(this,R.raw.captura);

        // Get the ViewModel
        //mViewModel = new LupViewModel(getApplication());

        // ARRANCAMOS LANGUAJE DETECTION
        LanguageIdentificationOptions identifierOptions =
                new LanguageIdentificationOptions.Builder()
                        .setConfidenceThreshold(0.8f)  //SOLO TENER EN CUENTA SI TIENE MAS DE X % DE PERTENECER
                        .build();

        languageIdentifier = LanguageIdentification
                .getClient(identifierOptions);

        //ARRANCAMOS BARCODE SCAN

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats( //SETEAMOS LOS FORMATOS DISPONIBLES
                                Barcode.FORMAT_CODE_128,
                                Barcode.FORMAT_CODE_39,
                                Barcode.FORMAT_CODE_93,
                                Barcode.FORMAT_CODABAR,
                                Barcode.FORMAT_EAN_8,
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_ITF,
                                Barcode.FORMAT_UPC_A,
                                Barcode.FORMAT_UPC_E)
                        .build();

        scanner = BarcodeScanning.getClient();

        //ARRANCAMOS TTS DE GOOGLE
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {

                Log.i(TAG, "Text-To-Speech engine is initialized");
            } else if (status == TextToSpeech.ERROR) {

                Log.i(TAG, "Error occurred while initializing Text-To-Speech engine");
            }
        },"com.google.android.tts");

        //MOSTRAR MOTORES
        //System.out.println("Motores de sintesis : " + textToSpeech.getEngines());

        //ARRANCAMOS TTS DE AHOLAB
        ahoTTS = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {

                //SI INICIA RECOGEMOS LAS VOCES Y LAS METEMOS EN UNA LISTA
                mainList=ahoTTS.getVoices();
                auxList.addAll(mainList);

                //SI SE INICIA BIEN EMPIEZA LA EJECUCION
                startCamera();

                Log.i(TAG, "Text-To-Speech AHOTTS engine is initialized");
            } else if (status == TextToSpeech.ERROR) {

                Toast.makeText(CameraActivity.this,
                        "AHO TTS NO SE HA ENCONTRADO", Toast.LENGTH_LONG).show();

                startCamera();

                Log.i(TAG, "Error occurred while initializing Text-To-Speech engine");
            }
        },"com.aholab.ahottsandroid");

        ahoTTS.setSpeechRate(0.90f); //SETEANDO VELOCIDAD DEL LA VOZ


    }

    private void startCamera() {  //EMPIEZA LA EJECUCION DESPUES DE ARRANCAR LOS SERVICIOS NECESARIOS

        //EMPEZAMOS ARRANCANDO LA CAMARA

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {

                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                start(cameraProvider);  //LLAMAR A ScanWorker

            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("RestrictedApi")
    private void start(@NonNull ProcessCameraProvider cameraProvider) {

        //LIVE PREVIEW DE LA CAMARA
        Preview preview = new Preview.Builder()
                .build();

        //SELECTOR DE CAMARA
       CameraSelector cameraSelector = new CameraSelector.Builder()
               .requireLensFacing(CameraSelector.LENS_FACING_BACK)
               .build();


        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setImageQueueDepth(1) //MAXIMO ANALIZAR UNA FOTO A LA VEZ
                .setTargetResolution(new Size(1280, 720)) //ESPECIFICANDO RESOLUCION
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        //SET DE CONFIGURACION DE CAPTURA
        final ImageCapture imageCapture = builder
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                //.setTargetRotation(view.getDisplay().getRotation())
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build();

        //ORIENTACION DE LA CAPTURA AUTOMATICA

        OrientationEventListener orientationEventListener = new OrientationEventListener((Context)this) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation;

                // Monitors orientation values to determine the target rotation value
                if (orientation >= 45 && orientation < 135) {
                    rotation = Surface.ROTATION_270;
                } else if (orientation >= 135 && orientation < 225) {
                    rotation = Surface.ROTATION_180;
                } else if (orientation >= 225 && orientation < 315) {
                    rotation = Surface.ROTATION_90;
                } else {
                    rotation = Surface.ROTATION_0;
                }

                imageCapture.setTargetRotation(rotation);
            }
        };

        orientationEventListener.enable();
        //

       //CAMBIAMOS DE INTERFAZ + AÑADIMOS elementos interfaz
        setContentView(R.layout.activity_camera);
        captura = findViewById(R.id.capturar);
        stop = findViewById(R.id.stop);
        imageView = findViewById(R.id.imageView);
        mPreviewView = findViewById(R.id.camera);
        progressBar = findViewById(R.id.progressBar2);
        historial = findViewById(R.id.historial);
        bluetooth = findViewById(R.id.bt);
        barcode = findViewById(R.id.barcode);
        atras = findViewById(R.id.atras);
        next = findViewById(R.id.next);
        pause = findViewById(R.id.pause);

        //

        //DEFINIENDO RECTANGULO DE CAPTURA IGUAL A LA PREVIEW
        ViewPort viewPort = ((PreviewView)findViewById(R.id.camera)).getViewPort();
        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageAnalysis)
                .addUseCase(imageCapture)
                .setViewPort(viewPort)
                .build();


        //Tratando excepcion de cambio de camara ,si no se encuentra trasera a frontal
        try{
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);}

        catch (Exception e){
            // No se ha encontrado camara trasera y pasa a frontal

            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build();

            try {
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);
                }catch (Exception e2){

                //En el caso de que no se encuentre ninguna camara se reiniciara la aplicacion

                Toast.makeText(CameraActivity.this,
                        "Camara no detectada ,reinicie aplicacion", Toast.LENGTH_LONG).show();

                startActivity(reinicio);
            }
        }

        //CONTROL CAMARA
        CameraControl controlCamara = camera.getCameraControl();
        //LIVE INFO CAMARA
        CameraInfo infoCamara = camera.getCameraInfo();
        //System.out.println("INFO CAMARA AQUI:");
        System.out.println(infoCamara.getCameraState());

        //listo para poder capturar

        Toast.makeText(CameraActivity.this,
                "Listo para capturar", Toast.LENGTH_LONG).show();

        //AUTO-FOCUS EN LA RECORTE QUE CAPTURA
        //Se tienen que conseguir los puntos en donde se quiere hacer el enfoque y mandarselo al metodo

        //final MeteringPointFactory factory = mPreviewView.createMeteringPointFactory(cameraSelector);
        //final MeteringPoint point = factory.createPoint(event.getX(), motionEvent.getY());
        //final FocusMeteringAction action = new FocusMeteringAction.Builder(point).build();
        //controlCamara.startFocusAndMetering(action);

        //
        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        //BOTON PARAR / PAUSE DE HABLAR
        stop.setOnClickListener(view -> {
            if (textToSpeech.isSpeaking() || ahoTTS.isSpeaking()) { //PARAR TTS
                textToSpeech.stop();
                ahoTTS.stop();
            }
            showWorkFinished(); // QUITAR BOTONES OCULTOS
        });

        //BOTON BARCODE
        barcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //WorkerUtils.setExecutionTime(System.currentTimeMillis()); //INICIO TIEMPO DE EJECUCION

                mp.start(); //Sonido captura
                showWorkInProgress(); //MOSTRAR BOTONES OCULTOS

                //mViewModel.cancelWork(); //PARAR WORK MANAGER PARA NO TENER INTERFERENCIAS

                System.out.println("CAPTURANDO!");

                imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() { //TOMAMOS LA FOTO
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {

                        System.out.println("CAPTURADO!");
                        Bitmap imagenBitmap = imageProxyToBitmap(image); //PASAMOS LA FOTO A BITMAP

                        imagen = InputImage.fromBitmap(imagenBitmap, 0);

                        Task<List<Barcode>> result = scanner.process(imagen)
                                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                                    @Override
                                    public void onSuccess(List<Barcode> barcodes) {
                                        // Task completed successfully
                                        // ...
                                        System.out.println("PROCESADO ACABADO!");
                                        ahoTTS.setLanguage(new Locale("es", "SPA"));

                                        if (barcodes.size()==0){
                                            System.out.println("ENTRA DENTRO DE SPEAK SIN CODIGOS");
                                            ahoTTS.speak("No se han detectado codigo de barras", TextToSpeech.QUEUE_FLUSH, null, null);
                                        }
                                        for (Barcode barcode : barcodes) {
                                            Rect bounds = barcode.getBoundingBox();
                                            Point[] corners = barcode.getCornerPoints();

                                            String rawValue = barcode.getRawValue();

                                            int valueType = barcode.getValueType();
                                            // See API reference for complete list of supported types
                                            String info = barcode.getDisplayValue();


                                            ahoTTS.speak(info, TextToSpeech.QUEUE_FLUSH, null, null);
                                            System.out.println("INFO: " + info + " "+valueType + " "+rawValue + " "+ barcode.getCalendarEvent()+ " "+barcode.getContactInfo()+ " "+barcode.getDriverLicense()+barcode.getEmail()+ " "+barcode.getPhone()+ " "+barcode.getSms()+ " "+barcode.getUrl()+ " "+barcode.getWifi());
                                        }
                                    }
                                });

                        image.close();
                    }
                });
            }
            });


        //BOTON CAPTURAR
        captura.setOnClickListener(v -> {

            initialTime = System.currentTimeMillis(); //INICIO TIEMPO DE EJECUCION

            //mp.start(); //Sonido captura
            showWorkInProgress(); //MOSTRAR BOTONES OCULTOS

            //mViewModel.cancelWork(); //PARAR WORK MANAGER PARA NO TENER INTERFERENCIAS

            if (textToSpeech.isSpeaking()) { //PARAR TTS
                textToSpeech.stop();
            }
            if (ahoTTS.isSpeaking()) { //PARAR AHOTTS
                ahoTTS.stop();
            }

            System.out.println("CAPTURANDO!");

            AlertDialog.Builder idiomaNoSoportado = new AlertDialog.Builder(this);
            idiomaNoSoportado.setTitle("¿ REPRODUCIR ? ");
            idiomaNoSoportado.setMessage("Si pulsas OK se reproducira el texto detectado.");

            imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() { //TOMAMOS LA FOTO
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {

                    System.out.println("CAPTURADO!");
                    Bitmap imagenBitmap = imageProxyToBitmap(image); //PASAMOS LA FOTO A BITMAP

                    //GUARDADO DE IMAGEN EN EL SERVIDOR
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    imagenBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] fototransformada = stream.toByteArray();
                    fotoen64 = Base64.encodeToString(fototransformada,Base64.DEFAULT);

                    realizarGuardado(fotoen64);


                    //RECORTAMOS LA IMAGEN (800x600)

                    int width=imagenBitmap.getWidth();
                    int height=imagenBitmap.getHeight();

                    //Cogemos el centro de la imagen
                    int centerX=width/2;
                    int centerY=height/2;

                    //En el caso de que haya algun problema con el recorte que coja la foto original.
                    /*Bitmap recortado;
                    try {
                        recortado = Bitmap.createBitmap(imagenBitmap, centerX ,centerY-200, 0, 0);
                    }catch (Exception e){
                        System.out.println("Problema con el recorte del bitmap");
                        recortado = imagenBitmap;
                    }

                    Bitmap finalRecortado = recortado; */

                    Bitmap finalRecortado = imagenBitmap;

                    runOnUiThread(() -> {
                        // TODO Auto-generated method stub
                        imageView.setRotation(0); //EN PC SE VE GIRADO PERO EN MOVIL EN VERTICAL
                        imageView.setImageBitmap(finalRecortado); //SETEAMOS FOTO TOMADA
                    });

                    imagen = InputImage.fromBitmap(finalRecortado , 0); //

                    // COMIENZA DETECCION DE TEXTO EN IMAGEN (OCR)


                    recognizer.process(imagen)
                            .addOnSuccessListener(visionText -> {

                                System.out.println("INSERTA EN BASE DE DATOS");

                                // Task completed successfully
                                texto = visionText.getText();
                                System.out.print("Este es el resultado: " + texto);


                                //INSERTAR EN BDD
                                String resource = texto;
                                String hour = dtf4.format(LocalDateTime.now());

                                //Gestion tiempo ejecucion
                                long tFinal = System.currentTimeMillis();
                                double tiempoEjecucion = (tFinal - initialTime)/1000.0;

                                String timeS = Double.toString(tiempoEjecucion);
                                dbHelper.insertar(hour, resource, timeS);
                                //

                                //WORK MANAGER STARTS
                                //mViewModel.start(texto);
                                //

                                //getPossibleLanguuages(texto); //POSIBLES IDIOMAS DETECTADOS

                                //DETECTOR DE LENGUAJES
                                languageIdentifier.identifyLanguage(texto).addOnSuccessListener(idioma -> {
                                    System.out.println("IDOMA: " + idioma);
                                    Locale locale = new Locale(idioma); //CREAMOS LOCALE PARA ASIGNAR IDIOMA A TTS

                                    //SI OCR NO DETECTA NINGUN CARACTER
                                    if (texto.equals("")) {
                                        ahoTTS.setLanguage(new Locale("es", "SPA"));
                                        System.out.println("ENTRA DENTRO DE SPEAK SIN TEXTO");
                                        ahoTTS.speak("No se ha detectado texto", TextToSpeech.QUEUE_FLUSH, null, null);

                                        //SI TEXTO EN CASTELLANO O EUSKERA
                                    } else if (idioma.equals("es") || idioma.equals("eu")) {
                                        if (ahoTTS != null) {
                                            //ASIGNAMOS Lenguaje A TTS
                                            ahoTTS.setLanguage(locale);

                                            //SELECCINAMOS VOZ DEPENDE DEL IDIOMA
                                            if (idioma.equals("es")) {
                                                ahoTTS.setVoice(auxList.get(1)); //VOZ CHICO
                                            } else {
                                                ahoTTS.setVoice(auxList.get(2)); //VOZ CHICA
                                            }

                                            System.out.println("ENTRA DENTRO DE SPEAK ahotts");
                                            ahoTTS.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
                                        }

                                        //SI IDIOMA DISTINTO A CASTELLANO O EUSKERA
                                    } else {
                                        if (textToSpeech != null) {
                                            //SI TEXTO EN INGLES O CATALAN
                                            if (idioma.equals("en") || idioma.equals("ca")) {
                                                textToSpeech.setLanguage(locale);
                                                System.out.println("ENTRA DENTRO DE SPEAK google");
                                                textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
                                            }
                                         else {
                                            //NO SOPORTAR IDIOMA DIFERENTES A CATALAN/INGLES/EUSKERA/CASTELLANO
                                            ahoTTS.setLanguage(new Locale("es"));
                                            System.out.println("ENTRA DENTRO DE SPEAK IDIOMA NO SOPORTADO");
                                            ahoTTS.speak("Idioma no soportado o desconocido ,¿ desea escuchar igualmente ?", TextToSpeech.QUEUE_FLUSH, null, null);

                                            //SI NO SE DETECTA CORRECTAMENTE EL IDIOMA O NO ES SOPORTADO SE DA LA OPCION DE REPRODUCIR DE TODAS FORMAS
                                            idiomaNoSoportado.setPositiveButton("OK", (dialog, which) -> {
                                                    ttsErrorAux(texto); //REPRODUCIR IGUALMENTE
                                                });
                                            idiomaNoSoportado.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

                                            AlertDialog dialog = idiomaNoSoportado.create(); //CREAR DIALOGO
                                            dialog.show(); //MOSTRAR DIALOGO
                                            }
                                        }
                                    }

                                    //BOTON ANALISIS INTELIGENTE TEXTO

                                    pause.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if (textToSpeech.isSpeaking() || ahoTTS.isSpeaking()) { //PARAR TTS
                                                textToSpeech.stop();
                                                ahoTTS.stop();
                                            }
                                            ahoTTS.setLanguage(new Locale("es"));
                                            System.out.println("ENTRA DENTRO DE NAVEGACION");
                                            ahoTTS.speak("NAVEGACION ACTIVADA", TextToSpeech.QUEUE_FLUSH, null, null);

                                            processText(visionText);
                                        }
                                    });

                                    //Mientras habla no quitar opcion de pause
                                }).addOnCompleteListener(h->{
                                    System.out.println("COMPLETADO");
                                }).addOnFailureListener(f->{
                                    System.out.println("FALLO");
                                });

                            })
                            .addOnFailureListener(Throwable::printStackTrace);

                    image.close(); //CERRAR IMAGEN (ARCHIVO TEMPORAL)

                }
                @Override
                public void onError(@NonNull ImageCaptureException error) {
                    error.printStackTrace();
                    //CAPTURA FALLIDA REINICIAMOS
                    startActivity(reinicio);
                }

            });
        });

        //BOTON DE HISTORIAL DE BDD
        historial.setOnClickListener(view -> {
            showWorkFinished(); //QUITAR BOTONES

            if (textToSpeech.isSpeaking()) { //PARAR TTS
                textToSpeech.stop();
            }
            if (ahoTTS.isSpeaking()) { //PARAR AHOTTS
                ahoTTS.stop();
            }

            Intent intent = new Intent(CameraActivity.this,BDDActivity.class);
            startActivity(intent);
        });

        //SWITCH BLUETOOTH

        Intent blueetoothServer = new Intent(this, BluetoothActivity.class);

        bluetooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if (isChecked) { //SI CHEQUEADO
                    System.out.println("BLUETOOTH ESCUCHANDO");
                    //CARGA ACTIVIDAD BLUETOOTH
                    startActivity(blueetoothServer);
                } else {
                        // The toggle is disabled
                    }
                }
        });
    }

    //CONVERSION DE PROXY A BITMAP
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
        ByteBuffer buffer = planeProxy.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    //MOSTRAR BOTONES OCULTOS
    private void showWorkInProgress() {
        progressBar.setVisibility(View.VISIBLE);
        stop.setVisibility(View.VISIBLE);

    }
    //OCULTAR BOTONES OCULTOS
    private static void showWorkFinished() {
        progressBar.setVisibility(View.INVISIBLE);
        stop.setVisibility(View.INVISIBLE);
    }

    //OBTENER POSIBLES LENGUAJES DETECTADOS
    private void getPossibleLanguuages (String text){
        // [START get_possible_languages]
        LanguageIdentifier languageIdentifier =
                LanguageIdentification.getClient();
        languageIdentifier.identifyPossibleLanguages(text)
                .addOnSuccessListener(identifiedLanguages -> {
                    for (IdentifiedLanguage identifiedLanguage : identifiedLanguages) {
                        String language = identifiedLanguage.getLanguageTag();
                        float confidence = identifiedLanguage.getConfidence();
                        Log.i(TAG, language + " (" + confidence + ")");
                    }
                })
                .addOnFailureListener(
                        e -> {
                            // Model couldn’t be loaded or other internal error.
                            // ...
                        });
        // [END get_possible_languages]

    }

    //SI PROBLEMA CON EL TEXTO Y SE QUIERE REPRODUCIR AUN ASI
    private void ttsErrorAux(String texto){
        if (ahoTTS.isSpeaking()){ ahoTTS.stop();}
        ahoTTS.setLanguage(new Locale("es"));
        ahoTTS.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private int contador = 0;
    private void processText(Text result){
        String resultText = result.getText();
        List<Text.TextBlock> bloques = result.getTextBlocks();
        contador=0;
        next.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               Text.TextBlock bloque = bloques.get(contador);
               String texto = bloque.getText();
               String idioma = bloque.getRecognizedLanguage();
               ahoTTS.setLanguage(new Locale(idioma));
               ahoTTS.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);

               if (contador==bloques.size()-1){
                   contador = bloques.size()-1;
               }else{
                   contador = contador + 1;
               }
           }
       });
        atras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Text.TextBlock bloque = bloques.get(contador);
                String texto = bloque.getText();
                String idioma = bloque.getRecognizedLanguage();
                ahoTTS.setLanguage(new Locale(idioma));
                ahoTTS.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
                if (contador==0){
                    contador = 0;
                }else{
                    contador = contador - 1;
                }
            }
        });

    }

    //METODO GET TTS
    static TextToSpeech getTTS(){
        return ahoTTS;
    }

    //METODOS RELACIONADOS CON INSERCCION DE FOTOS EN EL SERVIDOR


    private void realizarGuardado(final String foto) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                "http://ec2-52-56-170-196.eu-west-2.compute.amazonaws.com/ecalvo023/WEB/imagenes.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.print(response);
                        Toast.makeText(getApplicationContext(), "Guardado exitoso", Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "ERROR EN EL GUARDADO", Toast.LENGTH_LONG).show();
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> parametros = new Hashtable<String, String>();
                parametros.put("imagen", foto);
                parametros.put("user",User.getUsuario());

                return parametros;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }
}