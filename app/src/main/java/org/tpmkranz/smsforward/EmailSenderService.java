package org.tpmkranz.smsforward;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSenderService extends Service {
    NotificationManager mManager;
    Service mContext;
    SharedPreferences settings;
    int mNotificationOffset;
    public static String INTENTEXTRABODY = "emailBody";
    public static String INTENTEXTRASUBJECT = "emailSubject";

    public EmailSenderService() {
    }

    @Override
    public void onCreate(){
        super.onCreate();
        mContext = this;
        mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        settings = getSharedPreferences(SetupActivity.SHAREDPREFSNAME, Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if (intent != null) {
            String emailBody = intent.getStringExtra(INTENTEXTRABODY);
            String emailSubject = intent.getStringExtra(INTENTEXTRASUBJECT);
            if (emailBody != null && emailSubject != null) {
                SendMailTask sendMailTask = new SendMailTask();
                sendMailTask.execute(emailSubject, emailBody);
                return START_STICKY;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    private class SendMailTask extends AsyncTask<String, Integer, Boolean>{
        Notification notification;
        int mNotificationId;

        @Override
        protected void onPreExecute(){
            String notificationText = mContext.getResources().getString(R.string.service_notification_ticker);
            String notificationLabel = mContext.getResources().getString(R.string.app_name);
            mNotificationId = R.string.app_name + mNotificationOffset++;
            notification = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(notificationText)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(notificationLabel)
                .setContentText(notificationText)
                .build();
            mManager.notify(mNotificationId, notification);
            mContext.startForeground(mNotificationId, notification);
        }

        @Override
        protected Boolean doInBackground(String... params) {

            final String username = settings.getString(SetupActivity.SHAREDPREFSEMAIL, "");
            final String password = settings.getString(SetupActivity.SHAREDPREFSPASSWORD, "");
            final String target = settings.getString(SetupActivity.SHAREDPREFSTARGET, "");

            Properties props = new Properties();
            props.setProperty("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.socketFactory.port", settings.getString(SetupActivity.SHAREDPREFSPORT, ""));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.host", settings.getString(SetupActivity.SHAREDPREFSSERVER, ""));
            props.put("mail.smtp.port", settings.getString(SetupActivity.SHAREDPREFSPORT, ""));
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.setProperty("mail.smtp.quitwait", "false");
            Session sesh = Session.getInstance(props, new javax.mail.Authenticator(){
                protected javax.mail.PasswordAuthentication getPasswordAuthentication(){

                    return new javax.mail.PasswordAuthentication(username,password);
                }
            });
            try {
                Message message = new MimeMessage(sesh);
                message.setFrom(new InternetAddress(username));
                message.setRecipient(Message.RecipientType.TO, InternetAddress.parse(target)[0]);
                message.setSubject(params[0]);
                message.setText(params[1]);
                Transport.send(message);
            }catch (MessagingException e){
                Log.e(mContext.getResources().getString(R.string.app_name), e.toString());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success){
            mManager.cancel(mNotificationId);
            mContext.stopForeground(true);
        }
    }
}
