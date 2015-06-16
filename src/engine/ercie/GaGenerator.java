package engine.ercie;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import util.algo.Classifier;
import util.datastructure.AppNode;
import util.datastructure.EnvStructure;
import util.datastructure.GroupActivity;
import util.datastructure.RelationTable;
import util.func.CrossValidate;
import weka.classifiers.bayes.net.EditableBayesNet;
import weka.core.Instances;

/*Mao Yuan Weng 2012/01*/

public class GaGenerator {

	ErcieXMLHandler ercieXMLHandler;
	
	public Map<String, GroupActivity> gaList = new LinkedHashMap<String, GroupActivity>();
	public Instances insts;
	public int level;

	public GaGenerator() {
		try {
			ercieXMLHandler = new ErcieXMLHandler();
			File dir = new File("./_weka_training_data");
			insts = new Instances(new FileReader("./_weka_training_data/"
					+ dir.list()[0]));
		} catch (Exception ex) {
		}
	}

	public GaGenerator(int level) {
		try {
			ercieXMLHandler = new ErcieXMLHandler();
			File dir = new File("./_weka_training_data");
			insts = new Instances(new FileReader("./_weka_training_data/" + dir.list()[0]));
		} catch (Exception ex) {
		}
		this.level = level;
	}

	public void buildFirstGaList() {
		String[] activityList = (String[]) EnvStructure.activityList.toArray(new String[0]);
		int i = 0;
		for (String str : activityList) {
			GroupActivity ga = new GroupActivity("g1-" + Integer.toString(i + 1));
			ga.actMemberList.add(activityList[i]);
			gaList.put(ga.GID, ga);
			i++;
		}
	}

	public void buildGaList() {
		gaList = ercieXMLHandler.getBuildGaList();
	}

	public double[] findsMatrixMin(String[] lastActivityList, double[][] sMatrix) {
		double min = 9999;
		double[] minArr = new double[3];
		Boolean allZeroFlag = true;
		for (int i = 0; i < lastActivityList.length; i++) {
			for (int j = 0; j < lastActivityList.length; j++) {
				if (sMatrix[i][j] < min && sMatrix[i][j] > 0) {
					min = sMatrix[i][j];
					minArr[1] = i;
					minArr[2] = j;
				}
				if (sMatrix[i][j] != 0) {
					allZeroFlag = false;
				}
			}
		}
		if (allZeroFlag)
			minArr[0] = 0;
		else
			minArr[0] = min;
		return minArr;

	}

	public double findsMatrixMax(String[] lastActivityList, double[][] sMatrix) {
		double max = 0;

		for (int i = 0; i < lastActivityList.length; i++) {
			for (int j = 0; j < lastActivityList.length; j++) {
				if (sMatrix[i][j] > max && sMatrix[i][j] > 0)
					max = sMatrix[i][j];
			}
		}
		return max;

	}

	

	public Boolean checkSameService(String gaName1, String gaName2,
			Map<String, RelationTable> actAppList) {
		Boolean same = false;
		ArrayList<AppNode> gaAppList1 = actAppList.get(gaName1).appList;
		ArrayList<AppNode> gaAppList2 = actAppList.get(gaName2).appList;
		for (AppNode aNode1 : gaAppList1) {
			for (AppNode aNode2 : gaAppList2) {
				if (aNode1.appName.equals(aNode2.appName)
						&& aNode1.state.equals(aNode2.state)
						&& aNode1.escType.equals(aNode2.escType)) {
					if (aNode1.confidence > 0.9 && aNode2.confidence > 0.9) {
						same = true;
					}
				}
			}
		}

		return same;
	}

	public Boolean buildHGA(Classifier DBN, GaGenerator lastGA,
			GaEscGenerator lastGAESC, Boolean retrain) {
		/* 找出上一個gaList */
		Set<String> keys = lastGA.gaList.keySet();
		String[] lastGAList = (String[]) keys.toArray(new String[0]);
		/* 根據 classifiers來算smatrix */
		int round = CrossValidate.cvRound;
		double sMatrix[][] = buildSMatrix("./_output_results/sMatrix/round"
				+ round + "_sMatrix_" + (level - 1) + ".txt", DBN, lastGAList,
				retrain);
		/* 根據smatrix來build gaList */
		int gaIndex = 0;/* 第n個ga */

		Boolean continueFlag = true;
		while (continueFlag) {
			double minArr[] = findsMatrixMin(lastGAList, sMatrix);
			double min = minArr[0];
			int i = (int) minArr[1];
			int j = (int) minArr[2];
			/* 檢查 i j是否有共同的 explicit/implict */
			Map<String, RelationTable> actAppList = lastGAESC.actAppList;
			String gaName1 = lastGAList[i];
			String gaName2 = lastGAList[j];
			Boolean sameService = checkSameService(gaName1, gaName2, actAppList);
			/* 第二層才開始合併 */
			if (level != 1 && min != 0) {
				if (sameService) {
					GroupActivity ga = new GroupActivity("g"
							+ Integer.toString(level) + "-"
							+ Integer.toString(gaIndex + 1));
					ArrayList<String> memberList1 = lastGA
							.getGroupMember(lastGAList[i]);
					ArrayList<String> memberList2 = lastGA
							.getGroupMember(lastGAList[j]);
					for (String str : memberList1) {
						/* 加入合併前該ga所有的 activity */
						if (!ga.actMemberList.contains(str)) {
							ga.actMemberList.add(str);
						}
					}
					for (String str : memberList2) {
						if (!ga.actMemberList.contains(str)) {
							ga.actMemberList.add(str);
						}
					}
					gaList.put(ga.GID, ga);
					/* 登記加過的activity */
					sMatrix[i][i] = -1;
					sMatrix[j][j] = -1;
					gaIndex += 1;
					continueFlag = false;
				}
				sMatrix[i][j] = 0;
				sMatrix[j][i] = 0;

			} else if (level != 1 && min == 0) {
				continueFlag = false;
			}

		}
		/* 沒有任何相似的activity */
		if (gaList.size() == 0) {
			return false;
		} else {
			/* 沒被group到的也加入ga_list */
			for (int i = 0; i < lastGAList.length; i++) {
				Boolean existInGroup = false;
				for (int j = 0; j < lastGAList.length; j++) {
					if (i == j && sMatrix[i][j] < 0) {
						existInGroup = true;
					}
				}
				if (!existInGroup) {
					GroupActivity ga = new GroupActivity("g"
							+ Integer.toString(level) + "-"
							+ Integer.toString(gaIndex + 1));
					ArrayList<String> memberList1 = lastGA
							.getGroupMember(lastGAList[i]);
					for (String str : memberList1) {
						if (!ga.actMemberList.contains(str)) {
							ga.actMemberList.add(str);
						}
					}
					gaList.put(ga.GID, ga);
					gaIndex += 1;
				}
			}
			return true;
		}
	}

	static public void writeHGA(String hgaPath,
			ArrayList<GaGenerator> GaGeneratorList) {
		try {
			FileWriter writer = new FileWriter(new File(hgaPath), false);
			for (int i = 0; i < GaGeneratorList.size(); i++) {
				Map<String, GroupActivity> gaList = GaGeneratorList.get(i).gaList;
				Set<String> keys = gaList.keySet();
				for (String str : keys) {
					writer.write(str + " :");
					ArrayList<String> memberList = gaList.get(str).actMemberList;
					for (String str2 : memberList) {
						writer.write(str2 + ", ");
					}
					writer.write("\r\n");
					writer.flush();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public double[][] buildSMatrix(String smPath, Classifier DBN,
			String[] activityList, Boolean retrain) {
		EditableBayesNet[] classifiers = DBN.buildARModelwithAllFeature(
				activityList, retrain);
		double sMatrix[][] = new double[activityList.length][activityList.length];
		for (int i = 0; i < activityList.length; i++) {
			for (int i2 = 0; i2 < activityList.length; i2++) {
				EditableBayesNet classifier = classifiers[i];

				EditableBayesNet classifier2 = classifiers[i2];

				double dis = calDistanceForClassifier(classifier, classifier2);
				sMatrix[i][i2] = dis;
			}
		}
		/* print result */
		try {
			FileWriter writer = new FileWriter(new File(smPath), false);
			for (int i = 0; i < activityList.length; i++) {
				for (int j = 0; j < activityList.length; j++) {
					writer.write(sMatrix[i][j] + " ");
				}
				writer.write("\r\n");
				writer.flush();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return sMatrix;

	}

	private double calDistanceForClassifier(EditableBayesNet classifier,
			EditableBayesNet classifier2) {
		HashSet<String> nodeList = new HashSet<String>();

		/* 取兩個classifier node的聯集 */
		for (int i = 0; i < classifier.getNrOfNodes() - 1; i++) {
			String node = classifier.getNodeName(i);
			nodeList.add(node);
		}
		for (int i = 0; i < classifier2.getNrOfNodes() - 1; i++) {
			String node = classifier2.getNodeName(i);
			nodeList.add(node);
		}
		/* 計算每個node的distance */

		double totalDist = 0;
		for (String node : nodeList) {

			int stateNum = insts.attribute(node).numValues();
			for (int k = 0; k < stateNum; k++) {
				int iNode1 = classifier.getNode2(node);
				/* get probability if node exist */
				double pe1;
				if (iNode1 == -1) {
					pe1 = 0;
				} else {
					pe1 = classifier.getProbability(iNode1, 1, k);
				}
				int iNode2 = classifier2.getNode2(node);
				double pe2;
				if (iNode2 == -1) {
					pe2 = 0;
				} else {
					pe2 = classifier2.getProbability(iNode2, 1, k);
				}
				totalDist += Math.pow(pe1 - pe2, 2);
			}

		}
		Math.pow(totalDist, 0.5);
		return totalDist;
	}

	public ArrayList<String> getGID(String activity) {
		Collection<GroupActivity> collection = gaList.values();
		String GID = null;
		ArrayList<String> gidArr = new ArrayList<String>();
		Iterator iterator = collection.iterator();
		while (iterator.hasNext()) {
			GroupActivity ga = (GroupActivity) iterator.next();
			for (String str : ga.actMemberList) {
				if (str.equals(activity)) {
					GID = ga.GID;
					gidArr.add(GID);
				}
			}
		}
		return gidArr;
	}

	public ArrayList<String> getGroupMember(String GID) {
		Collection<GroupActivity> collection = gaList.values();
		Iterator iterator = collection.iterator();
		while (iterator.hasNext()) {
			GroupActivity ga = (GroupActivity) iterator.next();
			if (ga.GID.equals(GID)) {
				return ga.actMemberList;
			}
		}
		return null;
	}

}
