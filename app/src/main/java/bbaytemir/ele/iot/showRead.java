package bbaytemir.ele.iot;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class showRead extends AppCompatActivity {
    private static final String TAG = "bluetooth2";

    TextView txtArduino,txt1,txt2,txt3,txt4,txt5,txt6,txt7,txt8,txt9;
    ImageView imgRGB;
    ImageView imgECO;
    ImageView imgTVOC;

    Handler h;

    final int RECIEVE_MESSAGE = 1;        // Status  for Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();
    private ArrayList<BluetoothDevice> mDeviceList;

    private ConnectedThread mConnectedThread;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    TabHost tabHost;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){
            Log.e("showRead",e.getMessage());
        }

        setContentView(R.layout.activity_show_read);

         tabHost = (TabHost)findViewById(android.R.id.tabhost);

        tabHost.setup();

        TabHost.TabSpec spec;

        spec = tabHost.newTabSpec("0");
        spec .setIndicator("Color");
        spec.setContent(R.id.Renk);
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("1").setContent(R.id.HavaKalitesi);
        spec .setIndicator("Air Quality");
        tabHost.addTab(spec);



        tabHost.setCurrentTab(0);

        txtArduino = (TextView) findViewById(R.id.txtAll);
        txt1 = (TextView) findViewById(R.id.txt1);
        txt2 = (TextView) findViewById(R.id.txt2);
        txt3 = (TextView) findViewById(R.id.txt3);
        txt4 = (TextView) findViewById(R.id.txtRGB);
        imgRGB = (ImageView) findViewById(R.id.imgRGB);
        imgECO = (ImageView) findViewById(R.id.imgAir_ECO);
        imgTVOC = (ImageView) findViewById(R.id.imgAir_TVOC);

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:													// if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);					// create string from bytes array
                        sb.append(strIncom);												// append string
                        int endOfLineIndex = sb.indexOf("\r\n");							// determine the end-of-line
                        if (endOfLineIndex > 0) { 											// if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);				// extract string
                            sb.delete(0, sb.length());										// and clear
                            String fir,las;

                            fir = sbprint.substring(0,1);
                            las = sbprint.substring(sbprint.length()-1);
                            //txtArduino.setText("fir las: " + fir + las); 	        // update TextView
                            Log.d("message",sbprint);
                            if(fir.equals("#") && las.equals("$")) {
                                String[] datas = sbprint.substring(1,sbprint.length()-1).split(",");
                                if(datas.length==6) {
                                    txtArduino.setText("Data from Arduino: " + sbprint);            // update TextView

                                    txt1.setText("ECO2 : " + datas[0]);
                                    txt2.setText("TVOC : " + datas[1]);
                                    txt3.setText("Temprature : " + datas[2]);

                                    try {
                                        txt4.setText("#" + Integer.toHexString(Integer.decode(datas[3])) + Integer.toHexString(Integer.decode(datas[4])) + Integer.toHexString(Integer.decode(datas[5])));
                                        imgRGB.setBackgroundColor(Color.parseColor("#" + Integer.toHexString(Integer.decode(datas[3])) + Integer.toHexString(Integer.decode(datas[4])) + Integer.toHexString(Integer.decode(datas[5]))));
                                    }catch (Exception e){
                                        Log.e("Bluetooth err",e.getMessage());
                                    }
                                    try {
                                        String color_eco = "cccccc";
                                        if(Integer.decode(datas[0])<1000)
                                            color_eco = "5cb85c";
                                        else if(Integer.decode(datas[0])<2000)
                                            color_eco = "5bc0de";
                                        else
                                            color_eco = "d9534f";
                                        String color_tvoc = "cccccc";
                                        if(Integer.decode(datas[1])<150)
                                            color_tvoc = "5cb85c";
                                        else if(Integer.decode(datas[1])<300)
                                            color_tvoc = "5bc0de";
                                        else
                                            color_tvoc = "d9534f";
                                        imgECO.setBackgroundColor(Color.parseColor("#" + color_eco));
                                        imgTVOC.setBackgroundColor(Color.parseColor("#" + color_tvoc));
                                    }catch (Exception e){
                                        Log.e("Bluetooth err",e.getMessage());
                                    }
                                }
                                /*

    <!--

    Serial.print(hr);
    Serial.print(",");
    Serial.print(spo2);
    Serial.print(",");
    Serial.print(eco2);
    Serial.print(",");
    Serial.print(tvoc);
    Serial.print(",");
    Serial.print(t);
    Serial.print(",");
    Serial.print(hum);
    -->

 */
                            }
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            };
        };


        mDeviceList = getIntent().getExtras().getParcelableArrayList("device.list");
        if (mDeviceList == null) {
            Toast.makeText(this, "No device", Toast.LENGTH_SHORT).show();
            finish();
        }
        for (int i = 0; i < mDeviceList.size(); i++) {


            Log.d(TAG, "...onResume - try connect...");

            // Set up a pointer to the remote node using it's address.
            BluetoothDevice device = mDeviceList.get(i);

            // Two things are needed to make a connection:
            //   A MAC address, which we got above.
            //   A Service ID or UUID.  In this case we are using the
            //     UUID for SPP.

            try {
                btSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                Log.e("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
            }

    /*try {
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
    }*/

            // Establish the connection.  This will block until it connects.
            Log.d(TAG, "...Connecting...");
            try {
                btSocket.connect();
                Log.d(TAG, "....Connection ok...");
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.e("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                }
            }

            // Create a data stream so we can talk to server.
            Log.d(TAG, "...Create Socket...");

            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();

        }
    }

    float lastX;

    @Override
    public boolean onTouchEvent(MotionEvent touchevent) {
        switch (touchevent.getAction()) {
            // when user first touches the screen to swap
            case MotionEvent.ACTION_DOWN: {
                lastX = touchevent.getX();
                break;
            }
            case MotionEvent.ACTION_UP: {
                float currentX = touchevent.getX();

                // if left to right swipe on screen
                if (lastX < currentX) {

                    switchTabs(false);
                }

                // if right to left swipe on screen
                if (lastX > currentX) {
                    switchTabs(true);
                }

                break;
            }
        }
        return false;
    }

    public void switchTabs(boolean direction) {
        if (direction) // true = move left
        {
            if (tabHost.getCurrentTab() == 0)
                tabHost.setCurrentTab(tabHost.getTabWidget().getTabCount() - 1);
            else
                tabHost.setCurrentTab(tabHost.getCurrentTab() - 1);
        } else
        // move right
        {
            if (tabHost.getCurrentTab() != (tabHost.getTabWidget()
                    .getTabCount() - 1))
                tabHost.setCurrentTab(tabHost.getCurrentTab() + 1);
            else
                tabHost.setCurrentTab(0);
        }
    }
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"

                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();        // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }
        /*
        public void receiveData(BluetoothSocketWrapper socket) throws IOException{
            InputStream socketInputStream =  socket.getInputStream();
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = socketInputStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    Log.i("logging", readMessage + "");
                } catch (IOException e) {
                    break;
                }
            }
        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }
}
