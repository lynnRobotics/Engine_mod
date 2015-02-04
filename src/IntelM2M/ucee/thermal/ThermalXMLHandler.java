package IntelM2M.ucee.thermal;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import IntelM2M.mchess.Mchess;

public class ThermalXMLHandler {

	File xml = new File("./_input_data/thermalInitilization.xml");
	
	public ThermalXMLHandler(){
		xml= new File(Mchess.thermalInitializationPath);
	}
	
	public double getInitConstraint(){
		double initConstraint = 1.0;
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/initConstraint");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				String curString = curElement.element("value").getText();
  				initConstraint = Double.parseDouble(curString);
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return initConstraint;
	}
	
	public boolean getTooColdFlag(){
		boolean tooColdFlag = false;
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/tooColdFlag");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				String curString = curElement.element("value").getText();
  				tooColdFlag = Boolean.parseBoolean(curString);
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return tooColdFlag;
	}
	
	public int getIterateLimit(){
		int iterateLimit = 20;
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/iterateLimit");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				String curString = curElement.element("value").getText();
  				iterateLimit = Integer.parseInt(curString);
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return iterateLimit;
	}
	
	public double getIncrementConstraint(){
		double incrementConstraint = 0.1;
		try{
  			SAXReader saxReader = new SAXReader();
  			Document document = saxReader.read(xml);
  			List list = document.selectNodes("/metaData/incrementConstraint");
  			Iterator iter = list.iterator();
  			while(iter.hasNext()){
  				Element curElement = (Element)iter.next();
  				String curString = curElement.element("value").getText();
  				incrementConstraint = Double.parseDouble(curString);
  			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return incrementConstraint;
	}
	
}
