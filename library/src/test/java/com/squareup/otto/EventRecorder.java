package com.squareup.otto;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

/** Base class for other subscriber classes. */
public abstract class EventRecorder {

	private List<String> events = new ArrayList<String>();

	protected void recordEvent(String event) {
		events.add(event);
	}
	
	protected boolean isFirstEvent() {
		return events.size() == 0;
	}
	
	public List<String> getEvents() {
		return events;
	}
	
	public void assertEventsCount(int count) {
		Assert.assertEquals(count, events.size());
	}
	
	public void assertLastEvent(String event) {
		Assert.assertTrue(events.size() > 0);
		Assert.assertEquals(event, events.get(events.size() - 1));
	}	
	
}
