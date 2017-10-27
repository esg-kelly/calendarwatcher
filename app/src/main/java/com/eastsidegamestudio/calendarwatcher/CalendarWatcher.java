package com.eastsidegamestudio.calendarwatcher;

import java.util.List;
import java.util.Map;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;

public class CalendarWatcher {
	
	public static final String GOOGLE_CLIENT_ID = "245344345351-fjdjh6l6dgvqgguuuo7h9drdtgb2jj9v.apps.googleusercontent.com";
	public static final String GOOGLE_CLIENT_SECRET = "lU7IN6BEpw4S7tkqk5ScvQ0a";
	public static final String GOOGLE_REDIRECT_URI = "http://localhost/oauth2callback";
	
	public static List<String> GOOGLE_SCOPES;
	
	public static GoogleAccountCredential GOOGLE_CREDENTIAL;
	
	public static Calendar GOOGLE_CALENDAR;
	
	public static CalendarList CALENDARLIST;
	public static CalendarListEntry CALENDAR_SELECTED;
	
	public static final int CHECK_INTERVAL = 30;	// In seconds
	public static final int INCOMING_BUFFER = 15;	// In minutes
	
	public static final String DATASTORE_ID = "";
	public static final String PREFS_NAME = "CalendarWatcher";
	
	public static final String HOCKEYAPP_ID = "a3bdeabf3670bc9756218be19db7517a";
	
	public static final String DEFAULT_SLEEP_TIME = "18:00";
	public static final String DEFAULT_WAKE_TIME = "9:00";
}
