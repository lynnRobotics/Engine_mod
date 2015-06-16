package engine.esdse;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mchess.Mchess;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


public class EsdseXMLHandler {
	
	File xml = new File("./_input_data/esdseInitilization.xml");
	
	public EsdseXMLHandler(){
		xml = new File(Mchess.esdseInitializationPath);
	}
	
	public int getSensorCount(){
		int sensorCount = 6;
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/sensorCount");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				String curString = curElement.element("value").getText();
  				sensorCount = Integer.parseInt(curString);
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return sensorCount;
	}
	
	public int getMaxNoSignalDuration(){
		int maxNoSignalDuration = 45;
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/maxNoSignalDuration");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				String curString = curElement.element("value").getText();
  				maxNoSignalDuration = Integer.parseInt(curString);
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return maxNoSignalDuration;
	}
	
	public double getStableDuration(){
		double stableDuration = 5;
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/stableDuration");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				String curString = curElement.element("value").getText();
  				stableDuration = Double.parseDouble(curString);
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return stableDuration;
	}

}
