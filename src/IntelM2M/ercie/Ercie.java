package IntelM2M.ercie;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import s2h.platform.node.Sendable;
import IntelM2M.ercie.classifier.GaDbnClassifier;
import IntelM2M.ercie.erc.GaEscGenerator;
import IntelM2M.func.text2Arff;

/**
 * ERCIE (originally EPCIE)
 * 
 * @author Mao (2012.06)
 */

public class Ercie {

	ErcieXMLHandler ercieXMLHandler;
	
	/* Some GA related information (I don't really understand)*/
	public ArrayList<GaGenerator> GaGeneratorList;
	public ArrayList<GaDbnClassifier> GaDbnList;
	public ArrayList<GaEscGenerator> GaEscList;
	
	/* Used for inferring GA */
	public GAinference gaInference;
	
	public ArrayList<String> currentActInferResultSet = new ArrayList<String>();
	public ArrayList<String> previousActInferResultSet = new ArrayList<String>();
	public ArrayList<String> previousActInferResultSetForReject = new ArrayList<String>();
	
	public Date startTime = new Date();
	public double duration;				// used in Esdse
	
	/* modularize */
	public double threshold = 5;        // used in Esdse       // unit is second 
	static Boolean retrain = false;     // Retrain or not
	static int trainLevel = 3;          // Level of group activity
	String rawTrainingDataPath = "./_input_data/ercielInitilization.xml"; // For real-time usage
	/* modularize */

	public Ercie() {
		ercieXMLHandler = new ErcieXMLHandler();
		threshold = ercieXMLHandler.getThreshold();
		retrain = ercieXMLHandler.getRetrain();
		trainLevel = ercieXMLHandler.getTrainLevel();
		rawTrainingDataPath = ercieXMLHandler.getRawTrainingDataPath();
		
		System.out.println("...using training threshold: "+Double.toString(threshold));
		
		GaGeneratorList = new ArrayList<GaGenerator>();  // Record GA structure 
		GaDbnList = new ArrayList<GaDbnClassifier>();    // Record DBN for each GA 
		GaEscList = new ArrayList<GaEscGenerator>();     // Record ERC for each GA 
	}

	/* Train and build the model (I don't really understand)*/
	public void buildModel() {
		// build i layer GA
		for (int i = 1; i <= trainLevel; i++) {
			GaGenerator GA = new GaGenerator(i);  // Multiple GA at layer i
			
			if (i == 1) GA.buildFirstGaList();
			else {
				Boolean flag = GA.buildHGA(GaDbnList.get(i - 2), GaGeneratorList.get(i - 2), GaEscList.get(i - 2), retrain);
				if (!flag) break;
			}
			
			// Convert training data for GA at layer i
			text2Arff.convertGaRawToArff(GA, rawTrainingDataPath);

			// Build GA model
			GaDbnClassifier GaDBN = new GaDbnClassifier();
			GaDBN.buildGaModel(GA, retrain);

			// Build GA�@ERC�@model
			GaEscGenerator GAESC = new GaEscGenerator(GA, retrain);
			if (i == 1)
				GAESC.buildAllESC(GaDBN.classifier, "./_output_results/ESC/_ga_esc_" + i + ".txt", GA, null, null);
			else
				GAESC.buildAllESC(GaDBN.classifier, "./_output_results/ESC/_ga_esc_" + i + ".txt", GA, GaGeneratorList.get(0), GaEscList.get(0));

			GaDBN.allSetDefaultValue(GA);

			// Record ERC, ith HGA, classifier
			GaGeneratorList.add(GA);
			GaDbnList.add(GaDBN);
			GaEscList.add(GAESC);
		}
		// Write the structure of HGA (Hierarchical Group Activity)
		GaGenerator.writeHGA("./_output_results/hga.txt", GaGeneratorList);
	}

	/* GA inference for simulator */
	public void gaInferenceForSimulator(String read) {
		gaInference = new GAinference(GaGeneratorList, GaDbnList, GaEscList, read);
		gaInference.buildInferResult();
	}

	/* GA inference for real-time */
	public void gaInferenceForRealTime(Map<String, String> sensorReading) {
		gaInference = new GAinference(GaGeneratorList, GaDbnList, GaEscList);
		gaInference.buildInferResultForRealTime_New(sensorReading);
	}
	
	/* GA inference for real-time (new version), Human number is now considered */
	public void gaInferenceForRealTime_new(Map<String, String> sensorReading, int humanNumber) {
		gaInference = new GAinference(GaGeneratorList, GaDbnList, GaEscList, humanNumber);
		gaInference.buildInferResultForRealTime_New(sensorReading);
	}
}
