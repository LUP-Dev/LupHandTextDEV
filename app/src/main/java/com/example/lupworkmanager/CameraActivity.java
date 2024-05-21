package com.example.lupworkmanager;

import static com.example.lupworkmanager.ClasificadorDeColor.clasificador;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
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

import java.nio.ByteBuffer;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class CameraActivity extends AppCompatActivity {

    //private LupViewModel mViewModel;
    private static final String TAG = CameraActivity.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    static Button stop;
    @SuppressLint("StaticFieldLeak")
    static View progressBar;
    private static TextToSpeech textToSpeech;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    Camera camera;
    Button captura;
    Button color;
    Button barcode;
    Button atras;
    Button next;
    Button pause;
    Button historial;
    ImageView imageView;
    PreviewView mPreviewView;
    Intent reinicio;
    //TEXTO OCR
    String texto;
    //TIEMPO EJECUCION
    long initialTime;
    //GUARDADO SERVIDOR
    String fotoen64;

    Switch modo, flash;
    MediaPlayer mp; //Sonido captura
    LanguageIdentifier languageIdentifier;
    //BDD
    DateTimeFormatter dtf4;
    private InputImage imagen;
    private int contador = 0;

    private int centralPixelColor;

    private Timer timer;
    private TextView textoLinterna;

    private View burbuja;
    private View cuadrado;
    private ImageCapture imageCapture;
    private TimerTask timerTask;

    //OCULTAR BOTONES OCULTOS
    private static void showWorkFinished() {
        progressBar.setVisibility(View.INVISIBLE);
        stop.setVisibility(View.INVISIBLE);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();

        //INICIO BASE DE DATOS
        dtf4 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        //Inicializar objeto reinicio por si las moscas
        reinicio = new Intent(CameraActivity.this, Inicio.class);

        //GIRO DE PANTALLA
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        //SE SETEA EL CONTENT VIEW HASTA QUE SE CARGEN LOS COMPONENTES NECESARIOS
        setContentView(R.layout.activity_inicio);

        //SONIDO DE CAPTURA INICIALIZACION
        mp = MediaPlayer.create(this, R.raw.captura);

        // ARRANCAMOS LANGUAJE DETECTION
        LanguageIdentificationOptions identifierOptions =
                new LanguageIdentificationOptions.Builder()
                        .setConfidenceThreshold(0.8f)  //SOLO TENER EN CUENTA SI TIENE MAS DE X % DE PERTENECER
                        .build();

        languageIdentifier = LanguageIdentification
                .getClient(identifierOptions);


        //ARRANCAMOS TTS DE GOOGLE
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.i(TAG, "Text-To-Speech engine is initialized");
            } else if (status == TextToSpeech.ERROR) {
                Log.i(TAG, "Error occurred while initializing Text-To-Speech engine");
            }
        }, "com.google.android.tts");

        startCamera();
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

        AtomicReference<ImageCapture.Builder> builder = new AtomicReference<>(new ImageCapture.Builder());

        //SET DE CONFIGURACION DE CAPTURA
        imageCapture = builder.get()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                //.setTargetRotation(view.getDisplay().getRotation())
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build();

        //ORIENTACION DE LA CAPTURA AUTOMATICA

        OrientationEventListener orientationEventListener = new OrientationEventListener((Context) this) {
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


        //CAMBIAMOS DE INTERFAZ + AÑADIMOS elementos interfaz
        setContentView(R.layout.activity_camera);
        captura = findViewById(R.id.capturar);

        color = findViewById(R.id.color);
        color.setVisibility(View.INVISIBLE);
        modo = findViewById(R.id.switchModo);

        flash = findViewById(R.id.switchFlash);
        textoLinterna = findViewById(R.id.text_flash);
        flash.setVisibility(View.INVISIBLE);
        textoLinterna.setVisibility(View.INVISIBLE);
        burbuja = findViewById(R.id.color_bubble);
        burbuja.setVisibility(View.INVISIBLE);
        cuadrado = findViewById(R.id.cuadradoFoco);
        cuadrado.setVisibility(View.INVISIBLE);

        stop = findViewById(R.id.stop);
        imageView = findViewById(R.id.imageView);
        mPreviewView = findViewById(R.id.camera);
        progressBar = findViewById(R.id.progressBar2);
        atras = findViewById(R.id.atras);
        next = findViewById(R.id.next);
        pause = findViewById(R.id.pause);


        //DEFINIENDO RECTANGULO DE CAPTURA IGUAL A LA PREVIEW
        ViewPort viewPort = ((PreviewView) findViewById(R.id.camera)).getViewPort();
        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageAnalysis)
                .addUseCase(imageCapture)
                .setViewPort(viewPort)
                .build();


        //Tratando excepcion de cambio de camara ,si no se encuentra trasera a frontal
        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);

        } catch (Exception e) {
            // No se ha encontrado camara trasera y pasa a frontal
            cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build();

            try {
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);
            } catch (Exception e2) {

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

        modo.setOnClickListener(v -> {
            //Modo Color
            if (modo.isChecked()) {
                color.setVisibility(View.VISIBLE);
                captura.setVisibility(View.INVISIBLE);
                camera.getCameraControl().setLinearZoom(0.75f);
                flash.setChecked(false);
                flash.setVisibility(View.VISIBLE);
                textoLinterna.setVisibility(View.VISIBLE);
                burbuja.setVisibility(View.VISIBLE);
                cuadrado.setVisibility(View.VISIBLE);
                GradientDrawable drawable = (GradientDrawable) burbuja.getBackground();
                drawable.setColor(Color.WHITE);
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_OFF);
                imageView.setVisibility(View.INVISIBLE);
                startImageUpdateTimer();

            } else {
                //Modo Captura
                captura.setVisibility(View.VISIBLE);
                color.setVisibility(View.INVISIBLE);
                camera.getCameraControl().setLinearZoom(0f);
                flash.setVisibility(View.INVISIBLE);
                textoLinterna.setVisibility(View.INVISIBLE);
                camera.getCameraControl().enableTorch(false);
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_AUTO);
                imageView.setVisibility(View.VISIBLE);
                burbuja.setVisibility(View.INVISIBLE);
                cuadrado.setVisibility(View.INVISIBLE);
                stopImageUpdateTimer();
            }
        });

        flash.setOnClickListener(v -> {
            if (flash.isChecked()) {
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_ON);
                camera.getCameraControl().enableTorch(true);
            } else {
                imageCapture.setFlashMode(ImageCapture.FLASH_MODE_OFF);
                camera.getCameraControl().enableTorch(false);
            }
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

                    imagen = InputImage.fromBitmap(imagenBitmap, 90); //

                    // COMIENZA DETECCION DE TEXTO EN IMAGEN (OCR)

                    recognizer.process(imagen)
                            .addOnSuccessListener(visionText -> {

                                // Task completed successfully
                                texto = visionText.getText();
                                System.out.print("Este es el resultado: " + texto);


                                //Gestion tiempo ejecucion
                                long tFinal = System.currentTimeMillis();
                                double tiempoEjecucion = (tFinal - initialTime) / 1000.0;


                                //DETECTOR DE LENGUAJES
                                languageIdentifier.identifyLanguage(texto).addOnSuccessListener(idioma -> {
                                    System.out.println("IDIOMA: " + idioma);
                                    Locale locale = new Locale(idioma, "ES"); //CREAMOS LOCALE PARA ASIGNAR IDIOMA A TTS

                                    //SI OCR NO DETECTA NINGUN CARACTER
                                    if (texto.isEmpty()) {
                                        textToSpeech.setLanguage(new Locale("es", "ES"));
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
                                            } else {
                                                //NO SOPORTAR IDIOMA DIFERENTES A CATALAN/INGLES/EUSKERA/CASTELLANO
                                                textToSpeech.setLanguage(new Locale("es", "ES"));
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
                                }).addOnCompleteListener(h -> {
                                    System.out.println("COMPLETADO");
                                }).addOnFailureListener(f -> {
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

        //CAPTURA COLOR
        color.setOnClickListener(v -> {
            // Lógica para capturar una imagen cuando se presiona el botón "color"
            // Inicia la captura de imagen automáticamente al presionar el botón "color"
            stopImageUpdateTimer();
            initialTime = System.currentTimeMillis(); // INICIO TIEMPO DE EJECUCION
            calcularYDecirColor();
            progressBar.setVisibility(View.INVISIBLE);
            startImageUpdateTimer();

        });
    }

    // Método para obtener el color del píxel central de la vista previa de la cámara
    private void getColorFromCameraPreview() {
        // Obtener la vista previa de la cámara como un bitmap
        Bitmap previewBitmap = mPreviewView.getBitmap();
        // Escalar el bitmap para que coincida con las dimensiones de la burbuja
        int targetWidth = burbuja.getWidth();
        int targetHeight = burbuja.getHeight();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(previewBitmap, targetWidth, targetHeight, true);
        // Obtener el color del píxel central del bitmap escalado
        centralPixelColor = scaledBitmap.getPixel(targetWidth / 2, targetHeight / 2);
        // Establecer el color de fondo de la burbuja con el color del píxel central
        runOnUiThread(() -> {
            GradientDrawable drawable = (GradientDrawable) burbuja.getBackground();
            drawable.setColor(centralPixelColor);
        });
    }

    // Método para iniciar el temporizador y actualizar la imagen cada cierto intervalo de tiempo
    private void startImageUpdateTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 0, 100);
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        getColorFromCameraPreview(); // Método para actualizar el color de la burbuja
                    }
                });
            }
        };
    }

    // Método para detener el temporizador
    private void stopImageUpdateTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    private void calcularYDecirColor() {

        // Pronuncia la etiqueta de color utilizando el motor de texto a voz (TTS)
        pronunciarTexto(obtenerColor());
    }

    // Método para calcular el color promedio de una imagen
    private String obtenerColor() {


        // Obtiene los componentes de color individuales
        int rojo = Color.red(centralPixelColor);
        int verde = Color.green(centralPixelColor);
        int azul = Color.blue(centralPixelColor);

        return clasificador(rojo, verde, azul);
    }


    // Método para pronunciar texto en voz alta utilizando el motor de texto a voz (TTS)
    private void pronunciarTexto(String texto) {
        if (textToSpeech != null) {
            textToSpeech.setLanguage(Locale.getDefault()); // Establece el idioma predeterminado
            textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        }
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

    //OBTENER POSIBLES LENGUAJES DETECTADOS
    private void getPossibleLanguuages(String text) {
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

    private void ttsErrorAux(String texto) {
        if (textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
        textToSpeech.setLanguage(new Locale("es"));
        textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void processText(Text result) {
        String resultText = result.getText();
        List<Text.TextBlock> bloques = result.getTextBlocks();
        contador = 0;
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Text.TextBlock bloque = bloques.get(contador);
                String texto = bloque.getText();
                String idioma = bloque.getRecognizedLanguage();
                textToSpeech.setLanguage(new Locale(idioma));
                textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);

                if (contador == bloques.size() - 1) {
                    contador = bloques.size() - 1;
                } else {
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
                if (contador == 0) {
                    contador = 0;
                } else {
                    contador = contador - 1;
                }
            }
        });

    }

}