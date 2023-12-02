package cn.catver.sms_read;

import androidx.annotation.NonNull;

public class SMSMessage {
    String Body;
    String Address;

    public SMSMessage(String b,String address){
        Body = b;
        Address = address;
    }

    public String getBody() {
        return Body;
    }

    public String getAddress() {
        return Address;
    }

    public SMSMessage clone(){
        return new SMSMessage(Body,Address);
    }

    @NonNull
    @Override
    public String toString() {
        return Address+": "+Body;
    }
}
