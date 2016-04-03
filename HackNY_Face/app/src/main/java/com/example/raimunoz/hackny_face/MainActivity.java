package com.example.raimunoz.hackny_face;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.widget.EditText;
import android.widget.ImageView;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android.telephony.SmsManager;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.widget.Toast;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.clarifai.api.exception.ClarifaiException;
import com.clarifai.api.ClarifaiClient;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.text.DateFormat;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private int number=0;
    //TextView myLabel;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    private static final String TAG = MainActivity.class.getSimpleName();

    public static int CAMERA_REQUEST=9868;
    private SurfaceView face_pic;
    private SurfaceHolder surfaceholder;
    Camera camera;

    private Button picbutton;
    //private TextView Results;
    private Bitmap cameraImage;

    private Intent i;

    private String phonenum="12013498325";

    private String[] ids = {"face","people","man","woman","facial expression","adult","facial expression","boy"};



    private String[] ids_a= new String[20];

    private static final int CODE_PICK = 1;

    private static int count=0;

    private static int ids_index=0;

    private final ClarifaiClient client = new ClarifaiClient(Credentials.CLIENT_ID,
            Credentials.CLIENT_SECRET);

    Camera.PictureCallback jpegCallback;

    @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        face_pic = (SurfaceView)findViewById(R.id.face_pic);
        surfaceholder = face_pic.getHolder();
        surfaceholder.addCallback(this);
        surfaceholder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

       setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        //camera.setDisplayOrientation(90);
        //face_pic = (ImageView) findViewById(R.id.faceView);
        //Results = (TextView) findViewById(R.id.Result);
        picbutton = (Button) findViewById(R.id.picbutton);

        jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(String.format("/sdcard/Person.jpg"));
                    outStream.write(data);
                    outStream.close();
                    Log.d("Log", "onPictureTaken - wrote bytes: " + data.length);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                }
                //Toast.makeText(getApplicationContext(), "Picture Saved", 2000).show();
                //refreshCamera();
            }


        };



        picbutton.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View view) {
                    Log.e(TAG, "The App Started!");

                     findBT();
                     try {
                         openBT();
                     } catch (IOException e) {
                         e.printStackTrace();
                     }


                     //Intent picIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
               // startActivityForResult(picIntent, CAMERA_REQUEST)
               if(count==1) {
                   camera.takePicture(null, null, jpegCallback);
                   Log.e(TAG, "Image capture Successful");
                   BitmapFactory.Options options = new BitmapFactory.Options();
                   options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                   Bitmap cameraImage = BitmapFactory.decodeFile("/sdcard/Person.jpg", options);
                   Log.e(TAG, "Camera Bitmap successful");
                   starting(cameraImage);
               }
                //refreshCamera();
            }
        });
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            //myLabel.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("MAKECU"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
       // myLabel.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        //receiving.setText("Hello");
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();
        //Intent i=  new Intent(this, MainActivity.class);
        //startActivity(i);
        //myLabel.setText("Bluetooth Opened");
        //count=0;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        //receiving.setText("Hello");
        if(count==1) {
            beginListenForData();
            //receiving.setText(number);
            number++;
        }
        else
        {
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        //receiving.setText("Goodbye");
        //closeBT();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        //receiving.setText("Goodbye");
        //closeBT();
        //beginListenForData();
    }

    void beginListenForData()
    {

        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            //myLabel.setText(data);
                                            //receiving.setText(data);
                                            Log.e(TAG, data);
                                            count=1;

                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public void refreshCamera() {
        if (surfaceholder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceholder);
            camera.startPreview();
        } catch (Exception e) {

        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        refreshCamera();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // open the camera
            camera = Camera.open();
            camera.setDisplayOrientation(90);
        } catch (RuntimeException e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
        android.hardware.Camera.Parameters param;
        param = camera.getParameters();

        // modify parameter
        param.setPreviewSize(352, 288);
        camera.setParameters(param);
        try {
            // The Surface has been created, now tell the camera where to draw
            // the preview.
            camera.setPreviewDisplay(surfaceholder);
            camera.startPreview();
        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // stop preview and release camera
        camera.stopPreview();
        camera.release();
        camera = null;
    }




    protected void starting(Bitmap cameraImage) {
        //super.onActivityResult(requestCode, resultCode, data);
        //if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Log.e(TAG, "The image has been received");
            //cameraImage = (Bitmap) data.getExtras().get("data");

            //face_pic.setImageBitmap(cameraImage);

            if (cameraImage != null) {
                Log.e(TAG, "The image has been received");
                //face_pic.setImageBitmap(cameraImage);
                //textView.setText("Recognizing...");
                picbutton.setEnabled(false);
                //ByteArrayOutputStream bs = new ByteArrayOutputStream();
                //cameraImage.compress(Bitmap.CompressFormat.JPEG, 90, bs);


                // Run recognition on a background thread since it makes a network call.
                new AsyncTask<Bitmap, Void, RecognitionResult>() {
                    @Override
                    protected RecognitionResult doInBackground(Bitmap... bitmaps) {
                        return recognizeBitmap(bitmaps[0]);
                    }

                    @Override
                    protected void onPostExecute(RecognitionResult result) {
                        updateUIForResult(result);
                    }
                }.execute(cameraImage);
            } else {
                Log.e(TAG, "The image has not been received");
                //Results.setText("Unable to load selected image.");
            }
    }


    private RecognitionResult recognizeBitmap(Bitmap bitmap) {
        try {
            // Scale down the image. This step is optional. However, sending large images over the
            // network is slow and  does not significantly improve recognition performance.
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 320,
                    320 * bitmap.getHeight() / bitmap.getWidth(), true);

            // Compress the image as a JPEG.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out);
            byte[] jpeg = out.toByteArray();

            // Send the JPEG to Clarifai and return the result.
            return client.recognize(new RecognitionRequest(jpeg)).get(0);
        } catch (ClarifaiException e) {
            Log.e(TAG, "Clarifai error", e);
            return null;
        }
    }

    /** Updates the UI by displaying tags for the given result. */
    private void updateUIForResult(RecognitionResult result) {
        Log.e(TAG,"HEY, I'm WORKING! ");
        if (result != null) {
            if (result.getStatusCode() == RecognitionResult.StatusCode.OK) {
                // Display the list of tags in the UI.
                Log.e(TAG, result.getStatusCode().toString());

                StringBuilder b = new StringBuilder();

                for (Tag tag : result.getTags()) {
                    b.append(b.length() > 0 ? ", " : "").append(tag.getName());

                    ids_a[ids_index]=tag.getName();
                    ids_index++;
                }

                for(int j=0;j<ids_a.length;j++) {
                    for (int i = 0; i < ids.length; i++) {
                        if (ids_a[j].equals(ids[i]) && count == 0){
                            count = 1;
                        }else if (count == 0) {
                            Log.e(TAG, "AW, no face");
                        } else{
                        }
                    }
                }

                //Intent i = new Intent(this, Recognition.class);
                //i.putExtra("byteArray", bs.toByteArray());
                //startActivityForResult(i,2);

                if (count == 1) {
                    Log.e(TAG, "Hey there's someone at the door at: ");
                    Log.e(TAG, DateFormat.getDateTimeInstance().format(new Date()));

                    SmsManager smsManager = SmsManager.getDefault();
                    String message="Hey there's someone at the door at: "+DateFormat.getDateTimeInstance().format(new Date());//+" with the following tags"+b;

                    smsManager.sendTextMessage(phonenum, null, message, null, null);
                    smsManager.sendTextMessage(phonenum, null, b.toString(), null, null);



                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    //sendIntent.setClassName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity");
                    sendIntent.setPackage("com.android.mms");
                    sendIntent.putExtra("address", phonenum);
                    sendIntent.putExtra("sms_body",message);

/* Adding The Attach */
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/Person.jpg"));
                    sendIntent.setType("image/jpg");

                    sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(sendIntent,"Send");
                }
                else
                {
                    Log.e(TAG,"No ones there!");
                }


                Log.e(TAG,"Tags:\n" + b);


            } else {
                Log.e(TAG, "Clarifai: " + result.getStatusMessage());
                //Results.setText("Sorry, there was an error recognizing your image.");
                Log.e(TAG, "Sorry, there was an error recognizing your image.");
            }
        } else {
            //Results.setText("Sorry, there was an error recognizing your image.");
        }
        //selectButton.setEnabled(true);
    }


}
