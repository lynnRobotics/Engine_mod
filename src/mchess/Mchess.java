package mchess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Date;

import org.apache.log4j.Logger;

import engine.ercie.Ercie;
import engine.ercie.ErcieXMLHandler;
import engine.ercie.GAinference;
import engine.ercie.GaGenerator;
import engine.esdse.Esdse;
import s2h.platform.support.MessageUtils;
import util.datastructure.EnvStructure;
import util.mq.Consumer;

/* Import necessary package */

/**
 * MCHESS
 * 
 * @author Mao (2012.06)
 */

public class Mchess extends Consumer implements Runnable {
	
	MchessXMLHandler mchessXMLHandler;

	public static String ercieInitializationPath = "./_input_data/ercieInitilization.xml";
	public static String thermalInitializationPath = "./_input_data/thermalInitilization.xml";
	public static String environmentInitializationPath = "./_input_data/environmentInitialization.xml";
	public static String esdseInitializationPath = "./_input_data/esdseInitilization.xml";
	public static String mchessInitializationPath = "./_input_data/mchessInitilization.xml";
	public static String mqURL = "tcp://140.112.49.154:61616"; // used by esdse and Mchess
	
	/* Two main engine */
	static public Ercie ercie = null;
	static public Esdse esdse;
	
	/* Different kinds of reading */ // Were in ESDSE, but UCEE and ESDSE both need it
	public static Map<String, String> sensorReading = new LinkedHashMap<String, String>();
	public static Map<String, Double> ezMeterAmpereReading = new LinkedHashMap<String, Double>();
	public static Map<String, Double> temperatureReading = new LinkedHashMap<String, Double>();
	public static Map<String, Double> humidityReading = new LinkedHashMap<String, Double>();
	public static Map<String, Double> illuminationReading = new LinkedHashMap<String, Double>();
	
	// Were in ERCIE, used by ESDSE
	public static GAinference gaInference;
	public static ArrayList<String> currentActInferResultSet = new ArrayList<String>();
	public static ArrayList<String> previousActInferResultSet = new ArrayList<String>();
	public static ArrayList<String> previousActInferResultSetForReject = new ArrayList<String>();
	public static Date startTime = new Date();
	
	s2h.platform.support.JsonBuilder json = MessageUtils.jsonBuilder();
	//private Logger log = Logger.getLogger(Mchess.class.getName());

	public Mchess(String mode) {
		super();               // Consumer's constructor
		// Modularize
		mchessXMLHandler = new MchessXMLHandler();
		// Modularize
		esdse = new Esdse();   // Initialization process
		/* Set up MQ information */
		this.setURL("tcp://140.112.49.154:61616");
		this.setTopic("ssh.RAW_DATA");
		this.connect();
		this.listen();
		
		if(mode.equals("-run")) {
			realTimeSysProc();     // Call build model function in epcie
		}
	}

	/* For real-time usage */
	public void realTimeSysProc() {
		ercie.buildModel();
	}
	
	/* MQ message process function */
	@Override
	public void processMsg(String m) {
		esdse.processMQMessage(m);
	}
	
	/* Define what M-CHESS thread should do */ 
	@Override
	public void run() {
		while(true){
			/* If thread doesn't start then try to start again */
			if(!this.isStarted()) {
				this.start();
				this.listen();
				esdse.backToLive = true;
				esdse.signal = false;
			} 
			/* Thread starts, send out the start signal */
			else if (esdse.backToLive) {
				json.reset();
				esdse.producer.sendOut(json.add("subject", "signal").add("current_resend", "start").toJson(), "ssh.RAW_DATA");
			}
			/* Try to sleep one second */
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("please add the argument!\ne.g. [mchess] -run or [mchess] -learn");
			return; // Test necessary argument from input
		}
		if(args[0].equals("-run")) {
			System.out.println("===Running Mode===");
			ercie = new Ercie();  // New a ercie object // training threshold is initialized in ercieXMLHandler.xml
			new Mchess(args[0]).run();          // New a M-CHESS object and start the thread
		}
		else if(args[0].equals("-learn")) {
			System.err.println("===Learning Mode===");
			new Mchess(args[0]).run();          // New a M-CHESS object and start the thread
		}
	}
	
	//------------------------------Was called by ESDSE------------------------------------------------------------
	static public void setInitialSensorReading(){
		sensorReading = EnvStructure.initialSensorReading;
	}
	// Initialization of comfort sensor reading according to location
	static public void setInitialComfortSensorReading(){
		for (String location : EnvStructure.roomList) {
			temperatureReading.put(location, null);
			humidityReading.put(location, null);
			illuminationReading.put(location, null);
		}
	}
	// Initialization of ezmeter reading of each appliance
	static public void setInitialEZmeterReading(){
		for (String appliance : EnvStructure.ezMeterNameList) {
			ezMeterAmpereReading.put(appliance, -1.0);
		}
	}
	static public void gaInferenceForRealTime(int humanNumber){
		ercie.gaInferenceForRealTime(Mchess.sensorReading, humanNumber);
	}
	//------------------------------Was called in ESDSE------------------------------------------------------------
	
}
