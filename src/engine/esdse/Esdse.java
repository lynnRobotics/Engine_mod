package engine.esdse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import mchess.Mchess;
import engine.ercie.GAinference;
import engine.ercie.GaEscGenerator;
import engine.ercie.classifier.GaDbnClassifier;
import s2h.platform.node.PlatformMessage;
import s2h.platform.node.PlatformTopic;
import s2h.platform.node.Sendable;
import s2h.platform.support.JsonBuilder;
import s2h.platform.support.MessageUtils;
import util.control.ControlAgent;
import util.datastructure.AppNode;
import util.datastructure.EnvStructure;
import util.datastructure.RelationTable;
import util.datastructure.SensorNode;
import util.mq.Producer;

/**
 * @author Mao (2012.06)
 * Revised by Shu-Fan, 2013/11/19
 * Modularized by Guan-Lin, 2015/4/30
 */

public class Esdse {
	// MQ related 
	public Producer producer = new Producer();
	private JsonBuilder json = MessageUtils.jsonBuilder();
	private int reconnect_counter = 0;

	// Preference and Control agent 
	public ControlAgent controlAgent = new ControlAgent(producer);
	
	// Check whether all the sensors are collected 
	private ArrayList<String> updatedSensorList = new ArrayList<String>();
	public Date firstSensorArrivalTime = null;
	// sensorReading.size() - "current" - "camera" - "audio"
	// 24 - 12 - 5 - 1 = 6
	int sensorCount = 6; // modularize
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
	
	// Control the appliance or not 
	private boolean doControl = true;
	private boolean initialization = true;
	
	// Some parameters for rule based condition
	private boolean activityChanged = false;
	private boolean wakeUpFlag = false;
	private boolean standbyOff = false;
	
	// Communicate with web page for confirming the control 
	public boolean signal = true;
	private Date noSignalStartTime = null;
	private int maxNoSignalDuration = 45; // unit is second //modularize
	public boolean reject = false;
	public boolean backToLive = false;
	
	// Hallway activity recognition usage 
	private int NumOfPeople = 0;
	private int OldNumOfPeople = 0;
	private boolean ToBeGoOut = false;
	private boolean standbyAllOn = false;
	
	// Modularization
	EsdseXMLHandler esdseXMLHandler;
	double duration;
	double stableDuration = 5;
	
	public Esdse() {
		// modularize
		esdseXMLHandler = new EsdseXMLHandler();
		sensorCount = esdseXMLHandler.getSensorCount();
		maxNoSignalDuration = esdseXMLHandler.getMaxNoSignalDuration();
		stableDuration = esdseXMLHandler.getStableDuration();
		
		// MQ producer send initialization signal to web interface
		producer.setURL(Mchess.mqURL);
		while(!producer.connect());
		producer.getSendor();
		json.reset();
		producer.sendOut(json.add("subject", "signal").add("initialization", "start").toJson(), "ssh.CONTEXT");
		
		// Initialization of sensor readings
		Mchess.setInitialSensorReading();
		
		// Initialization of comfort sensor reading according to location
		Mchess.setInitialComfortSensorReading();
		
		// Initialization of ezmeter reading of each appliance
		Mchess.setInitialEZmeterReading();
	}

	/* Called by default MQ processMsg(String m) in Mchess.java */
	public void processMQMessage(String message) {
		// Receive over 50000 message then restart producer
		reconnect_counter++;
		if(reconnect_counter > 50000) {
			producer.disconnect();
			while(!producer.connect());
			producer.getSendor();
			reconnect_counter = 0;
		}
		
		// Process received message : reject, recover, accept
		String subject = extractValue(message, "subject");
		String value;
		if(subject.equals("signal")){
			value = extractValue(message, "value");
			if(extractValue(message, "current_resend").equals("end")){
				backToLive = false;
			}
			
			if(value.equals("")){
				return;
			} 
			else if(value.equals("reject")){
				signal = true;
				reject = true;
			} 
			else if(value.equals("recover")){
				reject = true;
				json.reset();
				producer.sendOut(json.add("subject", "signal").add("value", "recover_ack").toJson(), "ssh.RAW_DATA");
			}
			else if (value.equals("accept")) {
				signal = true;
				reject = false;
			}
			else {
				return;
			}
			// For debugging
			System.err.println("reject = " + reject);
		}
		// Message from socketmeter
		else if(subject.equals("socketmeter")){
			String ampere = extractValue(message, "ampere");
			double ampere_value = Double.parseDouble(ampere);
			String type = subject;                   
			String id = extractValue(message, "id"); 
			SensorNode node = EnvStructure.ezMeterList.get(type + "_" + id);
			if (node != null) {
				// Get corresponding appliance name according to ezMeter id
				String applianceName = EnvStructure.ezMeterList.get(type + "_" + id).name;
				// Put newest ampere info into ezMete reading
				Mchess.ezMeterAmpereReading.put(applianceName, ampere_value);
			}

			// If id = 20 (AC_bedroom), set temperature of bedroom 
			// else if id = 21 (AC_livingroom), set temperature of livingroom
			// TODO not use id
			if(id.equals("20")){
				int temperature = Integer.parseInt(extractValue(message, "target_temperature"));
				EnvStructure.roomACTargetTemperature.put("bedroom", Integer.toString(temperature));
			}
			else if(id.equals("21")){
				int temperature = Integer.parseInt(extractValue(message, "target_temperature"));
				EnvStructure.roomACTargetTemperature.put("livingroom", Integer.toString(temperature));
			}

			// If all the ezmeter reading is updated then end the initialization 
			// Because zeMeter is the slowest sensor node during info retrieval
			if(initialization){
				// Dump those not ready ezMeters
				Set<String> keySet = Mchess.ezMeterAmpereReading.keySet();
				for(String ezMeter : keySet){
					if(Mchess.ezMeterAmpereReading.get(ezMeter).equals(-1.0)){
						System.err.print(ezMeter + ", ");
					}
				}
				System.out.println();
				if (!Mchess.ezMeterAmpereReading.containsValue(-1.0)) {
					initialization = false;
					producer.sendOut(json.add("subject", "signal").add("initialization", "end").toJson(), "ssh.CONTEXT");
				}
			}
			return;
		}
		else if (subject.equals("people")) {
			SensorNode sensorNode = GaDbnClassifier.getSensorNode(message);
			if (sensorNode != null) {
				Mchess.sensorReading.put(sensorNode.name, sensorNode.discreteValue);
			}
			return;
	    }
		else if (subject.equals("current")){
	    	SensorNode sensorNode = GaDbnClassifier.getSensorNode(message);
			if(sensorNode != null){
				Mchess.sensorReading.put(sensorNode.name, sensorNode.discreteValue);
			}
		}
		// If receive signal from web then start the next iteration's recognition
		if(signal){
			//Calendar cl = Calendar.getInstance();
			//while(time_substract(cl, Calendar.getInstance()) < 20);
			noSignalStartTime = null;
			processForRealTime(message);
		} 
		else{
			if(noSignalStartTime == null){
				noSignalStartTime = new Date();
			} 
			else {
				checkNoSignalDuration();
			}
		}
	}
	/* If there's a GA we need to merge them (real-time version) */
	private ArrayList<AppNode> eusAggregationForRealTime(GAinference gaInference, Map<String, String> sensorReading){
		ArrayList<String> gaInferResultList = gaInference.gaInferResultList;
		ArrayList<AppNode> eusAggregationList = new ArrayList<AppNode>();
		// Inference result set might be empty 
		if(gaInferResultList.size() == 0){
			// Copy app from appList
			Map<String, AppNode> appList = EnvStructure.appList;
			Set<String> keySet = appList.keySet();
			for(String str : keySet){
				AppNode app = appList.get(str);
				AppNode newApp = app.copyAppNode(app);
				eusAggregationList.add(newApp);
			}
		}
		else{
			for(String str : gaInferResultList){
				for (GaEscGenerator gaEsc : gaInference.GaEscList) {
					boolean containKey = gaEsc.actAppList.containsKey(str);
					if(containKey){
						ArrayList<AppNode> tmpList = gaEsc.actAppList.get(str).appList;
						for (AppNode tmp : tmpList) {
							AppNode app = tmp.copyAppNode(tmp);
							int same = -1;
							for (int i = 0; i < eusAggregationList.size(); i++) {
								if(eusAggregationList.get(i).appName.equals(app.appName)) same = i;
							}
							if(same < 0) eusAggregationList.add(app); 
							else {
								int newPriority = getPriority(app);
								int oldPriority = getPriority(eusAggregationList.get(same));
								if (newPriority < oldPriority) {
									eusAggregationList.get(same).state = app.state;
									eusAggregationList.get(same).escType = app.escType;
									eusAggregationList.get(same).confidence = app.confidence;
								}
							}
						}
					}
				}
			}
		}

		// Update EUS from sensorReading
		// A map to store status of each sensor and extract list of sensor name from sensorStatus
		Map<String, ArrayList<String>> sensorStatus = EnvStructure.sensorStatus;
		String[] sensorName = (String[]) sensorStatus.keySet().toArray(new String[0]);
		for(AppNode eus : eusAggregationList){
			for(int i = 0; i < sensorName.length; i++){
				if (eus.appName.equals(sensorName[i])) eus.envContext = sensorReading.get(sensorName[i]);
			}
		}
		return eusAggregationList;
	}
	
	// This is appliance agent(thermal, visual) dispatcher
	private void eusDispatch_new(ArrayList<AppNode> eusList, GAinference gaInference) {
		// Get inferred activity set
		Set<String> actInferResultSet = gaInference.actInferResultSet;
		// Store location of inferred activity
		Set<String> actLocation = new HashSet<String>();
		// Get all set of combination of room and activity
		Map<String, RelationTable> actAppList = EnvStructure.actAppList;
		Set<String> actRoomSet = actAppList.keySet();
		
		// Match inferred activity with room-activity set 
		// If there's a match then store it in actLocation
		for (String str : actInferResultSet) {
			for (String str2 : actRoomSet) {
				if (str2.contains(str)) {
					String location = str2.split("_")[0];
					actLocation.add(location);
				}
			}
		}
		
		for (AppNode app : eusList) {
			// Set before in case no matching
			app.agentName = "ap";
			if (app.comfortType.equals("thermal") && app.global && !actInferResultSet.contains("GoOut")){
				// Since current_AC_livingroom is in global, we need to make sure it won't be dispatched to
				// thermal agent if activity only happens in bedroom
				if(!actLocation.contains("bedroom") || actLocation.size() != 1){
					app.agentName = "thermal";
				}
			}
			else if(app.comfortType.equals("visual")){
				for (String str : actLocation) {
					if (app.location.equals(str)) {
						app.agentName = "visual";
					}
				}
			} 
			else if(app.comfortType.equals("thermal")){
				for (String str : actLocation) {
					if (app.location.equals(str)) {
						app.agentName = "thermal";
					}
				}
			} 
			else {
				app.agentName = "ap";
			}
		}
	}

	/* Go through all process (Recognition -> Provide Service) */
	public void processForRealTime(String message) {
		// If still in the process of initialization then we don't infer
		if (initialization) {
			return;
		}
		SensorNode sensorNode = GaDbnClassifier.getSensorNode(message);
		// Do not process unknown sensor node
		if (sensorNode == null) {
			return;
		}
		// Compute human number
		int humanNumber = 0;
		for (String sensorName : Mchess.sensorReading.keySet()) {
			if (sensorName.contains("people") && Mchess.sensorReading.get(sensorName).startsWith("on")) {
				humanNumber += Integer.parseInt(Mchess.sensorReading.get(sensorName).split("_")[1]);
			}
		}
		NumOfPeople = humanNumber;
		// Set parameter for checking Whether GoOut or not
		// If there's no people at home and previous people count > present people count
		if ((NumOfPeople == 0) && (OldNumOfPeople > NumOfPeople)) {
		   ToBeGoOut = true; 
		}
		OldNumOfPeople = NumOfPeople;
		
		// Rule-based check and control ComeBack & GoOut
		controlAgent.checkComeBackAndGoOut(sensorNode,ToBeGoOut,OldNumOfPeople,standbyAllOn);
		
		// Update sensor reading
		Mchess.sensorReading.put(sensorNode.name, sensorNode.discreteValue);
		// If this sensor hasn't shown at this round then add to updatedSensorList
		if (!updatedSensorList.contains(sensorNode.name)) {
			updatedSensorList.add(sensorNode.name);
			// For debugging, print out how many sensor's already updated during this round
			//if(!initialization) System.out.print(updatedSensorList.size() + " ");
			if (updatedSensorList.size() == 1) {
				firstSensorArrivalTime = new Date();
			}
		}
		
		// Original initialization return position
		
		// If the received sensor number isn't enough and waiting time is still under boundary (1.5s)
		// then we don't infer
		if (updatedSensorList.size() < sensorCount) {
			//System.out.print(updatedSensorList.size() + " ");
			if (((new Date().getTime() - firstSensorArrivalTime.getTime()) / 1000.0) < 3) return;
		}

		// For debugging, print out the name of last added sensor
		// System.out.println("last add = " + sensorNode.name);
		
		updatedSensorList.clear();
		
		// For debugging, print out sensor readings
		Map<String, ArrayList<String>> sensorStatus = EnvStructure.sensorStatus;
		String[] sensorNameArray = (String[]) sensorStatus.keySet().toArray(new String[0]);
		
		if(Mchess.ercie == null) { // jump out if it is in a learning mode
			for (String sensorName : sensorNameArray) {
				String featureString = Mchess.sensorReading.get(sensorName).split("_")[0];
				System.out.print(featureString + " ");
			}
			/////
			// print the Activity Label System.out.print("#"+...);
			/////
			System.out.println();
			return;
		}
		////////////////////// running mode //////////////////////

		System.out.println(); // start with "@" for logging
		System.out.print("@"); // start with "@" for logging
		for (String sensorName : sensorNameArray) {
			System.out.print(sensorName + ":" + Mchess.sensorReading.get(sensorName) + ",");
		}
		System.out.println();
		
		// Show comfort sensor readings according to each location
		System.out.print("@@"); // start with "@" for logging
		for (String location : EnvStructure.roomList) {
			System.out.print(location + ":" + Mchess.temperatureReading.get(location) + " C,");
			System.out.print(location + ":" + Mchess.humidityReading.get(location) + " %,");
			System.out.print(location + ":" + Mchess.illuminationReading.get(location) + " Lux,");
		}
		System.out.println();
		
		// Print out number of human in this iteration
		System.out.println("#humanNumer = " + humanNumber);
		
		// Infer GA
		Mchess.gaInferenceForRealTime(humanNumber);
		
		// Print out inferred activity
		if (Mchess.gaInference.actInferResultSet.size() == 0) {
			System.out.print("@@@Infer: NoActivity");
		}
		else {
			for(String activity : Mchess.gaInference.actInferResultSet){
				System.out.print("@@@Infer: " + activity);
			}
		}
		System.out.println(","+sdf.format(Calendar.getInstance().getTime()));
		
		
		// Check unwanted (ComeBack & GoOut) activity
		// Now we address ComeBack & GoOut with some rules
		if (Mchess.gaInference.actInferResultSet.contains("ComeBack") || Mchess.gaInference.actInferResultSet.contains("GoOut")) {
			return;
		}
		
		// Check inferred activity set with previous time 
		// If there's one activity not including in the previous inferred set, set sameInferResult = false; 
		Mchess.currentActInferResultSet = new ArrayList<String>(Mchess.gaInference.actInferResultSet);
		boolean sameInferResult = true;
		for (String activity : Mchess.currentActInferResultSet) {
			if (!Mchess.previousActInferResultSet.contains(activity)) {
				sameInferResult = false;
				break;
			}
		}
		
		// If inferred activity set equals to previous time
		Date currentTime = new Date();
		if (Mchess.currentActInferResultSet.size() == Mchess.previousActInferResultSet.size() && sameInferResult) {
			duration = (currentTime.getTime() - Mchess.startTime.getTime()) / 1000.0;
			activityChanged = false;
			// Make sure activity changed is not because some noise
			if (duration < stableDuration) {
				activityChanged = true;
				System.err.println("Still in the non-stable period!");
				System.out.println("===========================================");
				return;
			}
		}
		else {
			// Decide wake up or not
			if (Mchess.previousActInferResultSet.contains("AllSleeping")) {
				wakeUpFlag = true;
			}
			Mchess.previousActInferResultSet = new ArrayList<String>(Mchess.gaInference.actInferResultSet);
			Mchess.startTime = currentTime;
			System.err.println("Activity change happened!");
			System.out.println("===========================================");
			return;
		}
		
		// Update previous result set
		Mchess.previousActInferResultSet = new ArrayList<String>(Mchess.gaInference.actInferResultSet);
		
		// Check previous activity for reject mode 
		// If activity set is different between present set and reject set 
		// activity've changed, reject = false;
		if(Mchess.gaInference.actInferResultSet.size() != Mchess.previousActInferResultSetForReject.size()){
			reject = false;
		}
		else{
			for(String activity : Mchess.gaInference.actInferResultSet){
				if(!Mchess.previousActInferResultSetForReject.contains(activity)){
					sameInferResult = false;
					System.err.println("Inferred activity is different from the one in reject mode!");
					reject = false;
					break;
				}
			}
		}
		
		// Reject period
		if (reject && sameInferResult) {
			Mchess.previousActInferResultSetForReject = new ArrayList<String>(Mchess.gaInference.actInferResultSet);
			sendInferedActivityToMQ();
			System.err.println("In the rejected period!");
			return;
		}
		
		Mchess.previousActInferResultSetForReject = new ArrayList<String>(Mchess.gaInference.actInferResultSet);
		
		// Send inferred activity to MQ
		sendInferedActivityToMQ();
		
		ArrayList<AppNode> eusList = null;
		ArrayList<AppNode> decisionList = null;
		
		// Optimization step
		// Inferred result set might be empty
		if(Mchess.gaInference.actInferResultSet.size() == 0){
			eusList = eusAggregationForRealTime(Mchess.gaInference, Mchess.sensorReading);
			decisionList = eusList;
		} 
		else {
			// 1.EUS aggregation
			eusList = eusAggregationForRealTime(Mchess.gaInference, Mchess.sensorReading);
			// 2.EUS dispatch
			eusDispatch_new(eusList, Mchess.gaInference); 
			// 3.Optimization
			Optimizer op = new Optimizer(producer);
			// 4.Get appliance control decision list
			decisionList = op.getOptDecisionList(eusList, Mchess.gaInference);
		}
		
		// Debug print out
		System.out.println("Origin eusList");
		for (AppNode app : eusList) {
			System.out.print(app.appName + ":" + app.envContext + ", ");
		}
		System.out.println();	
		System.out.println("eusList size = " + eusList.size());
	
		System.out.println("desicionList");
		for(AppNode app : decisionList){
			System.out.print(app.appName + ":" + app.envContext + ", ");
		}
		System.out.println();
		System.out.println("desicionList size = " + decisionList.size());
		
		// Control appliance or not
		if (!doControl) {
			System.out.println("=================================================");
			return;
		} 
		// If wake up then open all standby power
		else if(wakeUpFlag){
			wakeUpFlag = false;
			if(standbyOff){
				standbyOff = false;
				controlAgent.turnOnStandbyPower();
			}
			return;
		}
		// If there is no activity
		else if (Mchess.gaInference.actInferResultSet.size() == 0) {
			return;
		}
		else {
			if(Mchess.gaInference.actInferResultSet.contains("AllSleeping") && Mchess.gaInference.actInferResultSet.size() == 1){
				ArrayList<String> exceptionID = new ArrayList<String>();
				exceptionID.add("16");
				if (!standbyOff) {
					standbyOff = true;
					controlAgent.turnOffStandbyPower(exceptionID);
				}
			}
			System.out.println("Control starts!");
			boolean controlExistence = controlAgent.controlAppliance(decisionList, eusList);
			System.out.println("Control finishes!");
			// If controlExistence is true means control finish
			// Set signal to false and wait for signal from log engine
			if(controlExistence){
				signal = false;
			}
			// Even if control not finish or fail, we still need to send signal to web and log engine
			else if(activityChanged){
				//reject = false;
				//System.err.println("activity changed!");
				controlAgent.sendControlStartSignal();
				controlAgent.sendControlEndSignal();
			}
		}
		System.out.println("====================================================");
	}

	/* Send inferred activity to MQ */
	private void sendInferedActivityToMQ() {
		json.reset();
		if (Mchess.gaInference.actInferResultSet.size() != 0) {
			String inferedActivity = "";
			String inferedGA = "";
			// Concat all inferred activities
	    	for (String activity : Mchess.gaInference.actInferResultSet) {
	    		inferedActivity = inferedActivity.concat(activity + " ");
	    	}
	    	inferedActivity = inferedActivity.trim().replace(' ', '#');
	    	// Concat all inferred GAs
	    	for(String GA : Mchess.gaInference.gaInferResultList){
	    		inferedGA = inferedGA.concat(GA + " ");
	    	}
	    	inferedGA = inferedGA.trim().replace(' ', '#');
	    	// Send message to MQ
	    	if (!inferedActivity.equals("")) {
	    		producer.sendOut(json.add("subject", "activity").add("name", inferedActivity).add("GA", inferedGA).toJson(), "ssh.CONTEXT");
	    	}
		} 
		else {
			producer.sendOut(json.add("subject", "activity").add("name", "NoActivity").add("GA", "NoGA").toJson(), "ssh.CONTEXT");
		}
	}
	
	/* Return priority according to the combination of status and preference type (i.e., explicit or implicit) 
	 * The higher priority the lower number
	 * */
	private int getPriority(AppNode app) {
		if (app.state.equals("on") && app.escType.equals("explicit")) {
			return 1;
		} else if (app.state.equals("on") && app.escType.equals("implicit")) {
			return 2;
		} else if (app.state.equals("standby") && app.escType.equals("explicit")) {
			return 3;
		} else if (app.state.equals("standby") && app.escType.equals("implicit")) {
			return 4;
		} else if (app.state.equals("off") && app.escType.equals("explicit")) {
			return 5;
		} else if (app.state.equals("off") && app.escType.equals("implicit")) {
			return 6;
		}
		return 7;
	}
	
	
	/*
	 * Check the duration of not receiving feedback signal from web interface 
	 * Duration > 45s, Directly set signal = true 
	 * Pretend like we received signal 
	 * */
	private void checkNoSignalDuration() {
		Date currentTime = new Date();
		double duration = (currentTime.getTime() - noSignalStartTime.getTime()) / 1000.0;
		if(duration > maxNoSignalDuration){
			signal = true;
			//noSignalStartTime = null;
			System.err.println("Long time no signal detected!");
		}
	}
	
	/* Extract value of key from message */
	private String extractValue(String message, String key) {
		return util.mq.JsonBuilder.getValue(message, key);
	}		
	
}
