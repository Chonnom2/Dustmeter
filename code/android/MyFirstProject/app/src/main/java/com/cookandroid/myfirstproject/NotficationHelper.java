package com.cookandroid.myfirstproject;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class NotficationHelper extends ContextWrapper {
   public static final String channel1Id = "channel1ID";
   public static final String channel1Name ="channel1";

   private NotificationManager manager;

   public NotficationHelper(Context base){
       super(base);

       if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O){
           createChannels();
       }
   }

   @RequiresApi(api = Build.VERSION_CODES.O)
   public void createChannels(){

       NotificationChannel channel1 = new NotificationChannel(channel1Id, channel1Name, NotificationManager.IMPORTANCE_DEFAULT);
       channel1.enableLights(true);
       channel1.enableVibration(true);
       channel1.setLightColor(R.color.colorPrimary);
       channel1.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

       getManager().createNotificationChannel(channel1);
   }

   public NotificationManager getManager(){
       if (manager == null){
           manager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
       }
       return manager;
   }

  public NotificationCompat.Builder getChannelNotification(String title, String message){
       return new NotificationCompat.Builder(getApplicationContext(), channel1Id)
               .setContentTitle(title)
               .setContentText(message)
               .setSmallIcon(R.drawable.ic_launcher_background);
  }
}
