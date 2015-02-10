

package IntelM2M.func;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import IntelM2M.algo.GaEtcGenerator;
import IntelM2M.datastructure.ExpResult;
//import IntelM2M.ercie.Epcieold;
import IntelM2M.ercie.GaGenerator;
import IntelM2M.ercie.classifier.GaDbnClassifier;

public class CrossValidate {
	
	/*input*/
	String cvAllDataPath="./_input_data/CrossValidate/cv_all_data3.txt";
	/*output*/
	String cvTestDataPath="./_input_data/CrossValidate/cvTmp/cv_test_data.txt";
	String cvTrainingDataPath="./_input_data/CrossValidate/cvTmp/cv_training_data.txt";
	String cvResultPath="./_output_results/cv_result.txt";
	static int crossParameter=4;
	public static int cvRound=0;
	

	public CrossValidate(){}
}
