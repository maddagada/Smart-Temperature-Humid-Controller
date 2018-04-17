package smartmetawear.smarttemperaturehumidcontroller;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;

import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Temperature.SensorType;

import bolts.Continuation;
import bolts.Task;

import com.mbientlab.metawear.android.BtleService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;
import android.support.v4.app.NotificationCompat.Builder;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private BtleService.LocalBinder serviceBinder;

    //Storing MAC address of MetaTracker, final key word will prevent this variable from updating from getting updated
    private final String MW_MAC_ADDRESS= "D9:FE:65:69:EE:2F";
    static MetaWearBoard mwBoard;

    ///Overriding onCreate method from the base class
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ///< Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);
        ///Adding listener for the button click
        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readtemp();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }
    //Override method for onServiceConnected
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceBinder = (BtleService.LocalBinder) service;
        Log.i("Temperature Monitor", "Service connected");
        /// Method to connect to the bluetooth
        connectBLE();
    }

    /// connectBLE method connects device using the MAC address of the MetaTracker
    private void connectBLE() {

        ///Creates an manager object for the android bluetooth service
        final BluetoothManager btManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        // Using the BluetoothManager it connects to the device with MAC Address defined in the variable (MW_MAC_ADDRESS).
        final BluetoothDevice remoteDevice =
                btManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);

        // Create a MetaWear board object for the Bluetooth Device
        mwBoard = serviceBinder.getMetaWearBoard(remoteDevice);
        try {
            Task<Void> task = mwBoard.connectAsync();
            task.waitForCompletion();

            if (task.isFaulted()) {
                //throw task.getError();
            }
        }
        catch (InterruptedException e){}




        Log.i("Temeperature monitor", "Service Connected to ... " + MW_MAC_ADDRESS);

    }
    @Override
    public void onServiceDisconnected(ComponentName componentName) { }


    /// A method to read temperature from the Meta tracker
    public void readtemp()
    {
        //final Random r = new Random();
        //tmpDisplay.append("Temp reading at  "+ dateFormat.format(date) + x.replace('.',' ') +"\n" );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("1", "test", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Channel");
            // Register the channel with the system
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "1")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Hello")
                    .setContentText("Temp")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            //mBuilder.notify();
            notificationManager.notify(1,mBuilder.build());
        }



        //Creating the temperature module object from the Meta board
        Temperature temp = mwBoard.getModule(Temperature.class);

        //Logging number of temperature sensors
        Log.i("Tempsens", String.valueOf(temp.sensors().length));

        //getting the PRESET_THERMISTOR sensor. [0] gets the first PRESET_THERMISTOR sensor from the list
        final Temperature.Sensor tempSensor = temp.findSensors(SensorType.PRESET_THERMISTOR)[0];

        ///Creating a route Asynchronously to the PRESET_THERMISTOR
        tempSensor.addRouteAsync(new RouteBuilder() {
            @Override

            //Configuring the PRESET_THERMISTOR sensor
            public void configure(RouteComponent source) {

                //Streams the Temperature data
                source.stream(new Subscriber() {

                    //Method to deal with the streamed data
                    @Override
                    public void apply(Data data, Object... env) {

                        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        Date date = new Date();

                        //Getting the object TmpDisplay of type TextView
                        TextView tmpDisplay = (TextView) findViewById(R.id.TmpDisplay);
                        //tmpDisplay.append("STarting \n"  );
                        String x = "CurrentTemperature (C) = " + data.value(Float.class);
                        //Log.i("tempsens", "Temperature1987 (C) = " + data.value(Float.class));
                        Log.i("tempsens", x );

                        //tmpDisplay.setText("Temp is "+ String.valueOf( r.nextFloat())  + data.value(Float.class));
                        tmpDisplay.append("Temp reading at  "+ dateFormat.format(date) + x.replace('.',' ') +"\n" );
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            NotificationChannel channel = new NotificationChannel("1", "test", NotificationManager.IMPORTANCE_DEFAULT);
                            channel.setDescription("Channel");
                            // Register the channel with the system
                            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MainActivity.this, "1")
                                    .setSmallIcon(R.drawable.ic_launcher_background)
                                    .setContentTitle("Hello")
                                    .setContentText("Temp")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            //mBuilder.notify();
                            notificationManager.notify(1,mBuilder.build());
                        }
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                tempSensor.read();
                return null;
            }
        });

    }

}