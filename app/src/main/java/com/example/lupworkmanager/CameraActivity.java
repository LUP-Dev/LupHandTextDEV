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


import com.google.common.util.concurrent.ListenableFuture;

import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;


import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Locale;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.core.MatOfRect;
//import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;


// Importa cualquier otra clase que necesites de OpenCV

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
    Intent reinicio;

    //TEXTO OCR
    String texto;

    //TIEMPO EJECUCION
    long initialTime;

    //GUARDADO SERVIDOR
    String fotoen64;


    MediaPlayer mp ; //Sonido captura

    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private InputImage imagen;

    LanguageIdentifier languageIdentifier;



    //private LupViewModel mViewModel;
    private static final String TAG = CameraActivity.class.getSimpleName();
    private static TextToSpeech textToSpeech;

    //BDD
    DateTimeFormatter dtf4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();

        // Asegúrate de que estás cargando el layout correcto aquí
        setContentView(R.layout.activity_camera); // Asegúrate de que este es el layout correcto que contiene mPreviewView

        // Inicialización de vistas después de cargar el layout correcto
        mPreviewView = findViewById(R.id.camera); // Asegúrate de que el ID es correcto
        captura = findViewById(R.id.capturar);
        stop = findViewById(R.id.stop);
        imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar2);
        atras = findViewById(R.id.atras);
        next = findViewById(R.id.next);
        pause = findViewById(R.id.pause);

        // Otros inicializadores
        mp = MediaPlayer.create(this, R.raw.captura);
        dtf4 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        reinicio = new Intent(CameraActivity.this, Inicio.class);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        initializeTextToSpeech();
        if (!loadCascadeFile()) {
            Toast.makeText(this, "No se pudo cargar el clasificador de cascada.", Toast.LENGTH_LONG).show();
            finish(); // Salir de la actividad si no se carga el clasificador
        }
        startCamera();
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.i(TAG, "Text-To-Speech engine is initialized");
            } else if (status == TextToSpeech.ERROR) {
                Log.e(TAG, "Error occurred while initializing Text-To-Speech engine");
            }
        }, "com.google.android.tts");
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(executor, this::analyzeImage);

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void analyzeImage(@NonNull ImageProxy image) {
        @SuppressLint("UnsafeOptInUsageError") Bitmap bitmap = imageProxyToBitmap(image);
        if (bitmap != null) {
            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);

            // Convertir a escala de grises
            Mat grayMat = new Mat();
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY);

            // Detectar manos
            MatOfRect handDetections = new MatOfRect();
            if (handDetector != null) {
                handDetector.detectMultiScale(grayMat, handDetections);
            }

            // Dibujar rectángulos alrededor de las manos detectadas
            for (org.opencv.core.Rect rect : handDetections.toArray()) {
                Imgproc.rectangle(mat, rect.tl(), rect.br(), new Scalar(255, 0, 0), 2);
            }

            // Convertir de vuelta a Bitmap para mostrar en ImageView
            final Bitmap processedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, processedBitmap);

            runOnUiThread(() -> imageView.setImageBitmap(processedBitmap));

            grayMat.release();
            mat.release();
            image.close();
        }
    }


    private Mat processFrame(Mat inputMat) {
        Mat grayMat = new Mat();
        Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_RGB2GRAY);
        // Aplica más procesamientos como detección de bordes, etc.
        return grayMat;  // Retorna el Mat procesado
    }

    private void detectObjects(Mat frame) {
        // Aplica algoritmos de OpenCV aquí, por ejemplo, detección de bordes, contornos, etc.
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


        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        //BOTON PARAR / PAUSE DE HABLAR
        stop.setOnClickListener(view -> {
            if (textToSpeech.isSpeaking()) { //PARAR TTS
                textToSpeech.stop();
            }
            showWorkFinished(); // QUITAR BOTONES OCULTOS
        });


        //BOTON CAPTURAR
        captura.setOnClickListener(v -> {

            initialTime = System.currentTimeMillis(); //INICIO TIEMPO DE EJECUCION

            mp.start(); //Sonido captura
            showWorkInProgress(); //MOSTRAR BOTONES OCULTOS

            //mViewModel.cancelWork(); //PARAR WORK MANAGER PARA NO TENER INTERFERENCIAS

            if (textToSpeech.isSpeaking()) { //PARAR TTS
                textToSpeech.stop();
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

                    runOnUiThread(() -> {
                        // TODO Auto-generated method stub
                        imageView.setRotation(90); //EN PC SE VE GIRADO PERO EN MOVIL EN VERTICAL
                        imageView.setImageBitmap(imagenBitmap); //SETEAMOS FOTO TOMADA
                    });

                    imagen = InputImage.fromBitmap(imagenBitmap , 90); //

                    // COMIENZA DETECCION DE TEXTO EN IMAGEN (OCR)

                    recognizer.process(imagen)
                            .addOnSuccessListener(visionText -> {

                                // Task completed successfully
                                texto = visionText.getText();
                                System.out.print("Este es el resultado: " + texto);


                                //Gestion tiempo ejecucion
                                long tFinal = System.currentTimeMillis();
                                double tiempoEjecucion = (tFinal - initialTime)/1000.0;


                                //DETECTOR DE LENGUAJES
                                languageIdentifier.identifyLanguage(texto).addOnSuccessListener(idioma -> {
                                    System.out.println("IDIOMA: " + idioma);
                                    Locale locale = new Locale(idioma); //CREAMOS LOCALE PARA ASIGNAR IDIOMA A TTS

                                    //SI OCR NO DETECTA NINGUN CARACTER
                                    if (texto.equals("")) {
                                        textToSpeech.setLanguage(new Locale("es", "SPA"));
                                        System.out.println("ENTRA DENTRO DE SPEAK SIN TEXTO");
                                        textToSpeech.speak("No se ha detectado texto", TextToSpeech.QUEUE_FLUSH, null, null);

                                        //SI TEXTO EN CASTELLANO O EUSKERA
                                    } else if (idioma.equals("es")) {
                                        if (textToSpeech != null) {
                                            //ASIGNAMOS Lenguaje A TTS
                                            textToSpeech.setLanguage(locale);

                                            System.out.println("ENTRA DENTRO DE SPEAK ahotts");
                                            textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
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
                                            textToSpeech.setLanguage(new Locale("es"));
                                            System.out.println("ENTRA DENTRO DE SPEAK IDIOMA NO SOPORTADO");
                                            textToSpeech.speak("Idioma no soportado o desconocido ,¿ desea escuchar igualmente ?", TextToSpeech.QUEUE_FLUSH, null, null);

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
                                            if (textToSpeech.isSpeaking()) { //PARAR TTS
                                                textToSpeech.stop();
                                            }
                                            textToSpeech.setLanguage(new Locale("es"));
                                            System.out.println("ENTRA DENTRO DE NAVEGACION");
                                            textToSpeech.speak("NAVEGACION ACTIVADA", TextToSpeech.QUEUE_FLUSH, null, null);

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

    private void ttsErrorAux(String texto){
        if (textToSpeech.isSpeaking()){ textToSpeech.stop();}
        textToSpeech.setLanguage(new Locale("es"));
        textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
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
               textToSpeech.setLanguage(new Locale(idioma));
               textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);

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
                textToSpeech.setLanguage(new Locale(idioma));
                textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
                if (contador==0){
                    contador = 0;
                }else{
                    contador = contador - 1;
                }
            }
        });

    }

    private CascadeClassifier handDetector;

    private boolean loadCascadeFile() {
        try {
            // Cargar el archivo de cascada desde los recursos.
            InputStream is = getResources().openRawResource(R.raw.haarcascade_hand); // Asegúrate de que el archivo se llame así en `res/raw`
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "hand_cascade.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Cargar el clasificador de cascada
            handDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (handDetector.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                handDetector = null;
                return false;
            } else {
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
            }

            cascadeDir.delete(); // Opcional: eliminar el directorio una vez que el clasificador ha sido cargado

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
        return false;
    }

}