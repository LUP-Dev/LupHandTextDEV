package com.example.lupworkmanager;

import static com.example.lupworkmanager.ClasificadorDeColor.clasificador;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.ImageFormat;
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
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;
import java.time.format.DateTimeFormatter;
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
    ImageView color;
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

    Switch modo;
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
    private ImageAnalysis imageAnalysis;

    private ImageView flash, zoomMenos, zoomMas;

    private SeekBar zoomBarra;

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


        imageAnalysis = new ImageAnalysis.Builder()
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
        setContentView(R.layout.activity_main);

        color = findViewById(R.id.captureImg);

        burbuja = findViewById(R.id.color_bubble);
        cuadrado = findViewById(R.id.cuadradoFoco);
        flash = findViewById(R.id.flashBoton);
        zoomMenos = findViewById(R.id.zoomOut);
        zoomBarra = findViewById(R.id.zoomSeekBar);
        zoomMas = findViewById(R.id.zoomIn);
        mPreviewView = findViewById(R.id.camera);


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
            preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
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


        //Boton de flash
        flash.setOnClickListener(v -> {

            if (camera.getCameraInfo().getTorchState().getValue() == TorchState.OFF) {

                camera.getCameraControl().enableTorch(true);
            } else {

                camera.getCameraControl().enableTorch(false);
            }
        });


        //CAPTURA COLOR
        color.setOnClickListener(v -> {

            pronunciarTexto(clasificador(centralPixelColor));

        });

        //ZOOM
        zoomBarra.setMax(100); // Configuramos el máximo de la barra de zoom

        // Obtener la relación de zoom mínima y máxima
        float minZoom = infoCamara.getZoomState().getValue().getMinZoomRatio();
        float maxZoom = infoCamara.getZoomState().getValue().getMaxZoomRatio();

        zoomBarra.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float zoomRatio = minZoom + (maxZoom - minZoom) * (progress / 100.0f);
                    camera.getCameraControl().setZoomRatio(zoomRatio);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No se necesita implementar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No se necesita implementar
            }
        });

        zoomMenos.setOnClickListener(v -> {
            float currentZoom = infoCamara.getZoomState().getValue().getZoomRatio();
            float newZoom = Math.max(minZoom, currentZoom - (maxZoom - minZoom) / 10.0f);
            camera.getCameraControl().setZoomRatio(newZoom);
            zoomBarra.setProgress((int) ((newZoom - minZoom) / (maxZoom - minZoom) * 100));
        });

        zoomMas.setOnClickListener(v -> {
            float currentZoom = infoCamara.getZoomState().getValue().getZoomRatio();
            float newZoom = Math.min(maxZoom, currentZoom + (maxZoom - minZoom) / 10.0f);
            camera.getCameraControl().setZoomRatio(newZoom);
            zoomBarra.setProgress((int) ((newZoom - minZoom) / (maxZoom - minZoom) * 100));
        });


        //Empieza a actualizar la burbuja cada cierto tiempo
        startImageUpdateTimer();

    }


    // Método para obtener el color del píxel central de la vista previa de la cámara
    private void leerImageAnalysis() {
        imageAnalysis.setAnalyzer(executor, imageProxy -> {
            if (imageProxy != null) {
                processImageProxy(imageProxy);
                imageProxy.close();
            }
        });
    }


    // Método para iniciar el temporizador y actualizar la imagen cada cierto intervalo de tiempo
    private void startImageUpdateTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 0, 50);
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        leerImageAnalysis(); // Método para actualizar el color del cuadrado
                    }
                });
            }
        };
    }

    // Método para detener el temporizador
    private void stopImageUpdateTimer() {
        if (timer != null) {
            timer.cancel();
            timerTask.cancel();
            timer = null;
        }
    }


    // Método para pronunciar texto en voz alta utilizando el motor de texto a voz (TTS)
    private void pronunciarTexto(String texto) {
        if (textToSpeech != null) {
            textToSpeech.setLanguage(Locale.getDefault()); // Establece el idioma predeterminado
            textToSpeech.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void processImageProxy(ImageProxy imageProxy) {

        if (imageProxy.getFormat() == ImageFormat.YUV_420_888) {
            // Obtener el buffer del plano Y (luminosidad)
            ImageProxy.PlaneProxy yPlane = imageProxy.getPlanes()[0];
            ByteBuffer yBuffer = yPlane.getBuffer();

            // Obtener las dimensiones de la imagen
            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();

            // Calcular las coordenadas del píxel central
            int centerX = width / 2 - 125;
            int centerY = height / 2;

            // Obtener el índice del píxel central en el buffer Y
            int yIndex = centerY * yPlane.getRowStride() + centerX * yPlane.getPixelStride();
            int y = yBuffer.get(yIndex) & 0xFF;

            // Obtener el buffer del plano U y V (croma)
            ImageProxy.PlaneProxy uPlane = imageProxy.getPlanes()[1];
            ImageProxy.PlaneProxy vPlane = imageProxy.getPlanes()[2];
            ByteBuffer uBuffer = uPlane.getBuffer();
            ByteBuffer vBuffer = vPlane.getBuffer();

            // Calcular las coordenadas del píxel central para los planos U y V
            int uvCenterX = centerX / 2;
            int uvCenterY = centerY / 2;
            int uvIndex = uvCenterY * uPlane.getRowStride() + uvCenterX * uPlane.getPixelStride();

            // Obtener los valores U y V del píxel central
            int u = uBuffer.get(uvIndex) & 0xFF;
            int v = vBuffer.get(uvIndex) & 0xFF;

            // Convertir los valores YUV a RGB
            int rgb = yuvToRgb(y, u, v);

            // Obtener los componentes de color individuales
            int rojo = Color.red(rgb);
            int verde = Color.green(rgb);
            int azul = Color.blue(rgb);

            //Guarda en la variable global el pixel visible actual
            centralPixelColor = Color.rgb(rojo, verde, azul);

            runOnUiThread(() -> {
                GradientDrawable drawable = (GradientDrawable) burbuja.getBackground();
                drawable.setColor(centralPixelColor);
            });
        }
    }

    //Transformar color YUV a RGB
    private int yuvToRgb(int y, int u, int v) {
        int r = y + (int) (1.370705 * (v - 128));
        int g = y - (int) (0.698001 * (v - 128)) - (int) (0.337633 * (u - 128));
        int b = y + (int) (1.732446 * (u - 128));

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return Color.rgb(r, g, b);
    }


}