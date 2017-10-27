package com.eastsidegamestudio.calendarwatcher;

import android.accounts.Account;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
	
	GoogleAuthorizationCodeFlow flow;
	
	HttpTransport transport;
	JacksonFactory jsonFactory;

    GoogleApiClient mGoogleApiClient;
    Account mAuthorizedAccount;

    ProgressDialog mProgressDialog;

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean resetUser = getIntent().getBooleanExtra("reset_user", false);

        CalendarWatcher.GOOGLE_SCOPES = new ArrayList<String>();
        CalendarWatcher.GOOGLE_SCOPES.add("https://www.googleapis.com/auth/calendar.readonly");

        try {
            transport = new ApacheHttpTransport();
            jsonFactory = JacksonFactory.getDefaultInstance();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }

		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/calendar.readonly"))
				.build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        SignInButton signInButton = (SignInButton)findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(this);

        checkForUpdates();
    }
    
    @Override
    protected void onStart() {
    	super.onStart();

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
            Log.d(TAG, "Got cached sign in!");
        } else {
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }

        checkForCrashes();
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        
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
    
	private void checkForCrashes() {
		CrashManager.register(this, CalendarWatcher.HOCKEYAPP_ID);
	}

	private void checkForUpdates() {
		// Remove this for store builds!
		UpdateManager.register(this, CalendarWatcher.HOCKEYAPP_ID);
	}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently

        Log.d(TAG,"Error connecting to Google APIs: " + connectionResult.toString());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                showProgressDialog();
                signIn();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            hideProgressDialog();
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            // Sign in successfully
            GoogleSignInAccount acct = result.getSignInAccount();

            Log.d(TAG, "handleSignInResult: " + result.isSuccess());
            mAuthorizedAccount = acct.getAccount();

            // Set up credentials here
			GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton("https://www.googleapis.com/auth/calendar.readonly"));
			credential.setSelectedAccount(mAuthorizedAccount);

			CalendarWatcher.GOOGLE_CREDENTIAL = credential;

			CalendarWatcher.GOOGLE_CALENDAR = new Calendar.Builder(transport, jsonFactory, CalendarWatcher.GOOGLE_CREDENTIAL).setApplicationName("Calendar Watcher").build();

            // Have we previously selected a calendar?
            SharedPreferences preferences = getSharedPreferences(CalendarWatcher.PREFS_NAME, 0);
            String calendarId = preferences.getString("calendarId", null);

            showProgressDialog();

            if (calendarId != null) {
                AsyncTask<String, String, CalendarListEntry> selectCalendarTask = new SelectCalendarTask(this);
                selectCalendarTask.execute(calendarId);

            } else {
                AsyncTask<String, String, CalendarList> calendarListTask = new CalendarListTask(this);
                calendarListTask.execute();
            }

        } else {
            // Signed out
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private class CalendarListTask extends AsyncTask<String, String, CalendarList> {

        private Context context;
        @Override
        protected CalendarList doInBackground(String... args) {

            CalendarList list = null;

            try {
                list = CalendarWatcher.GOOGLE_CALENDAR.calendarList().list().execute();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return list;
        }

        public CalendarListTask(Context context) {
            this.context = context;
        }

        @Override
        public void onPostExecute(CalendarList list) {

            CalendarWatcher.CALENDARLIST = list;
            this.context.startActivity(new Intent(this.context, SelectCalendar.class));
        }

    }

    private class SelectCalendarTask extends AsyncTask<String, String, CalendarListEntry> {

        private Context context;

        public SelectCalendarTask(Context context) {
            this.context = context;
        }

        @Override
        protected CalendarListEntry doInBackground(String... params) {
            CalendarListEntry calendar = null;

            if (params.length > 0) {
                try {
                    calendar = CalendarWatcher.CALENDAR_SELECTED = CalendarWatcher.GOOGLE_CALENDAR.calendarList().get(params[0]).execute();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            return calendar;
        }

        @Override
        public void onPostExecute(CalendarListEntry calendar) {
            if (calendar != null) {
                Intent intent = new Intent(this.context, MonitorCalendar.class);
                startActivity(intent);
            } else {
                AsyncTask<String, String, CalendarList> calendarListTask = new CalendarListTask(this.context);
                calendarListTask.execute();
            }

        }
    }


}
