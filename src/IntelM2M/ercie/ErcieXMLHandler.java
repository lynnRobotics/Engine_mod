package IntelM2M.ercie;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import IntelM2M.datastructure.GroupActivity;

public class ErcieXMLHandler {
	
	static public String ercieInitializationPath;
	File xml = new File("./_input_data/ercielInitilization.xml");
	
	public ErcieXMLHandler(){
		xml= new File(ercieInitializationPath);
	}
	
	public String getRawTrainingDataPath(){
		String rawTrainingDataPath = "./_input_data/trainingData.txt";
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/rawTrainingDataPath");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				rawTrainingDataPath = curElement.element("value").getText();
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return rawTrainingDataPath;
	}

	public double getThreshold(){
		double threshold = 0.1;
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/threshold");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				String curString = curElement.element("value").getText();
  				threshold = Double.parseDouble(curString);
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return threshold;
	}
	
	public Boolean getRetrain(){
		Boolean retrain = false;
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/retrain");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				String curString = curElement.element("value").getText();
  				retrain = Boolean.parseBoolean(curString);
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return retrain;
	}
	
	public int getTrainLevel(){
		int trainLevel = 20;
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/trainLevel");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				String curString = curElement.element("value").getText();
  				trainLevel = Integer.parseInt(curString);
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return trainLevel;
	}
	
	public Map<String, GroupActivity> getBuildGaList(){
		Map<String, GroupActivity> gaList = new LinkedHashMap<String, GroupActivity>();
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/gaList");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				String id = curElement.attributeValue("id");
  				String activityName = curElement.element("activityName").getText();
  				GroupActivity ga = new GroupActivity(id);
  				ga.actMemberList.add(activityName);
  				gaList.put(ga.GID, ga);
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return gaList;
	}
	
}
