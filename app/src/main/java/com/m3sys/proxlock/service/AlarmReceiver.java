package com.m3sys.proxlock.service;

/**
 Proximity-Lock
 Copyright (C) 2014  Marc W. Aitkin

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.m3sys.proxlock.MainActivity;


/*
    Set's then waits for an alarm, then launches the Service which does the locking
 */
public class AlarmReceiver extends BroadcastReceiver {

    private final int MINUTES = 1;
    private static final String TAG = MainActivity.BASE_TAG + "-AlarmReceiver";


    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "received alarm");
        context.startService(new Intent(context,ProxSecService.class));
    }

    /*
        Called from AutoStart on boot, and from MainActivity
     */
    public void SetAlarm(Context context)
    {
        Log.d(TAG,"alarm.SetAlarm");
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * MINUTES, pi); // Millisec * Second * Minute
    }

    /*
        Currently not used
     */
    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }






}
