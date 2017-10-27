package com.eastsidegamestudio.calendarwatcher;

import java.util.ArrayList;
import java.util.List;

import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class CalendarListAdapter extends BaseAdapter implements SpinnerAdapter {
	
	Context context;
	
	private List<CalendarListEntry> entries;
	
	public CalendarListAdapter(Context context, CalendarList calendarList) {
		this.context = context;
		this.entries = new ArrayList<CalendarListEntry>();
		
		this.entries.add(0, null);
		this.entries.addAll(calendarList.getItems());
		
	}
	
	@Override
	public int getCount() {
		return this.entries.size();
	}

	@Override
	public Object getItem(int position) {
		return this.entries.get(position);
	}

	@Override
	public long getItemId(int position) {
		if (position >= this.entries.size() || position < 0) {  
            return -1;  
        }
		
		return position;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        TextView textview = (TextView) inflater.inflate(android.R.layout.simple_spinner_item, null);
        
        CalendarListEntry entry = this.entries.get(position);
        if (entry == null) {
        	textview.setText("-- SELECT --");
        } else {
        	textview.setText(entry.getSummary());
        }
        
        return textview;
	}

}
