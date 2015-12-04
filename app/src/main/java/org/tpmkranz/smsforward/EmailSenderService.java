package org.tpmkranz.smsforward;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSenderService extends Service {
    NotificationManager mManager;
    Service mContext;
    Resources mRes;
    SharedPreferences settings;
    int mNotificationOffset;
    protected static String LOGCATTAG = "EmailSenderService";
    protected static String INTENTEXTRABODY = "emailBody";
    protected static String INTENTEXTRASUBJECT = "emailSubject";

    public EmailSenderService() {
    }

    @Override
    public void onCreate(){
        super.onCreate();
        mContext = this;
        mRes = getResources();
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
            JavaMailProperties.populateDefaultPropPreferences(settings, mRes);
            populatePropertiesFromPreferences(props, settings);
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
                String text = params[1]+"\n";
                PGPPubkeyEncryptionUtil encrypter = new PGPPubkeyEncryptionUtil(settings.getString(SetupActivity.SHAREDPREFSPUBKEY, ""));
                if (encrypter.hasKey())
                    text = encrypter.encrypt(text);
                else
                    encrypter = null;
                message.setText(text);
                Transport.send(message);
            }catch (Exception e){
                Log.e(LOGCATTAG, e.toString());
                return false;
            }
            return true;
        }

        private void populatePropertiesFromPreferences(Properties properties, SharedPreferences prefs) throws IllegalStateException{
            int count = prefs.getInt(SetupActivity.SHAREDPREFSJAVAMAILCOUNT, 0);
            if (count == 0){
                throw new IllegalStateException("No properties stored in preferences");
            }
            for (int i = 0; i < count; i++){
                String key = prefs.getString(SetupActivity.SHAREDPREFSJAVAMAILKEY + String.valueOf(i), "");
                String value = prefs.getString(SetupActivity.SHAREDPREFSJAVAMAILVALUE + String.valueOf(i), "");
                switch (value){
                    case "$PORT":
                        value = prefs.getString(SetupActivity.SHAREDPREFSPORT, "");
                        break;
                    case "$SERVER":
                        value = prefs.getString(SetupActivity.SHAREDPREFSSERVER, "");
                        break;
                    case "$TARGET":
                        value = prefs.getString(SetupActivity.SHAREDPREFSTARGET, "");
                        break;
                    case "$EMAIL":
                        value = prefs.getString(SetupActivity.SHAREDPREFSEMAIL, "");
                        break;
                    default:
                        break;
                }
                properties.setProperty(key, value);
            }
        }

        @Override
        protected void onPostExecute(Boolean success){
            mManager.cancel(mNotificationId);
            mContext.stopForeground(true);
        }

    }

}
