package cn.catver.sms_read;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SMSRecv extends BroadcastReceiver {
    public static final String TAG = "SMSRecv";
    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.i(TAG, "onReceive: a sms message");
        SMSMessage msg = SMSPaster.getSMSMessage(intent);
        Log.i(TAG, String.format("%s send to you: %s", msg.getAddress(),msg.getBody()));
        for (String s : MainActivity.NUMBERS) {
            if(msg.Address.contains(s)){
                Log.i(TAG, "onReceive: GO");
                MainActivity.PlayAlert();
                break;
            }
        }
    }
}
