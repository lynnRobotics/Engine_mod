package IntelM2M.mchess;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import s2h.platform.support.MessageUtils;

/* Import necessary package */
import IntelM2M.ercie.Ercie;
import IntelM2M.ercie.ErcieXMLHandler;
import IntelM2M.ercie.GaGenerator;
import IntelM2M.esdse.Esdse;
import IntelM2M.mq.Consumer;
import IntelM2M.ucee.thermal.ThermalXMLHandler;

/**
 * MCHESS
 * 
 * @author Mao (2012.06)
 */

public class Mchess extends Consumer implements Runnable {

	public static String ercieInitializationPath = "./_input_data/ercieInitilization.xml";
	public static String thermalInitializationPath = "./_input_data/thermalInitilization.xml";
	public static String environmentInitializationPath = "./_input_data/environmentInitialization.xml";
	
	/* Two main engine */
	static Ercie ercie = null;
	static Esdse esdse;
	
	/* Different kinds of reading */ // Were in ESDSE, but UCEE and ESDSE both need it
	public static Map<String, String> sensorReading = new LinkedHashMap<String, String>();
	public static Map<String, Double> ezMeterAmpereReading = new LinkedHashMap<String, Double>();
	public static Map<String, Double> temperatureReading = new LinkedHashMap<String, Double>();
	public static Map<String, Double> humidityReading = new LinkedHashMap<String, Double>();
	public static Map<String, Double> illuminationReading = new LinkedHashMap<String, Double>();
	
	
	s2h.platform.support.JsonBuilder json = MessageUtils.jsonBuilder();
	//private Logger log = Logger.getLogger(Mchess.class.getName());

	public Mchess(String mode) {
		super();               // Consumer's constructor
		esdse = new Esdse();   // Initialization process
		/* Set up MQ information */
		this.setURL("tcp://140.112.49.154:61616");
		this.setTopic("ssh.RAW_DATA");
		this.connect();
		this.listen();
		
		if(mode.equals("-run")) {
			realTimeSysProc();     // Call build model function in epcie
		}
	}

	/* For real-time usage */
	public void realTimeSysProc() {
		ercie.buildModel();
	}
	
	/* MQ message process function */
	@Override
	public void processMsg(String m) {
		esdse.processMQMessage(ercie, m);
	}
	
	/* Define what M-CHESS thread should do */ 
	@Override
	public void run() {
		while(true){
			/* If thread doesn't start then try to start again */
			if(!this.isStarted()) {
				this.start();
				this.listen();
				esdse.backToLive = true;
				esdse.signal = false;
			} 
			/* Thread starts, send out the start signal */
			else if (esdse.backToLive) {
				json.reset();
				esdse.producer.sendOut(json.add("subject", "signal").add("current_resend", "start").toJson(), "ssh.RAW_DATA");
			}
			/* Try to sleep one second */
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		
		if (args.length < 1) {
			System.out.println("please add the argument!\ne.g. [mchess] -run or [mchess] -learn");
			return; // Test necessary argument from input
		}
		
		if(args[0].equals("-run")) {
			System.out.println("===Running Mode===");
			ercie = new Ercie();  // New a ercie object // training threshold is initialized in ercieXMLHandler.xml
			new Mchess(args[0]).run();          // New a M-CHESS object and start the thread
		}
		else if(args[0].equals("-learn")) {
			System.err.println("===Learning Mode===");
			new Mchess(args[0]).run();          // New a M-CHESS object and start the thread
		}
	}
}
