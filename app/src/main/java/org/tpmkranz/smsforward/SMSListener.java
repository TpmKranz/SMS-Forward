package org.tpmkranz.smsforward;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SMSListener extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        ArrayList<String> msgBodyStrings, msgSourceStrings, msgTimestamps;
        msgBodyStrings = new ArrayList<>();
        msgSourceStrings = new ArrayList<>();
        msgTimestamps = new ArrayList<>();

        if (extras != null){
            Object[] smsExtras = (Object[]) extras.get("pdus");
            for (int i = 0; i < smsExtras.length; i++){
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) smsExtras[i]);
                msgBodyStrings.add(smsMessage.getMessageBody());
                msgSourceStrings.add(smsMessage.getOriginatingAddress());
                msgTimestamps.add(String.valueOf(smsMessage.getTimestampMillis()));
            }
        }

        StringBuilder emailBody = new StringBuilder();
        String template = context.getResources().getString(R.string.email_body_template);
        for (int j = 0; j < msgBodyStrings.size(); j++){
            emailBody.append(String.format(template, msgSourceStrings.get(j), msgBodyStrings.get(j)));
            if (j < msgBodyStrings.size()-1)
                emailBody.append(String.format("%n%n%n"));
        }

        Intent emailSendIntent = new Intent(context, EmailSenderService.class);
        emailSendIntent.putExtra(EmailSenderService.INTENTEXTRASUBJECT,
                String.format(context.getResources().getString(R.string.email_subject_template), msgBodyStrings.size()));
        emailSendIntent.putExtra(EmailSenderService.INTENTEXTRABODY, emailBody.toString());
        context.startService(emailSendIntent);
    }
}