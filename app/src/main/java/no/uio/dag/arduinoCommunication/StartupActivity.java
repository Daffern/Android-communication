package no.uio.dag.arduinoCommunication;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.ArrayList;
import java.util.List;

import no.uio.dag.R;
import no.uio.dag.arduinoCommunication.Usb.UsbAccessoryManager;


public class StartupActivity extends AppCompatActivity {


    UsbManager usbManager;
    UsbAccessoryManager usbAccessoryManager;
    List<UsbSerialPort> portList = new ArrayList<>();

    public static final int MESSAGE_REFRESH=0;
    public static final int MESSAGE_REFRESH_DELAY=2000;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final static String TAG="StartupActivity";

    ProgressBar progressBar;
    Button accessoryButton;
    Button hostButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        usbAccessoryManager = new UsbAccessoryManager(this);

        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        accessoryButton = (Button)findViewById(R.id.accessory_button);
        hostButton = (Button)findViewById(R.id.host_button);




    }



    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }
    private void hideProgressBar() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void refreshDevices(){
        showProgressBar();
        new AsyncTask<Void, Void, List<UsbSerialPort>>() {
            @Override
            protected List<UsbSerialPort> doInBackground(Void... params) {
                SystemClock.sleep(500);//animation
                List<UsbSerialDriver> driverList =  UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

                if (driverList.size() == 0) {
                    ProbeTable customTable = new ProbeTable();
                    customTable.addProduct(10755, 68, CdcAcmSerialDriver.class);//native port due
                    customTable.addProduct(10755, 61, CdcAcmSerialDriver.class);//programming port due
                    customTable.addProduct(9025, 62, CdcAcmSerialDriver.class);//mega adk
                    customTable.addProduct(10755, 67, CdcAcmSerialDriver.class);//arduino uno

                    UsbSerialProber prober = new UsbSerialProber(customTable);
                    driverList.addAll(prober.findAllDrivers(usbManager));

                }

                List<UsbSerialPort> portList = new ArrayList<UsbSerialPort>();
                for (final UsbSerialDriver driver : driverList) {
                    portList.addAll(driver.getPorts());
                }
                return portList;

            }
                @Override
                protected void onPostExecute(List<UsbSerialPort> result) {
                    //display the usb driver/port for host
                    portList.clear();
                    portList.addAll(result);

                    if (portList.size() == 1){
                        UsbSerialDriver driver = portList.get(0).getDriver();
                        UsbDevice device = driver.getDevice();

                        String string = driver.getClass().getSimpleName()+"\n "+
                                    device.getDeviceName()+" "+device.getVendorId();

                        hostButton.setText( string );
                    }else if (portList.size() > 1){
                        hostButton.setText("Found "+portList.size() + " ports");
                    }else{
                        hostButton.setText(getString(R.string.accessory_disconnected));
                    }

                    //display the accessory device
                    UsbAccessory usbAccessory = usbAccessoryManager.getAccessory();
                    if (usbAccessory == null){
                        accessoryButton.setText(getString(R.string.device_disconnected));
                    }else{

                        accessoryButton.setText(usbAccessory.toString());
                    }

                    hideProgressBar();
                    Log.d(TAG, "Done refreshing, " + portList.size() + " entries found.");
                }

        }.execute((Void) null);
    }

    @Override
    protected void onResume(){
        refreshHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, MESSAGE_REFRESH_DELAY);
        super.onResume();
    }
    @Override
    protected void onPause(){
        refreshHandler.removeMessages(MESSAGE_REFRESH);
        super.onPause();
    }

    private final Handler refreshHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_REFRESH:
                    refreshDevices();
                    refreshHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, MESSAGE_REFRESH_DELAY);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

    };

    //called from the view
    public void onAccessoryButtonClick(View view){

    }

    //called from the view
    public void onHostButtonClick(View view){
        if (portList.size() == 0){
            Toast.makeText(this,"No accessory found", Toast.LENGTH_SHORT);
        }else if (portList.size() == 1) {

            PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            registerReceiver(usbPermissionReceiver, filter);

            UsbSerialPort port = portList.get(0);//TODO make it possible to have several ports

            if (!usbManager.hasPermission(port.getDriver().getDevice()))
                usbManager.requestPermission(port.getDriver().getDevice(), mPermissionIntent);
            else
                ChatActivity.show(this,0);
        }
    }


    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){



                            ChatActivity.show(context, 0);
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

}
