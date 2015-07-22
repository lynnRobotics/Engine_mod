package mchess;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class MchessXMLHandler {

	File xml = new File("./_input_data/mchessInitilization.xml");
	
	public MchessXMLHandler(){
		xml= new File(Mchess.mchessInitializationPath);
	}
	
	
}
