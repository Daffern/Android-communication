package no.uio.dag.arduinoCommunication.Util;

/**
 * Created by Daff on 28.09.2015.
 */
public interface MessageListener {

    void onReceive(byte[] data);

    void onRunError(Exception e);

}
