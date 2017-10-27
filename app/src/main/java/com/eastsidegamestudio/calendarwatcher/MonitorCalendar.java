package com.eastsidegamestudio.calendarwatcher;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Build;

public class MonitorCalendar extends Activity {
	
	private TextView eventTitle;
	private TextView eventDescription;
	
	private LinearLayout layout;
	
	private Timer recurringTimer;
	private Timer sleepTimer;
	private Timer wakeTimer;
	
	Date sleepDate;
	Date wakeDate;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_monitor_calendar);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		System.out.println("Starting MonitorCalendar activity...");
		
		eventTitle = (TextView)findViewById(R.id.eventTitle);
		eventDescription = (TextView)findViewById(R.id.eventDescription);
		layout = (LinearLayout)findViewById(R.id.monitorLayout);
		
		SharedPreferences settings = getSharedPreferences(CalendarWatcher.PREFS_NAME, 0);
		String sleepStr = settings.getString("sleep_time", CalendarWatcher.DEFAULT_SLEEP_TIME);
		String wakeStr = settings.getString("wake_time", CalendarWatcher.DEFAULT_WAKE_TIME);
		
		SimpleDateFormat formatter = new SimpleDateFormat("k:mm");
		
		try {
			sleepDate = formatter.parse(sleepStr);
			wakeDate = formatter.parse(wakeStr);
			
			System.out.println(getNextDate(sleepDate).toString());
			System.out.println(getNextDate(wakeDate).toString());
			
			sleepTimer = new Timer();
			sleepTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					System.out.println("Going to sleep!");
					screenSleep();
				}
			}, getNextDate(sleepDate));
			
			wakeTimer = new Timer();
			wakeTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					System.out.println("Waking up");
					screenWakeUp();
				}
			}, getNextDate(wakeDate));
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		recurringTimer = new Timer();
		recurringTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				AsyncTask<Void, Void, List<Event>> task = new MonitorCalendarTask();
				task.execute();
			}
			
		}, 0, CalendarWatcher.CHECK_INTERVAL * 1000);
		
		
		
		
	}
	
	private Date getNextDate(Date date) {
		Calendar curCalendar = Calendar.getInstance();
		curCalendar.setTime(new Date());
		
		Calendar sourceCalendar = Calendar.getInstance();
		sourceCalendar.setTime(date);
		
		Calendar nextCalendar = Calendar.getInstance();
		nextCalendar.setTime(new Date());
		nextCalendar.set(Calendar.HOUR_OF_DAY, sourceCalendar.get(Calendar.HOUR_OF_DAY));
		nextCalendar.set(Calendar.MINUTE, sourceCalendar.get(Calendar.MINUTE));
		
		// Do we need to add a day?
		if (curCalendar.getTimeInMillis() >= nextCalendar.getTimeInMillis()) {
			nextCalendar.add(Calendar.DATE, 1);
		}
		
		return nextCalendar.getTime();
	}
	
	private void screenWakeUp() {

		
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				WindowManager.LayoutParams params = getWindow().getAttributes();
				
				params.screenBrightness = -1.0f;
				getWindow().setAttributes(params);
				
//				if (screenWakeLock != null && screenWakeLock.isHeld()) {
//					screenWakeLock.release();
//				}
//				
//				screenWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "screenWakeLock");
//				screenWakeLock.acquire();
//				
//				KeyguardManager keyguardManager = (KeyguardManager)getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE); 
//	            KeyguardLock keyguardLock =  keyguardManager.newKeyguardLock("screenWakeLock");
//	            keyguardLock.disableKeyguard();
			}
			
		});
		
	}
	
	private void screenSleep() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
//				screenWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "screenWakeLock");
//				screenWakeLock.acquire();
				
				WindowManager.LayoutParams params = getWindow().getAttributes();
				
				params.screenBrightness = 0.1f;
				getWindow().setAttributes(params);
			}
		});
		
		
	}
	
	@Override
    protected void onResume() {
    	super.onResume();
    	
    	screenWakeUp();
    }
	
	@Override
    protected void onPause() {
    	super.onPause();
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		System.out.println("Destroying MonitorCalendar activity");
		
		recurringTimer.cancel();
		recurringTimer.purge();
		
		sleepTimer.cancel();
		sleepTimer.purge();
		
		wakeTimer.cancel();
		wakeTimer.cancel();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.monitor_calendar, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_logout) {
			new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.logout)
				.setMessage(R.string.really_logout)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(MonitorCalendar.this, MainActivity.class);
						intent.putExtra("reset_user", true);
						intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
						startActivity(intent);
					}
				})
				.setNegativeButton(R.string.no, null)
				.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private List<Event> getActiveEvents(List<Event> events) {
		List<Event> activeEvents = new ArrayList<Event>();
		Iterator<Event> it = events.iterator();
		Event event;
		
		DateTime startDateTime;
		Date testTime = new Date();
		
		while (it.hasNext()) {
			event = it.next();
			startDateTime = event.getStart().getDateTime();
			if (startDateTime == null) {
				// Is this an all-day event?
				startDateTime = event.getStart().getDate();
			}
			if (startDateTime.getValue() < testTime.getTime()) {
				activeEvents.add(event);
			}
		}
		
		return activeEvents;
		
	}
	
	private List<Event> getIncomingEvents(List<Event> events) {
		List<Event> incomingEvents = new ArrayList<Event>();
		Iterator<Event> it = events.iterator();
		Event event;
		
		DateTime startDateTime;
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MINUTE, CalendarWatcher.INCOMING_BUFFER);
		
		Date testTime = cal.getTime();
		
		while (it.hasNext()) {
			event = it.next();
			startDateTime = event.getStart().getDateTime();
			if (startDateTime.getValue() < testTime.getTime()) {
				incomingEvents.add(event);
			}
		}
		
		return incomingEvents;
	}
	
	private class MonitorCalendarTask extends AsyncTask<Void, Void, List<Event>> {

		@Override
		protected List<Event> doInBackground(Void... arg) {
			
			List<Event> events = null;
			int retry = 0;
			
			// Get update from calendar
			try {
				DateTime minTime = new DateTime(new Date());
				
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				cal.add(Calendar.DATE, 1);
				
				DateTime maxTime = new DateTime(cal.getTime());
				Events request;
				
				do {
					request = CalendarWatcher.GOOGLE_CALENDAR.events()
						.list(CalendarWatcher.CALENDAR_SELECTED.getId())
						.setTimeMax(maxTime)
						.setTimeMin(minTime)
						.setOrderBy("startTime")
						.setSingleEvents(true)
						.execute();
					
					events = request.getItems();
				} while (events == null && retry++ < 2);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return events;
		}
		
		private void updateMonitor(Event event) {
			if (event == null) {
				eventTitle.setText("No active events");
				eventDescription.setText("");
			} else {
				eventTitle.setText(event.getSummary());
				
				SimpleDateFormat df = new SimpleDateFormat("h:mma");
				
				DateTime startTime;
				DateTime endTime;
				boolean allDay = false;
				
				startTime = event.getStart().getDateTime();
				if (startTime == null) {
					allDay = true;
					startTime = event.getStart().getDate();
				}
				
				endTime = event.getEnd().getDateTime();
				if (endTime == null) {
					endTime = event.getEnd().getDate();
				}
				
				String description = "";
				
				if (allDay) {
					description += "All day";
				} else {
					description += df.format(new Date(startTime.getValue())).toLowerCase();
					description += " - ";
					description += df.format(new Date(endTime.getValue())).toLowerCase();
				}

				Event.Creator creator = event.getCreator();
				String displayName = creator.getDisplayName();
				if (displayName != null) {
					description += " <b>(" + displayName + ")</b>";
				}
				
				eventDescription.setText(Html.fromHtml(description));
			}
			
		}
		
		protected void onPostExecute(List<Event> events) {
			if (events == null) {
				return;
			}
			
			List<Event> activeEvents = getActiveEvents(events);
			
			if (activeEvents.size() > 0) {
				updateMonitor(activeEvents.get(0));
				layout.setBackgroundColor(getResources().getColor(R.color.background_occupied));
			} else {
				List<Event> incomingEvents = getIncomingEvents(events);
				
				if (incomingEvents.size() > 0) {
					updateMonitor(incomingEvents.get(0));
					layout.setBackgroundColor(getResources().getColor(R.color.background_incoming));
				} else {
					updateMonitor(null);
					layout.setBackgroundColor(getResources().getColor(R.color.background_available));
				}
			}
		}
		
	}

}
