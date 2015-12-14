package no.uio.dag.arduinoCommunication.Util;

public class MyMessage{
    String message;
    public MyMessage(String message){
        this.message=message;
    }
    public void setMessage(String message){
        this.message=message;
    }
    public String getMessage(){
        return message;
    }
}
