package dotrural.ac.uk.journeys;

import java.util.ArrayList;

public class Journey {
	
	private int startMinutes = 0; 
	private int endMinutes = 0; 
	private String traveller = "";
    private int delay  =0;
    private ArrayList  busRoutes;
    private ArrayList  busRoutesInitialStage;
    private String journeyId ="";
    private String busStopCode ;
   
	
	public Journey () {
		
	}

	
	
	
	public void setDelay(int delay) {
		this.delay = delay;
	}
	
	public int getDelay() {
		return delay;
	}
	
	public void setEndsMinutes(int endMinutes) {
		this.endMinutes = endMinutes;
		
		
	}

	public void setStartMinutes(int startMinutes) {
		System.out.println("setting start minutes " + startMinutes);
		this.startMinutes = startMinutes;
		
	}
	
	public int getEndsMinutes() {
		// TODO Auto-generated method stub
		return endMinutes;
	}

	public int getStartMinutes() {
		// TODO Auto-generated method stub
		return startMinutes;
	}

	public void setTraveler (String name) {
		traveller = name;
	}

	public String getTraveller() {
		// TODO Auto-generated method stub
		return traveller;
	}
	
	public void setBusRoutes(ArrayList array) {
		busRoutes = array;
		
	}
	
	public ArrayList getBusRoutes() {
		// TODO Auto-generated method stub
		return busRoutes;
	}
	
	public void setBusRoutesInitialStage (ArrayList array) {
		busRoutesInitialStage = array;
		
	}
	
	public ArrayList getBusRoutesInitialStage () {
		// TODO Auto-generated method stub
		return busRoutesInitialStage;
	}

	public void setID(String stringID) {
		journeyId = stringID;
		
	}
	
	public String getID () {
		return journeyId;
	}

	public void setBusStopCode(String string) {
		busStopCode = string;
		
	}
	
	public String getBusStopCode() {
		// TODO Auto-generated method stub
		return busStopCode;
	}
}
