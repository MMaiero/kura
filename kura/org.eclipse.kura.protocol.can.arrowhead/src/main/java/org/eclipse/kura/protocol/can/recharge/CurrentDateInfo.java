package org.eclipse.kura.protocol.can.recharge;

import java.util.Map;

public class CurrentDateInfo {
	private final static String CURRENT_DATE_DAY= "current.date.day";
	private final static String CURRENT_DATE_MONTH= "current.date.month";
	private final static String CURRENT_DATE_YEAR= "current.date.year";
	
	private int currentDateDay;
	private int currentDateMonth;
	private int currentDateYear;
	
	public CurrentDateInfo(Map<String,Object> properties){
		int currentDateDay = Integer.parseInt((String) properties.get(CURRENT_DATE_DAY));
		int currentDateMonth = Integer.parseInt((String) properties.get(CURRENT_DATE_MONTH));
		int currentDateYear = Integer.parseInt((String) properties.get(CURRENT_DATE_YEAR));

		this.setCurrentDateDay(currentDateDay);
		this.setCurrentDateMonth(currentDateMonth);
		this.setCurrentDateYear(currentDateYear);
	}
	
	public int getCurrentDateDay() {
		return currentDateDay;
	}
	public void setCurrentDateDay(int currentDateDay) {
		this.currentDateDay = currentDateDay;
	}
	public int getCurrentDateMonth() {
		return currentDateMonth;
	}
	public void setCurrentDateMonth(int currentDateMonth) {
		this.currentDateMonth = currentDateMonth;
	}
	public int getCurrentDateYear() {
		return currentDateYear;
	}
	public void setCurrentDateYear(int currentDateYear) {
		this.currentDateYear = currentDateYear;
	}
}
