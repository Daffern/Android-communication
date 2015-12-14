package no.uio.dag.arduinoCommunication.Usb;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import no.uio.dag.arduinoCommunication.Util.MessageListener;
import no.uio.dag.arduinoCommunication.Util.MyMessage;

/**
 * Created by Daffern on 12.11.2015.
 */
public interface MyUsbManager {

    void setMessageListener(MessageListener messageListener);
    void sendMessage(byte[] data);
    boolean isConnected();


}
