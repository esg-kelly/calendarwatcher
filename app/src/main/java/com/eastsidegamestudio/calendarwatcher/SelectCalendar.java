package com.eastsidegamestudio.calendarwatcher;

import com.google.api.services.calendar.model.CalendarListEntry;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;

public class SelectCalendar extends Activity implements OnItemSelectedListener {
	
	Spinner calendarSpinner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_calendar);
		
		calendarSpinner = (Spinner)findViewById(R.id.calendarSpinner);
		CalendarListAdapter adapter = new CalendarListAdapter(this, CalendarWatcher.CALENDARLIST);
		calendarSpinner.setAdapter(adapter);
		calendarSpinner.setOnItemSelectedListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.select_calendar, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void onItemSelected(AdapterView<?> parent, View view, int position, long key) {
		if (key < -1) {
			System.out.println("Key is invalid");
			return;
		}
		
		CalendarListEntry calendar = (CalendarListEntry)calendarSpinner.getAdapter().getItem(position);
		
		if (calendar == null) {
			System.out.println("Calendar is invalid");
			return;
		}
		
		System.out.println("Selected calendar: " + calendar.getId());
		
		CalendarWatcher.CALENDAR_SELECTED = calendar;
		
		// Save our selected calendar for future use
		SharedPreferences settings = getSharedPreferences(CalendarWatcher.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit(); 
		editor.putString("calendarId", calendar.getId());
		editor.commit();
		
		Intent intent = new Intent(this, MonitorCalendar.class);
		startActivity(intent);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		
		
	}

}
