package util.control;

import java.util.ArrayList;

import mchess.Mchess;
import engine.esdse.Esdse;
import s2h.platform.support.JsonBuilder;
import s2h.platform.support.MessageUtils;
import util.datastructure.AppNode;
import util.datastructure.SensorNode;
import util.mq.Producer;

public class ControlAgent {
	ArrayList<AppNode> oldDecisionList = null;
	private final int sleepInterval = 3000;
	Producer producer;
	boolean controlExistence;
	JsonBuilder json = MessageUtils.jsonBuilder();
	private final int fanOnThreshold = 175;
	private boolean doorOpen = false;
	private int hallwayLightLuxTreshold = 30;
	
	public ControlAgent (Producer producer) {
		this.producer = producer;
	}
	
	public void setDecisionList(ArrayList<AppNode> decisionList) {
		oldDecisionList = new ArrayList<AppNode>(decisionList);
	}
	
	public ArrayList<AppNode> getDecisionList() {
		return oldDecisionList;
	}
	
	public boolean controlAppliance(ArrayList<AppNode> decisionList, ArrayList<AppNode> eusList){
		controlExistence = false;
		int i, j, k;
		for(i = 0; i < decisionList.size(); i++){
			AppNode app = decisionList.get(i);
			for(k = 0; k < eusList.size(); k++){
				if(eusList.get(k).appName.equals(app.appName)){
					break;
				}
			}
			AppNode eusApp = eusList.get(k);
			
			// The status of appliance is unchanged
			if(app.envContext.equals(eusApp.envContext)){
				continue; 
			}
			// Send Command to MQ according to new status of appliance
			// Light command
			if(app.appName.equals("light_livingroom")){
				String currentCentral = "off";
				String currentRing = "off";
				// If the status isn't off then get current status of central and ring light
				if(!eusApp.envContext.equals("off")){
					currentCentral = eusApp.envContext.split("_")[1];
					currentRing = eusApp.envContext.split("_")[2];
				}
				// M-CHESS decide to turn off the light
				if(app.envContext.equals("off")){
					// If light is not off then turn off the light
					if(!currentCentral.equals("off")){
						json.reset();
						json.add("value", "livingroom-central-light_0");
						sendCommand(json.add("change", "Darken"));
					}
					if(!currentRing.equals("off")){
						json.reset();
						json.add("value", "livingroom-ring-light_0");
						sendCommand(json.add("change", "Darken"));
					}
				}
				// M-CHESS decide to turn on the light
				else if(app.envContext.contains("on")){
					// Get the ideal level
					int level = Integer.valueOf(app.envContext.split("_")[1]);
					// We need to turn on central and ring light
					if(level >= 100){
						if(!currentCentral.equals("99")){
							json.reset();
							json.add("value", "livingroom-central-light_99");
							sendCommand(json.add("change", "Brighten"));
						}
						// Check whether we want to darken or brighten the ring light
						if(!currentRing.equals("off")){
							if(Integer.parseInt(currentRing) < level - 99){
								json.reset();
								json.add("value", "livingroom-ring-light_" + String.valueOf(level - 99));
								sendCommand(json.add("change", "Brighten"));
							}
							else if(Integer.parseInt(currentRing) > level - 99){
								json.reset();
								json.add("value", "livingroom-ring-light_" + String.valueOf(level - 99));
								sendCommand(json.add("change", "Darken"));
							}
						}
						else if(currentRing.equals("off")){
							json.reset();
							json.add("value", "livingroom-ring-light_" + String.valueOf(level - 99));
							sendCommand(json.add("change", "Brighten"));
						}
					}
					// We only turn on central light
					else{
						if(eusApp.envContext.equals("off")){
							json.reset();
							json.add("value", "livingroom-central-light_" + String.valueOf(level));
							sendCommand(json.add("change", "Brighten"));
						}
						else{
							if(Integer.parseInt(currentCentral) < level){
								json.reset();
								json.add("value", "livingroom-central-light_" + String.valueOf(level));
								sendCommand(json.add("change", "Brighten"));
							}
							else if(Integer.parseInt(currentCentral) > level){
								json.reset();
								json.add("value", "livingroom-central-light_" + String.valueOf(level));
								sendCommand(json.add("change", "Darken"));
							}
							if(!currentRing.equals("0")){
								json.reset();
								json.add("value", "livingroom-ring-light_0");
								sendCommand(json.add("change", "Darken"));
							}
						}
					}
				}
			} 
			else if(app.appName.equals("light_hallway")){ //mod_done
				if (app.envContext.equals("off")) {
					json.reset();
					sendCommand(json.add("value", "DOOR-LIGHT_OFF"));
				} 
				else if (app.envContext.equals("on_1")) {
					json.reset();
					sendCommand(json.add("value", "DOOR-LIGHT_ON"));
				}
			}
			else if(app.appName.equals("light_kitchen")){ //mod done
				if(app.envContext.equals("off")){
					json.reset();
					json.add("value", "kitchen-light_0");
					sendCommand(json.add("change", "Darken"));
				} 
				else if(app.envContext.contains("on")){
					int level = Integer.valueOf(app.envContext.split("_")[1]);
					// If the current state of light is off
					if(eusApp.envContext.equals("off")){
						json.reset();
						json.add("value", "kitchen-light_" + String.valueOf(level));
						sendCommand(json.add("change", "Brighten"));
					}
					else{
						if(Integer.parseInt(eusApp.envContext.split("_")[1]) < level){
							json.reset();
							json.add("value", "kitchen-light_" + String.valueOf(level));
							sendCommand(json.add("change", "Brighten"));
						}
						else if(Integer.parseInt(eusApp.envContext.split("_")[1]) > level){
							json.reset();
							json.add("value", "kitchen-light_" + String.valueOf(level));
							sendCommand(json.add("change", "Darken"));
						}
					}
				}
			}
			else if(app.appName.equals("light_study")){ //mod done
				if(app.envContext.equals("off")){
					json.reset();
					json.add("value", "study-light_0");
					sendCommand(json.add("change", "Darken"));
				} 
				else if(app.envContext.contains("on")){
					int level = Integer.valueOf(app.envContext.split("_")[1]);
					if(eusApp.envContext.equals("off")){
						json.reset();
						json.add("value", "study-light_" + String.valueOf(level));
						sendCommand(json.add("change", "Brighten"));
					}
					else{
						if(Integer.parseInt(eusApp.envContext.split("_")[1]) < level){
							json.reset();
							json.add("value", "study-light_" + String.valueOf(level));
							sendCommand(json.add("change", "Brighten"));
						}
						else if(Integer.parseInt(eusApp.envContext.split("_")[1]) > level){
							json.reset();
							json.add("value", "study-light_" + String.valueOf(level));
							sendCommand(json.add("change", "Darken"));
						}
					}
				}
			}
			else if(app.appName.equals("light_bedroom")){ //mod done
				if(app.envContext.equals("off")){
					json.reset();
					json.add("value", "bedroom-light_0");
					sendCommand(json.add("change", "Darken"));
				} 
				else if(app.envContext.startsWith("on")){
					int level = Integer.valueOf(app.envContext.split("_")[1]);
					if(eusApp.envContext.equals("off")){
						json.reset();
						json.add("value", "bedroom-light_" + String.valueOf(level));
						sendCommand(json.add("change", "Brighten"));
					}
					else{
						if(Integer.parseInt(eusApp.envContext.split("_")[1]) < level){
							json.reset();
							json.add("value", "bedroom-light_" + String.valueOf(level));
							sendCommand(json.add("change", "Brighten"));
						}
						else if(Integer.parseInt(eusApp.envContext.split("_")[1]) > level){
							json.reset();
							json.add("value", "bedroom-light_" + String.valueOf(level));
							sendCommand(json.add("change", "Darken"));
						}
					}
				}
			}
			// TV command
			else if(app.appName.equals("current_TV_livingroom")){ // mod done
				if (eusApp.envContext.equals("on") && (app.envContext.equals("standby") || app.envContext.equals("off"))) {
					json.reset();
					sendCommand(json.add("value", "TV_OFF"));
				}
			}
			// WaterColdFan command
			else if(app.appName.equals("current_watercoldfan_livingroom")){
				controlLivingRoomWaterColdFan2(app.envContext, eusApp.envContext);
			} 
			// XBOX command
			else if(app.appName.equals("current_xbox_livingroom")){ //mod done
				if (eusApp.envContext.equals("on") && (app.envContext.equals("standby") || app.envContext.equals("off"))) {
					json.reset();
					sendCommand(json.add("value", "XBOX_STOP"));
				}
			}
			// AC control command
			else if(app.appName.equals("current_AC_livingroom")){ //mod done
				if(!eusApp.envContext.startsWith("on") && app.envContext.startsWith("on")){
					json.reset();
					json.add("value", "openspace-AC_" + app.envContext);
					sendCommand(json.add("change", "Cool down"));
				}
				else if(eusApp.envContext.startsWith("on") && app.envContext.equals("off")){
					json.reset();
					json.add("value", "openspace-AC_off");
					sendCommand(json.add("change", "Warm up"));
				}
				else if(eusApp.envContext.startsWith("on") && app.envContext.startsWith("on")){
					String tempCurrent = eusApp.envContext.split("_")[1];
					String tempNew = app.envContext.split("_")[1];
					if(Integer.parseInt(tempCurrent) > Integer.parseInt(tempNew)){
						json.reset();
						json.add("value", "openspace-AC_" + app.envContext);
						sendCommand(json.add("change", "Cool Down"));
					}
					else if(Integer.parseInt(tempCurrent) < Integer.parseInt(tempNew)){
						json.reset();
						json.add("value", "openspace-AC_" + app.envContext);
						sendCommand(json.add("change", "Warm up"));
					}
				}
			}
			else if(app.appName.equals("current_AC_bedroom")){ //mod done
				if(!eusApp.envContext.equals("on") && app.envContext.startsWith("on")){
					json.reset();
					json.add("value", "bedroom-AC_" + app.envContext);
					sendCommand(json.add("change", "Cool down"));
				}
				else if(eusApp.envContext.startsWith("on") && app.envContext.equals("off")){
					json.reset();
					json.add("value", "bedroom-AC_off");
					sendCommand(json.add("change", "Warm up"));
				}
				else if(eusApp.envContext.startsWith("on") && app.envContext.startsWith("on")){
					String tempCurrent = eusApp.envContext.split("_")[1];
					String tempNew = app.envContext.split("_")[1];
					if(Integer.parseInt(tempCurrent) > Integer.parseInt(tempNew)){
						json.reset();
						json.add("value", "bedroom-AC_" + app.envContext);
						sendCommand(json.add("change", "Cool down"));
					}
					else if(Integer.parseInt(tempCurrent) < Integer.parseInt(tempNew)){
						json.reset();
						json.add("value", "bedroom-AC_" + app.envContext);
						sendCommand(json.add("change", "Warm up"));
					}
				}
			}
		}
		
		//setDecisionList(decisionList);
		// End of sending command
		if (controlExistence) {
			sendControlEndSignal();
		}
		
		return controlExistence;
	}
	
	public void sendControlStartSignal() {
		//json.reset();
		producer.sendOut(MessageUtils.jsonBuilder().add("subject", "signal").add("ack", "start").toJson(), "ssh.HCI.COMMAND.DISPLAY");
	}
	
	public void sendControlEndSignal() {
		//json.reset();
		producer.sendOut(MessageUtils.jsonBuilder().add("subject", "signal").add("ack", "end").toJson(), "ssh.HCI.COMMAND.DISPLAY");
	}
	
	public void sendCommand(JsonBuilder command) {
		if (!controlExistence) {
			sendControlStartSignal();
			controlExistence = true;
		}
		producer.sendOut(command.toJson(), "ssh.HCI.COMMAND.DISPLAY");
	}
	
	public void sendCommand(JsonBuilder command, String topic) {
		if (!controlExistence) {
			sendControlStartSignal();
			controlExistence = true;
		}
		producer.sendOut(command.toJson(), topic);
	}
	
	public void sendCommand(JsonBuilder command, String topic, boolean directContorl) {
		if (!controlExistence) {
			sendControlStartSignal();
			controlExistence = true;
		}
		producer.sendOut(command.toJson(), topic);
		try {
			Thread.sleep(sleepInterval);
		} catch (InterruptedException e) {
		}
	}
	
	public void controlLivingRoomWaterColdFan2(String newState, String oldState) {
		if (oldState.startsWith("on") && (newState.equals("standby") || newState.equals("off"))) {
			json.reset();
			sendCommand(json.add("value", "WATER_COLD_FAN_FOG"));
			json.reset();
			sendCommand(json.add("value", "WATER_COLD_FAN_TURN"));
			json.reset();
			sendCommand(json.add("value", "WATER_COLD_FAN_STOP"));
		} 
		else if (newState.startsWith("on")) {
			if (oldState.equals("standby") || oldState.equals("off")) {
				json.reset();
				sendCommand(json.add("value", "WATER_COLD_FAN_START"));
				json.reset();
				sendCommand(json.add("value", "WATER_COLD_FAN_FOG"));
				json.reset();
				sendCommand(json.add("value", "WATER_COLD_FAN_TURN"));
			}
		}
	}
	
	/* TODO Integrate with new light system */
	public void controlBedRoomLight(String newState, String oldState) {
		if (newState.equals("off")) {
			json.reset();
			sendCommand(json.add("value", "LIVINGROOM-LIGHT_OFF"));
		} else if (newState.equals("on_1")) {
			if (oldState.equals("off")) {
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_TWO_ON"));
			} else if (oldState.equals("on_2")) {
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_TWO_ON"));
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_THREE_OFF"));
			} else if (oldState.equals("on_3")) {
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_THREE_OFF"));
			}
		} else if (newState.equals("on_2")) {
			if (oldState.equals("off")) {
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_THREE_ON"));
			} else if (oldState.equals("on_1")) {
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_THREE_ON"));
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_TWO_OFF"));
			} else if (oldState.equals("on_3")) {
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_TWO_OFF"));
			}
		} else if (newState.equals("on_3")) {
			if (oldState.equals("off")) {
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_TWO_ON"));
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_THREE_ON"));
			} else if (oldState.equals("on_1")) {
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_THREE_ON"));
			} else if (oldState.equals("on_2")) {
				json.reset();
				sendCommand(json.add("value", "LIVINGROOM-LIGHT_TWO_ON"));
			}
		}
	}
	
	public void turnOnStandbyPower() {
		json.reset();
		producer.sendOut(json.add("subject", "controlmeter").add("type", "allopen").toJson(), "ssh.COMMAND");
	}
	
	public void turnOffStandbyPower(ArrayList<String> exceptionID) {
		String excpetionIDString = "";
		json.reset();
		if (exceptionID != null) {
			for (String id : exceptionID) {
				excpetionIDString = excpetionIDString.concat(id + " ");
			}
			excpetionIDString = excpetionIDString.trim().replace(' ', ',');
			producer.sendOut(json.add("subject", "controlmeter").add("type", "allclose").add("exception", excpetionIDString).toJson(), "ssh.COMMAND");
		} 
		else {
			producer.sendOut(json.add("subject", "controlmeter").add("type", "allclose").toJson(), "ssh.COMMAND");
		}
	}
	
	/* Rule-based check ComeBack and GoOut */
	public void checkComeBackAndGoOut(SensorNode sensorNode,boolean ToBeGoOut,int OldNumOfPeople,boolean standbyAllOn) {
		if(sensorNode.name.equals("switch_door_hallway") && sensorNode.discreteValue.equals("on")){
			// Door is opend by someone, HallwayActivity happens
			if (!doorOpen) doorOpen = true;
			json.reset();
			producer.sendOut(json.add("subject", "activity").add("name", "HallwayActivity").add("GA", "g1-6").toJson(), "ssh.CONTEXT");
			
			//controlAgent.sendControlStartSignal();
			// If door's open and illumination isn't enough then turn on hallway light
			// TODO: Do we really need to check doorOpen?
			if((Mchess.illuminationReading.get("hallway") < hallwayLightLuxTreshold) && doorOpen){
				json.reset();
				producer.sendOut(json.add("value", "DOOR-LIGHT_ON").toJson(), "ssh.COMMAND");
		    }
			
			// ComeBack and turn on standby power 
			// 1. Not going out
			// 2. There's no one in the house
			// 3. Standby power is all off
		    if((ToBeGoOut == false) && (OldNumOfPeople == 0) && (standbyAllOn == false)){
		    	System.err.println("ToBeGoOut: " + ToBeGoOut);
		    	System.err.println("Come Back! Turn on all standy power!");
		    	turnOnStandbyPower();
		    	standbyAllOn = true; 
		    }
		}
		else if(ToBeGoOut == true && sensorNode.name.equals("switch_door_hallway") && sensorNode.discreteValue.equals("off")){
			System.err.println("ToBeGoOut: " + ToBeGoOut);
	    	System.err.println("Go Out! Turn on all standy power!");
			if(doorOpen) doorOpen = false;
			
			// Send GoOut information to MQ
			json.reset();
			producer.sendOut(json.add("subject", "activity").add("name", "GoOut").add("GA", "g1-7").toJson(), "ssh.CONTEXT");
			
			// Turn off all the standby power
			turnOffStandbyPower(null);
			
			// Turn off all lights
			json.reset();
			producer.sendOut(json.add("value", "ALL_OFF").toJson(), "ssh.COMMAND");
			json.reset();
			producer.sendOut(json.add("value", "livingroom-central-light_0").toJson(), "ssh.COMMAND");
			json.reset();
			producer.sendOut(json.add("value", "livingroom-ring-light_0").toJson(), "ssh.COMMAND");
			json.reset();
			producer.sendOut(json.add("value", "kitchen-light_0").toJson(), "ssh.COMMAND");
			json.reset();
			producer.sendOut(json.add("value", "study-light_0").toJson(), "ssh.COMMAND");
			json.reset();
			producer.sendOut(json.add("value", "bedroom-light_0").toJson(), "ssh.COMMAND");
			ToBeGoOut = false;
			standbyAllOn = false;
		}
	}
}
