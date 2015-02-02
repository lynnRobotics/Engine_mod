package IntelM2M.ucee.thermal;

import java.io.File;

public class ThermalXMLHandler {

	File xml = new File("./_input_data/thermalInitilization.xml");
	
	public ThermalXMLHandler(String thermalXMLPath){
		xml= new File(thermalXMLPath);
	}
	
}
