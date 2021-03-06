package engine.ucee.thermal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mchess.Mchess;
import engine.ercie.GAinference;
import engine.ercie.GaGenerator;
import engine.esdse.Esdse;
import engine.esdse.Optimizer;
import s2h.platform.node.PlatformTopic;
import s2h.platform.node.Sendable;
import s2h.platform.support.JsonBuilder;
import s2h.platform.support.MessageUtils;
import util.datastructure.AppNode;
import util.datastructure.EnvStructure;
import util.datastructure.RelationTable;
import util.mq.Producer;

/**
 * Revised by Shu-Fan 2013/11/21
 */

public class ThermalAgent {
	
	ThermalXMLHandler thermalXMLHandler;
	
	/* modularize */
	double initConstraint = 1.0;
	boolean tooColdFlag = false; // The weather is too cold or not
	int iterateLimit = 20; //final int iterateLimit = 20; the number of iterations in getOptThermalListForOnline()
	double incrementConstraint = 0.1; //final double incrementConstraint = 0.1; the increments in every iteration in thermalIterate()
	Map<String, Integer> priorityList = new HashMap<String, Integer>(); // seems not used, Guan-Lin 20150210
	/* modularize */
	
	ArrayList<Double> constraintList = null;
	
	private Map<String, Double> pmvEvaluationResult = new HashMap<String, Double>();
	private Map<String, Integer> optimalTempOpenspace = new HashMap<String, Integer>();
	private Map<String, Integer> optimalTempBedroom = new HashMap<String, Integer>();
	private Map<String, String> locationActivity = new HashMap<String, String>(); // ex:<livingroom, WatchingTV>
	
	public ThermalAgent(){
		thermalXMLHandler = new ThermalXMLHandler();
		initConstraint = thermalXMLHandler.getInitConstraint();
		tooColdFlag = thermalXMLHandler.getTooColdFlag();
		iterateLimit = thermalXMLHandler.getIterateLimit();
		incrementConstraint = thermalXMLHandler.getIncrementConstraint();
		priorityList = thermalXMLHandler.getActivityPriorityList();
	}

	/* Get <activity, appList> */
	public Map<String, ArrayList<AppNode>> getActAppList(ArrayList<AppNode> decisionList, GAinference gaInference){
		// Get inferred activities from GA
		Set<String> singleAct = gaInference.actInferResultSet;

		// Get location list
		ArrayList<String> locationList = getActLocationList(singleAct);
		
		Map<String, ArrayList<AppNode>> actAppList = new LinkedHashMap<String, ArrayList<AppNode>>();
		// Insert appliance into appList based on locationList
		int i = 0;
		for(String act : singleAct){
			ArrayList<AppNode> appList = new ArrayList<AppNode>();
			for (AppNode app : decisionList) {
				if(app.global){
					appList.add(app);
				}
				else if(locationList.get(i).equals(app.location)){
					appList.add(app);
				}
			}
			actAppList.put(act, appList);
			i += 1;
		}
		return actAppList;
	}

	/* Add location of inferred activities into locationList */
	public ArrayList<String> getActLocationList(Set<String> singleAct) {
		ArrayList<String> locationList = new ArrayList<String>();
		
		// Get set of all location_activity pairs
		Set<String> location_actSet = EnvStructure.actAppList.keySet();
		// Add location of inferred activities into locationList 
		for(String act : singleAct){
			for(String location_act : location_actSet){
				String activity = location_act.split("_")[1];
				if (activity.equals(act)) {
					String location = location_act.split("_")[0];
					locationList.add(location);
				}
			}
		}
		if (locationList.size() != singleAct.size()) {
			return null;
		} 
		else{
			return locationList;
		}
	}
	
	/* Get single location according to single activity */
	public String getActLocation(String act) {
		Set<String> location_actSet = EnvStructure.actAppList.keySet();
		for (String location_act : location_actSet) {
			if (location_act.contains(act)) {
				String location = location_act.split("_")[0];
				return location;
			}
		}
		return null;
	}

	/* Calculate PMVList according to context from status of appliance */
	public ArrayList<Double> getPmvList(Map<String, ArrayList<AppNode>> actAppList) {

		// Get intensityList
		ArrayList<Double> intensityList = getActIntensityList(actAppList);

		// 活動量 溫度 風量 取得pmv
		ArrayList<Double> pmvList = new ArrayList<Double>();

		Set<String> actSet = actAppList.keySet();
		int i = 0;
		for (String act : actSet) {
			ArrayList<AppNode> appList = actAppList.get(act);
			String location = getActLocation(act);

			// Get intensity, temperature, vel, humidity
			double intensity = intensityList.get(i);
			// Guan-Lin: Get temp from the environment anyway. Do not get temp from airconditioner
			double temp = getTemperature(location);
			/*
			// Shu-Fan: If air condition haven't been turned on, we get temp from environment
			double temp = getTempFromAppList(appList, location);
			if (temp == 0) {
				temp = getTemperature(location);
			}
			*/
			
			double vel = getVelFromAppList(appList);
			double humidity = getHumidity(location);

			// Calculate PMV
			PMVCalculate pc = new PMVCalculate(intensity, temp, vel, humidity);
			double pmv = pc.getPMVandPPD()[0];

			pmvList.add(pmv);
			i += 1;
		}
		return pmvList;
	}
	
	/* Calculate PMVList according to context from environment */
	public ArrayList<Double> getPmvListFromEnvironment(Map<String, ArrayList<AppNode>> actAppList) {
		// Get intensityList
		ArrayList<Double> intensityList = getActIntensityList(actAppList);

		// 活動量 溫度 風量 取得pmv
		ArrayList<Double> pmvList = new ArrayList<Double>();

		Set<String> actSet = actAppList.keySet();
		int i = 0;
		for(String act : actSet){
			// Get appliances of corresponding activity
			ArrayList<AppNode> appList = actAppList.get(act);
			// Get corresponding location
			String location = getActLocation(act);
			// Get intensity, temperature, vel, humidity
			double intensity = intensityList.get(i);
			double temp = getTemperature(location);
			double vel = getVelFromAppList(appList);
			double humidity = getHumidity(location);
			
			// Calculate PMV
			PMVCalculate pc = new PMVCalculate(intensity, temp, vel, humidity);
			double pmv = pc.getPMVandPPD()[0];

			pmvList.add(pmv);
			i += 1;
		}

		return pmvList;
	}

	/* Get intensityList of related activity */
	public ArrayList<Double> getActIntensityList(Map<String, ArrayList<AppNode>> actAppList) {
		ArrayList<Double> intensityList = new ArrayList<Double>();
		
		Set<String> singleAct = actAppList.keySet();
		Map<String, RelationTable> act_location = EnvStructure.actAppList;
		Set<String> keySet = act_location.keySet();
		// Put all intensity of inferred activities to intensityList
		for(String act : singleAct){
			for(String str : keySet){
				if(str.contains(act)){
					double tmp = EnvStructure.actAppList.get(str).intensity;
					intensityList.add(tmp);
				}
			}
		}
		return intensityList;
	}

	/* Revised by Shu-Fan 2013/11/14, Not used by Guan-Lin 2015/2/4
	 * 1. Livingroom & studyroom & kitchen are all affected by current_AC_livingroom
	 * 2. Bedroom is affected by current_AC_bedroom
	 * 3. Return 0 if air condition haven't been turned on
	 
	public double getTempFromAppList(ArrayList<AppNode> appList, String location) {
		double temp = 0;
		for (AppNode app : appList) {
			if(app.appName.equals("current_AC_livingroom") && app.envContext.split("_").length > 1){
				if(location.equals("livingroom") || location.equals("study") || location.equals("kitchen"))
					temp = Double.parseDouble(app.envContext.split("_")[1]);
			}
			else if(app.appName.equals("current_AC_bedroom") && app.envContext.split("_").length > 1){
				if(location.equals("bedroom"))
					temp = Double.parseDouble(app.envContext.split("_")[1]);
			}
		}
		return temp;
	}
	*/
	
	/* Get temperature from environment */
	public double getTemperature(String location) {
		return Mchess.temperatureReading.get(location);
	}
	
	/* Get humidity from environment */
	public double getHumidity(String location) {
		return Mchess.humidityReading.get(location);
	}

	/* Get velocity according to appliance and environment */
	public double getVelFromAppList(ArrayList<AppNode> appList) {
		String fanStatus = "";
		for (AppNode app : appList) {
			/* 這邊沒有彈性 */
			if (app.appName.equals("current_watercoldfan_livingroom")) {
				if (app.envContext.split("_").length > 1) {
					fanStatus = app.envContext.split("_")[1];
				} else {
					fanStatus = app.envContext.split("_")[0];
				}
			}
		}
		double vel;
		if (fanStatus.equals("1")) {
			vel = 0.4;
		} else if (fanStatus.equals("2")) {
			vel = 0.6;
		} else if (fanStatus.equals("3")) {
			vel = 1;
		} else if (fanStatus.equals("standby") || fanStatus.equals("off")) {
			vel = 0.2;
		} else {
			vel = 0.2;
		}
		return vel;
	}

	/* Get final pmvList according to context from appList */
	/* input: decision array, inferResult */
	public ArrayList<Double> getComfortArray(ArrayList<AppNode> decisionList, GAinference gaInference){
		/* getThermalApp */
		// ArrayList<AppNode> thermalAppList=getThermalApp(decisionList);
		/* act-app List by location */
		Map<String, ArrayList<AppNode>> actAppList = getActAppList(decisionList, gaInference);
		/* cal PMV for each act-app */
		ArrayList<Double> pmvList = getPmvList(actAppList);

		return pmvList;
	}
	
	/* Get final pmvList according to context from environment */
	public ArrayList<Double> getComfortArrayFromEnvironment(ArrayList<AppNode> decisionList, GAinference gaInference){
		// getThermalApp
		// ArrayList<AppNode> thermalAppList = getThermalApp(decisionList);
		// Act-app List by location
		Map<String, ArrayList<AppNode>> actAppList = getActAppList(decisionList, gaInference);
		// Cal PMV for each act-app
		ArrayList<Double> pmvList = getPmvListFromEnvironment(actAppList);
		
		return pmvList;
	}

	/* Get optimization thermal control list */
	public ArrayList<AppNode> getOptThermalList(ArrayList<AppNode> eusList, GAinference gaInference, Producer producer) {
		// Optimal Thermal List of AppNode
		ArrayList<AppNode> optThermalList = new ArrayList<AppNode>(); 
		// Get thermal appList
		ArrayList<AppNode> thermalAppList = new ArrayList<AppNode>();
		ArrayList<AppNode> thermalRawList = new ArrayList<AppNode>();
		// Put all thermal related appliance into thermalAppList and thermalRawList
		for(AppNode app : eusList){
			if(app.agentName.equals("thermal")){
				AppNode app2 = app.copyAppNode(app);
				thermalAppList.add(app2);
				AppNode app3 = app.copyAppNode(app);
				thermalRawList.add(app3);
			}
		}
		
		// Thermal App list might be empty
		if (thermalAppList.size() == 0) {
			if (producer == null) {
				System.err.println("Sender is null");
			} 
			else {
				sendPMVToMQ(thermalRawList, gaInference, producer);
			}
			return new ArrayList<AppNode>();
		} 
		else {
			// Debug print out thermal app list
			System.err.print("thermal app list:");
			for(AppNode app : thermalAppList){
				System.err.print(app.appName + ",");
			}
			System.err.println();
			
			// Send PMV to MQ
			if (producer == null) {
				System.err.println("sender is null");
			} 
			else {
				sendPMVToMQ(thermalRawList, gaInference, producer);
			}
			
			// Deal with AC in openspace
			if(optimalTempOpenspace.size() != 0){
				String finalState = "";
				// If there's one activity's PMV already fulfills the constraint 
				if(optimalTempOpenspace.containsValue(0)){
					finalState = "off";
				}
				else{
					// Find the highest temp in optimalTemp
					int highestTemp = 0;
					for(int temp : optimalTempOpenspace.values()){
						if(temp > highestTemp) highestTemp = temp;
					}
					// If required temp > 30, we don't turn on the AC
					if(highestTemp > 30) finalState = "off";
					else finalState = "on_" + String.valueOf(highestTemp);
				}
				// Put appNode with new state in optThermalList
				for(AppNode appNode : thermalAppList){
					if(appNode.appName.equals("current_AC_livingroom")){
						appNode.envContext = new String(finalState);
						optThermalList.add(appNode.copyAppNode(appNode));
					}
				}
			}
			else{
				for(AppNode appNode : thermalAppList){
					if(appNode.appName.equals("current_AC_livingroom")){
						appNode.envContext = new String("off");
						optThermalList.add(appNode.copyAppNode(appNode));
					}
				}
			}
			
			// Deal with AC in bedroom
			if(optimalTempBedroom.size() != 0){
				String finalState = "";
				// If there's one activity's PMV already fulfills the constraint 
				if(optimalTempBedroom.containsValue(0)){
					finalState = "off";
				}
				else{
					// Find the highest temp in optimalTemp
					int highestTemp = 0;
					for(int temp : optimalTempBedroom.values()){
						if(temp > highestTemp) highestTemp = temp;
					}
					// If required temp > 30, we don't turn on the AC
					if(highestTemp > 30) finalState = "off";
					else finalState = "on_" + String.valueOf(highestTemp);
				}
				for(AppNode appNode : thermalAppList){
					if(appNode.appName.equals("current_AC_bedroom")){
						appNode.envContext = new String(finalState);
						optThermalList.add(appNode.copyAppNode(appNode));
					}
				}
			}
			else{
				for(AppNode appNode : thermalAppList){
					if(appNode.appName.equals("current_AC_bedroom")){
						appNode.envContext = new String("off");
						optThermalList.add(appNode.copyAppNode(appNode));
					}
				}
			}
			
			return optThermalList;
		}
	}
	
	/* optimization without MQ */
	public ArrayList<AppNode> getOptThermalListForOnline(ArrayList<AppNode> eusList, GAinference gaInference) {
		/* get thermal List */
		ArrayList<AppNode> thermalAppList = new ArrayList<AppNode>();
		ArrayList<AppNode> thermalRawList = new ArrayList<AppNode>();
		for (AppNode app : eusList) {
			if (app.agentName.equals("thermal")) {
				AppNode app2 = app.copyAppNode(app);
				thermalAppList.add(app2);
				AppNode app3 = app.copyAppNode(app);
				thermalRawList.add(app3);
			}
		}
		/* thermal App list有可能是空的 */
		if (thermalAppList.size() == 0) {
			return new ArrayList<AppNode>();
		} else {
			/* 從eusList中和thermal有關的電器，找出所有的狀態排列組合 */
			Optimizer op = new Optimizer();
			/* thermal App list有可能是空的 */
			ArrayList<String> candidateList = op.buildCandidateList(thermalAppList);

			int iterateCounter = 0;
			ArrayList<AppNode> bestAnswer = null;
			while ((bestAnswer == null || bestAnswer.size() == 0) && iterateCounter < iterateLimit) {
				bestAnswer = thermalIterate(candidateList, thermalAppList, thermalRawList, gaInference, iterateCounter);
				iterateCounter++;
			}

			if (bestAnswer.size() == 0) {
				System.out.println("No best answer found for thermal control!");
				return thermalRawList;
			} else {
				return bestAnswer;
			}
		}

	}
	
	/* Calculate PMV according to each activity and record <location_activity, pmv> 
	 * According to PMV calculate optimal temperature for activity in openspace and bedroom 
	 */
	public void computePmvForEachLocation(ArrayList<AppNode> thermalRawList, GAinference gaInference) {
		Map<String, ArrayList<AppNode>> actAppList = getActAppList(thermalRawList, gaInference);
		
		// Get intensityList according to inferred activities
		ArrayList<Double> intensityList = getActIntensityList(actAppList);
		
		// Get inferred activity set
		Set<String> actSet = actAppList.keySet();
		int i = 0;
		for (String act : actSet) {
			ArrayList<AppNode> appList = actAppList.get(act);
			String location = getActLocation(act);

			double intensity = intensityList.get(i);
			double temp = getTemperature(location);
			double vel = getVelFromAppList(appList);
			double humidity = getHumidity(location);
			// Adjust temperature according to vel
			//double temp_adjusted = (vel == 0.2) ? temp : temp - vel * 2;
			
			// Calculate PMV
			PMVCalculate pc = new PMVCalculate(intensity, temp, vel, humidity);
			double pmv = pc.getPMVandPPD()[0];
			pmvEvaluationResult.put(location + "_" + act, pmv);
			i += 1;
			
			// Record optimal temperature if we want to control AC, else we put 0
			// We deal with bedroom and openspace separately
			if(pmv > initConstraint){
				tooColdFlag = false;
				int optimalTemp = (int)Math.round(pc.getMostFitTemp(initConstraint));
				if(location.equals("bedroom")) optimalTempBedroom.put(act, optimalTemp);
				else optimalTempOpenspace.put(act, optimalTemp);
			}
			// if pmv is negative, we check whether the AC's been turned on
			// if not, negative is caused by the weather, we don't turn on the AC
			// if yes, we check if the temp from environment is truly affected by AC
			else if(pmv < -initConstraint){
				if(location.equals("bedroom")){
					for(AppNode appNode : appList){
						if(appNode.appName.equals("current_AC_bedroom")){
							if(appNode.envContext.contains("on")){
								if(!tooColdFlag){
									int optimalTemp = (int)Math.round(pc.getMostFitTemp(initConstraint));
									optimalTempBedroom.put(act, optimalTemp);
								}
								else{
									optimalTempBedroom.put(act, 0);
								}
							}
							else{
								tooColdFlag = true;
								optimalTempBedroom.put(act, 0);
							}
						}
					}
				}
				else{
					for(AppNode appNode : appList){
						if(appNode.appName.equals("current_AC_livingroom")){
							if(appNode.envContext.contains("on")){
								if(!tooColdFlag){
									int optimalTemp = (int)Math.round(pc.getMostFitTemp(initConstraint));
									optimalTempOpenspace.put(act, optimalTemp);
								}
								else{
									optimalTempOpenspace.put(act, 0);
								}
							}
							else{
								tooColdFlag = false;
								optimalTempOpenspace.put(act, 0);
							}
						}
					}
				}
			}
			// pmv is in the constraint
			else{
				if(location.equals("bedroom")) optimalTempBedroom.put(act, 0);
				else optimalTempOpenspace.put(act, 0);
			}
		}
	}
	
	/* Send PMV to MQ */
	public void sendPMVToMQ(ArrayList<AppNode> thermalRawList, GAinference gaInference, Producer producer) {
		JsonBuilder json = MessageUtils.jsonBuilder();
		String location;
		String activity;
		computePmvForEachLocation(thermalRawList, gaInference);
		for (String key : pmvEvaluationResult.keySet()) {
			String[] split = key.split("_");
			location = split[0];
			activity = split[1];
			producer.sendOut(json.add("subject", "pmv").add("location", location).add("activity", activity).add("value", String.valueOf(pmvEvaluationResult.get(key))).toJson(), "ssh.CONTEXT");
		}
	}

	/* Guan-Lin 20150206
	private ArrayList<Double> relaxConstraint(Set<String> actInferResultSet, ArrayList<Double> constraintList, int counter) {

		Map<String, Integer> inferPriorityList = new HashMap<String, Integer>();

		for (String str : actInferResultSet) {
			inferPriorityList.put(str, priorityList.get(str));
		}

		List<Map.Entry<String, Integer>> listData = new ArrayList<Map.Entry<String, Integer>>(inferPriorityList.entrySet());

		Collections.sort(listData, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o2.getValue() - o1.getValue());
			}
		});

		for (int i = 0; i < actInferResultSet.size(); i++) {
			constraintList.add(initConstraint);
		}

		int i = 0;
		for (Map.Entry<String, Integer> entry : listData) {
			String key = entry.getKey();
			int j = 0;
			for (String str : actInferResultSet) {
				if (key.equals(str)) {
					break;
				}
				j++;
			}
			if (i == 0) {
				constraintList.set(j, 1 + 0.7 * counter);
			} else if (i == 1) {
				constraintList.set(j, 1 + 0.3 * counter);
			}
			i++;
		}

		return constraintList;
	}
	*/

	/* Run through all combinations and try to find solution */
	private ArrayList<AppNode> thermalIterate(ArrayList<String> candidateList, ArrayList<AppNode> thermalAppList, ArrayList<AppNode> thermalRawList, GAinference gaInference, int counter) {

		// 原始環境的pmv和power consumption
		ArrayList<Double> rawPmvList = getComfortArrayFromEnvironment(thermalRawList, gaInference);
		double rawAmp = Optimizer.calEnergyConsumption(thermalRawList);

		ThermalSolution ts = new ThermalSolution();

		//ArrayList<Double> constraintList = new ArrayList<Double>();

		// Set initialized constraint
		if (counter == 0) {
			constraintList = new ArrayList<Double>();
			for (int i = 0; i < gaInference.actInferResultSet.size(); i++) {
				constraintList.add(initConstraint);
			}
		} 
		else {
		}
		
		ArrayList<Integer> notMetConstraintList = new ArrayList<Integer>();
		
		// iterate
		for (String candidate : candidateList) {
			notMetConstraintList.clear();
			Optimizer.updateState(thermalAppList, candidate);
			
			// Get new PMV and corresponding energy consumption
			ArrayList<Double> pmvList = getComfortArray(thermalAppList, gaInference);
			double amp = Optimizer.calEnergyConsumption(thermalAppList);
			
			// 判斷pmvList是否全部都在constraint內
			boolean flag = true;
			for(int i = 0; i < pmvList.size(); i++){
				double pmv = pmvList.get(i);
				double constraint = constraintList.get(i);
				// Pmv does not fulfill the constraint
				if (Math.abs(pmv) > constraint) {
					flag = false;
					notMetConstraintList.add(i);
					//break;
				}
			}
			if(flag){
				// Add solution to ts
				ts = thermalListEvaluation(thermalAppList, pmvList, amp, ts, rawPmvList, rawAmp);
				System.out.println("candidate = " + candidate + "rawPmv = " + pmvList.get(0) + " constraint = " + constraintList);
			}
		}
		// If there is no solution, increase constraint by 0.1
		if (ts.solution.size() == 0) {
			for (Integer i : notMetConstraintList) {
				constraintList.set(i, constraintList.get(i) + incrementConstraint);
			}
		}
		return ts.solution;
	}

	/* Choose one solution with least energy consumption */
	private ThermalSolution thermalListEvaluation(ArrayList<AppNode> thermalAppList, ArrayList<Double> pmvList, double amp, ThermalSolution ts, ArrayList<Double> rawPmvList, double rawAmp) {
		if (ts.setFlag == false) {
			ts.copy(thermalAppList, pmvList, amp);
			ts.setFlag = true;
		} 
		else{
			// evaluate best answer的條件
			// if total energy consumption is less than previous answer, we change answer
			if (amp < ts.totalAmp) {
				ts.copy(thermalAppList, pmvList, amp);
			}
		}
		return ts;
	}

	class ThermalSolution {
		ArrayList<AppNode> solution = new ArrayList<AppNode>();
		ArrayList<Double> solutionPMV = new ArrayList<Double>();
		double totalAmp = 0;
		Boolean setFlag = false;
		
		public ThermalSolution(){	
		}
		
		public void copy(ArrayList<AppNode> thermalAppList, ArrayList<Double> pmvList, double amp) {
			solution.clear();
			solutionPMV.clear();
			/* Copy present thermal appliance node to solutionList */ 
			for(AppNode app : thermalAppList){
				solution.add(app.copyAppNode(app));
			}
			for(Double pmv : pmvList){
				solutionPMV.add(pmv);
			}
			totalAmp = amp;
		}
	}

}
