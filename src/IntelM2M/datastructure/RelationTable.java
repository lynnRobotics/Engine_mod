package IntelM2M.datastructure;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RelationTable{

	
	
	public ArrayList<AppNode> appList;
	public double intensity;
	
	public RelationTable(){
		appList = new ArrayList<AppNode>();
		intensity = 0.0;
	}
	
}