package cn.catver.sms_read;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class TelephoneStateRecv extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
        String addr = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        if(addr == null) return;
        for (String s : MainActivity.CALLNUMBERS) {
            if(addr.contains(s)){
                int state = manager.getCallState();
                if(state == TelephonyManager.CALL_STATE_RINGING){
                    MainActivity.PlayAlert();
                }
                if(state == TelephonyManager.CALL_STATE_IDLE || state == TelephonyManager.CALL_STATE_OFFHOOK){
                    MainActivity.StopAlert();
                }
                break;
            }
        }
    }
}
