package no.uio.dag.arduinoCommunication.Usb;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import no.uio.dag.arduinoCommunication.Chat.OneComment;
import no.uio.dag.arduinoCommunication.Util.MessageListener;
import no.uio.dag.arduinoCommunication.Util.MyMessage;

/**
 * Created by Daffern on 13.12.2015.
 */
public class UsbHostManager implements  MyUsbManager{

    private final String TAG="USBHOSTMANAGER";

    private Context context;
    private UsbManager usbManager;
    private SerialInputOutputManager serialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private UsbSerialPort activeUsbSerialPort;

    SerialInputOutputManager.Listener ioListener;
    MessageListener myListener;

    public UsbHostManager(Context context){
        this.context = context;

        usbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);


        ioListener = new SerialInputOutputManager.Listener(){
            @Override
            public void onNewData(byte[] data) {
                myListener.onReceive(data);
            }
            @Override
            public void onRunError(Exception e) {
                Log.e(TAG, e.getMessage());
                myListener.onRunError(e);

            }
        };
    }

    public boolean startService(int portnumber){

        List<UsbSerialPort> ports = getUsbSerialPorts();
        if (ports.size() <= portnumber)
            return false;

        activeUsbSerialPort = ports.get(portnumber);

        openPort(activeUsbSerialPort);

        if (activeUsbSerialPort != null) {
            Log.i(TAG, "Starting io manager ..");

            serialIoManager = new SerialInputOutputManager(activeUsbSerialPort, ioListener);
            mExecutor.submit(serialIoManager);

        }
        return true;
    }


    private void stopIoManager() {
        if (serialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            serialIoManager.stop();
            serialIoManager = null;
        }
    }
    @Override
    public void setMessageListener(MessageListener messageListener) {
        this.myListener = messageListener;
    }

    @Override
    public void sendMessage(byte[] data) {
        try {
            activeUsbSerialPort.write(data, 300);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public boolean isConnected() {
        return false;
    }

    public List<UsbSerialPort> getUsbSerialPorts(){
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



    public boolean openPort(UsbSerialPort usbSerialPort){
        UsbDeviceConnection connection = usbManager.openDevice(usbSerialPort.getDriver().getDevice());

        if (connection == null) {
            Log.e(TAG, "connection was null");
            return false;
        }

        try {

            usbSerialPort.open(connection);
            usbSerialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        } catch (IOException e) {
            Log.e(TAG, "Error setting up device: " + e.getMessage(), e);

            try {
                usbSerialPort.close();
            } catch (IOException e2) {
                // Ignore.
            }
            return false;
        }
        return true;
    }




}
