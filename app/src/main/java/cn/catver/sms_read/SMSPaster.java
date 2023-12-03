package cn.catver.sms_read;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SMSPaster {
    public static SMSMessage getSMSMessage(Intent intent){
        StringBuilder bodyMsg = new StringBuilder();
        StringBuilder addrMsg = new StringBuilder();
        Bundle bundle = intent.getExtras();
        Object message[] = (Object[]) bundle.get("pdus");

        SmsMessage[] smsMessage = new SmsMessage[message.length];
        for (int i = 0; i < message.length; i++) {
            smsMessage[i] = SmsMessage.createFromPdu(((byte[]) message[i]));
            bodyMsg.append(smsMessage[i].getDisplayMessageBody());
        }

        addrMsg.append(SmsMessage.createFromPdu(((byte[]) message[0])).getDisplayOriginatingAddress());

        return new SMSMessage(bodyMsg.toString(),
                addrMsg.toString());

    }
}
