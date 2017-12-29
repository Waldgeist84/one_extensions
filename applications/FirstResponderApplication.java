/* 
 * Copyright 2015 Technische Universität Ilmenau, KN
 * Released under GPLv3. See LICENSE.txt for details. 
 */

package applications;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import report.FirstResponderAppReporter;
import core.Application;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.World;

/**
 * Simple application to model the communication behavior of first responders.
 * 
 * The corresponding <code>FirstResponderAppReporter/code> class can be used to record
 * information about the application behavior.
 * 
 * @see FirstResponderAppReporter
 * @author sk
 */
public class FirstResponderApplication extends Application {
	
	private final int serial;
	
	/** Run in passive mode - don't generate messages but respond */
	public static final String FR_PASSIVE = "passive";
	
	/** Message generation interval */
	public static final String FR_INTERVAL = "interval";
	/** Ping interval offset - avoids synchronization of message sending */
	public static final String FR_OFFSET = "offset";
	
	/** Destination address range - inclusive lower, exclusive upper */
	public static final String FR_PARENTS  = "parents";
	/** Destination address range - inclusive lower, exclusive upper */
	public static final String FR_CHILDREN = "children";
	
	/** Seed for the app's random number generator */
	public static final String FR_SEED = "seed";
	/** Size of the message */
	public static final String FR_SIZE = "size";
	
	/** Size of the message */
	public static final String FR_FWDRATIO = "fwdratio";
	/** Size of the message */
	public static final String FR_FWDRATIOSUBSET = "fwdratiosubset";
	
	/** Application ID */
	public static final String APP_ID = "edu.tui.kn.FirstResponderApplication";
	
	// Private vars
	private double	lastMessage = 0;
	private double	interval = 500;
	private double	fwdRatio = 0.3;
	private double	fwdRatioSubset = 0.4;
	private boolean passive = false;
	private int		seed = 0;
	private List<String> parents = new ArrayList<String>();
	private int		parentMin=0;
	private int		parentMax=0;
	private List<String> children = new ArrayList<String>();
	private int		childMin=0;
	private int		childMax=0;
	private int		messageSize=1;
	private Random	rng;
	private Random	fwrng;
	
	static private int nextSerial = 1;
	
	static public synchronized int nextSerialNumber() {
		return nextSerial++;
	}
	
	/** 
	 * Creates a new ping application with the given settings.
	 * 
	 * @param s	Settings to use for initializing the application.
	 */
	public FirstResponderApplication(Settings s) {
		serial = nextSerialNumber();
		
		if (s.contains(FR_PASSIVE)){
			this.passive = s.getBoolean(FR_PASSIVE);
		}
		if (s.contains(FR_INTERVAL)){
			this.interval = s.getDouble(FR_INTERVAL);
		}
		if (s.contains(FR_OFFSET)){
			// allow 5 sec offset between members of a group
			this.lastMessage = s.getDouble(FR_OFFSET)+serial*5;
		}
		if (s.contains(FR_SEED)){
			this.seed = s.getInt(FR_SEED);
		}
		if (s.contains(FR_SIZE)) {
			this.messageSize = s.getInt(FR_SIZE);
		}
		if (s.contains(FR_FWDRATIO)) {
			this.fwdRatio = s.getDouble(FR_FWDRATIO);
//			System.out.println(fwdRatio);
		}
		if (s.contains(FR_FWDRATIOSUBSET)) {
			this.fwdRatioSubset = s.getDouble(FR_FWDRATIOSUBSET);
//			System.out.println(fwdRatioSubset);
		}
		if (s.contains(FR_PARENTS)){
//			int[] destination = s.getCsvInts(FR_PARENTS,2);
//			this.parentMin = destination[0];
//			this.parentMax = destination[1];

			String parents = s.getSetting(FR_PARENTS);
			if(parents != null){
				StringTokenizer tokens = new StringTokenizer(parents,",");
				while(tokens.hasMoreTokens()){
					this.parents.add(tokens.nextToken());
				}
				
				this.parentMin = 0;
				this.parentMax = this.parents.size() - 1;
//				System.out.println(parents);
//				System.out.println(this.parents.get(0));
//				System.out.println(parentMin + " - " + parentMax);
			}
		}
		if (s.contains(FR_CHILDREN)){
//			int[] destination = s.getCsvInts(FR_CHILDREN,2);
//			this.childMin = destination[0];
//			this.childMax = destination[1];
			
			String nodes = s.getSetting(FR_CHILDREN);
			if(nodes != null){
				StringTokenizer tokens = new StringTokenizer(nodes,",");
				while(tokens.hasMoreTokens()){
					this.children.add(tokens.nextToken());
				}
				
				this.childMin = 0;
				this.childMax = this.children.size() - 1;
			}
//			System.out.println(nodes);
//			System.out.println(childMin + " - " + childMax);
		}
		
		rng = new Random(this.seed*serial);
		fwrng=new Random(this.seed*serial + 1000);
		super.setAppID(APP_ID);
	}
	
	/** 
	 * Copy-constructor
	 * 
	 * @param a
	 */
	/**
	 * @param a
	 */
	public FirstResponderApplication(FirstResponderApplication a) {
		super(a);
		
		serial = nextSerialNumber();
		
		this.lastMessage = a.getLastMessage();
		this.interval = a.getInterval()+serial*5;	// check whether numbers build up (e.g. 1 for first, 2 for second...)
		this.passive = a.isPassive();
		this.parents = a.getParents();
		this.parentMax = a.getParentMax();
		this.parentMin = a.getParentMin();
		this.children = a.getChildren();
		this.childMax = a.getChildMax();
		this.childMin = a.getChildMin();
		this.seed = a.getSeed();
		this.fwdRatio = a.getFwdRatio();
		this.fwdRatioSubset = a.getFwdRatioSubset();
		this.messageSize = a.getMessageSize();
		this.rng = new Random(this.seed*serial);
		this.fwrng=new Random(this.seed*serial + 1000);
	}
	
	/** 
	 * Handles an incoming message. If the message is a ping message replies
	 * with a pong message. Generates events for ping and pong messages.
	 * 
	 * @param msg	message received by the router
	 * @param host	host to which the application instance is attached
	 */
	@Override
	public Message handle(Message msg, DTNHost host) {
		String type = (String)msg.getProperty("fr_type");
		if (type==null) return msg; // Not a ping/pong message
		
		// we're the recipient
		if (msg.getTo()==host) {
			
			float forwardingIndex = fwrng.nextFloat();
//			System.out.println("type " + type + " ratio " + fwdRatio + " forwardingIndex " + forwardingIndex );
			
			
			
			if (type.equalsIgnoreCase("report") && !(parentMin == 0 && parentMax == 0)){
				
//				System.out.println("intermediate node bottom-up");
				
				if (forwardingIndex < fwdRatio){
//					System.out.println("forward to parent ");
					
					String id = "command" + SimClock.getIntTime() + "-" + 
							host.getAddress();
					Message m = new Message(host, randomParent(), id, getMessageSize());	
					m.addProperty("fr_type", "report");
					m.setAppID(APP_ID);
					host.createNewMessage(m);
					
					super.sendEventToListeners("ForwardedReport", null, host);
				}
				super.sendEventToListeners("GotReport", null, host);

			} else if (type.equalsIgnoreCase("command") && !(childMin == 0 && childMax == 0)){
				
//				System.out.println("intermediate node top-down");
				
				if (forwardingIndex < fwdRatio){
					
					forwardingIndex = fwrng.nextFloat(); // next to include 100 % of the first selection
					
					if (forwardingIndex < fwdRatioSubset ){ // forward to single child node
//						System.out.println("forward to single child ");
						String id = "command" + SimClock.getIntTime() + "-" + 
							host.getAddress();
						Message m = new Message(host, randomChild(), id, getMessageSize());	
						m.addProperty("fr_type", "command");
						m.setAppID(APP_ID);
						host.createNewMessage(m);
						
						super.sendEventToListeners("ForwardedCommand", null, host);
				//	}
				/**/	} else if (forwardingIndex > (1 - fwdRatioSubset)) { // forward to subset of child nodes
//						System.out.println("forward to children subset ");
						int childMinIndex = 0;
						int childMaxIndex = 0;
						while (childMaxIndex == childMinIndex) {
							childMinIndex = rng.nextInt(childMax - childMin);
							childMaxIndex = rng.nextInt(childMax - childMin);
						}
						
						for (int i = (childMaxIndex - childMinIndex); i > 0; i--){
							String id = "command" + SimClock.getIntTime() + "-" + 
								host.getAddress();
							Message m = new Message(host, getHost(Integer.parseInt(children.get(childMinIndex + i))), id, getMessageSize());	
							m.addProperty("fr_type", "command");
							m.setAppID(APP_ID);
							host.createNewMessage(m);
							
							super.sendEventToListeners("ForwardedCommand", null, host);
						}
					} else { // forward to all child nodes
//						System.out.println("forward to all children ");
						for (int i = (childMax - childMin); i > 0; i--){
							String id = "command" + SimClock.getIntTime() + "-" + 
								host.getAddress();
							Message m = new Message(host, getHost(Integer.parseInt(children.get(childMin + i))), id, getMessageSize());	
							m.addProperty("fr_type", "command");
							m.setAppID(APP_ID);
							host.createNewMessage(m);
							
							super.sendEventToListeners("ForwardedCommand", null, host);
						}
					}
				/**/	
				}
				super.sendEventToListeners("GotCommand", null, host);
			}
				
		}
		
		return msg;
	}

	/** 
	 * Draws a random host from the destination range
	 * 
	 * @return host
	 */
	private DTNHost randomHost(String type) {
		int destMax = 0;
		int destMin = 0;
		int destaddr = 0;
		
		if (type.equals("report")){
			destMin = parentMin;
			destMax = parentMax;
			
			if (destMax == destMin) {
				destaddr = destMin;
			} else {
				destaddr = destMin + rng.nextInt(destMax - destMin);
			}
			
//			System.out.println("parentIndex " + destaddr);
//			System.out.println(parents.get(destaddr));
//			System.out.println(Integer.parseInt(parents.get(destaddr)));
			destaddr = Integer.parseInt(parents.get(destaddr));
		} else if (type.equals("command")){
			destMin = childMin;
			destMax = childMax;
			
			if (destMax == destMin) {
				destaddr = destMin;
			} else {
				destaddr = destMin + rng.nextInt(destMax - destMin);
			}
			
//			System.out.println("childIndex " + destaddr);
			destaddr = Integer.parseInt(children.get(destaddr));
		}
	
//		System.out.println(destaddr);
	
		World w = SimScenario.getInstance().getWorld();
		return w.getNodeByAddress(destaddr);
	}
	
	/** 
	 * Draws a random host from the destination range
	 * 
	 * @return host
	 */
	private DTNHost randomParent() {
		int destaddr = 0;
		
		int destMin = parentMin;
		int destMax = parentMax;
		
		if (destMax == destMin) {
			destaddr = destMin;
		} else {
			destaddr = destMin + rng.nextInt(destMax - destMin);
		}
		World w = SimScenario.getInstance().getWorld();
		return w.getNodeByAddress(Integer.parseInt(parents.get(destaddr)));
	}
	
	/** 
	 * Draws a random host from the destination range
	 * 
	 * @return host
	 */
	private DTNHost randomChild() {
		int destaddr = 0;
		
		int destMin = childMin;
		int destMax = childMax;
		
		if (destMax == destMin) {
			destaddr = destMin;
		} else {
			destaddr = destMin + rng.nextInt(destMax - destMin);
		}
		World w = SimScenario.getInstance().getWorld();
		return w.getNodeByAddress(Integer.parseInt(children.get(destaddr)));
	}
	
	/** 
	 * Draws a random host from the destination range
	 * 
	 * @return host
	 */
	private DTNHost getHost(int hostId) {
		World w = SimScenario.getInstance().getWorld();
		return w.getNodeByAddress(hostId);
	}
	
	@Override
	public Application replicate() {
		return new FirstResponderApplication(this);
	}

	/** 
	 * Sends a ping packet if this is an active application instance.
	 * 
	 * @param host to which the application instance is attached
	 */
	@Override
	public void update(DTNHost host) {
		if (this.passive) return;
		
		double curTime = SimClock.getTime();
		if (curTime - this.lastMessage >= this.interval) {
			// Time to send a new message
			String type = "fr";
			
			if (childMax == 0 && childMin == 0) { // leaf node
				type =  "report";
			} else if (parentMax == 0 && parentMin == 0) { // root node 
				type = "command";
			} 
			
			if(!type.equals("fr")){

//				System.out.println("host " + host.getAddress());
				
				Message m = new Message(host, randomHost(type), 
						type + SimClock.getIntTime() + "-" + host.getAddress(),
						getMessageSize());
				
				// type to distinguish flow direction
				m.addProperty("fr_type", type);	
				
				m.setAppID(APP_ID);
				host.createNewMessage(m);
				
				// Call listeners
				super.sendEventToListeners("Sent" + type, null, host);
				
				this.lastMessage = curTime;
			}
		}
	}

	/**
	 * @return the lastPing
	 */
	public double getLastMessage() {
		return lastMessage;
	}

	/**
	 * @param lastPing the lastPing to set
	 */
	public void setLastMessage(double lastMessage) {
		this.lastMessage = lastMessage;
	}

	/**
	 * @return the interval
	 */
	public double getInterval() {
		return interval;
	}

	/**
	 * @param interval the interval to set
	 */
	public void setInterval(double interval) {
		this.interval = interval;
	}

	/**
	 * @return the passive
	 */
	public boolean isPassive() {
		return passive;
	}

	/**
	 * @param passive the passive to set
	 */
	public void setPassive(boolean passive) {
		this.passive = passive;
	}

	/**
	 * @return the destMin
	 */
	public double getFwdRatio() {
		return fwdRatio;
	}

	/**
	 * @param destMin the destMin to set
	 */
	public void setFwdRatio(double fwdRatio) {
		this.fwdRatio = fwdRatio;
	}

	/**
	 * @return the destMin
	 */
	public double getFwdRatioSubset() {
		return fwdRatioSubset;
	}

	/**
	 * @param destMin the destMin to set
	 */
	public void setFwdRatioSubset(double fwdRatioSubset) {
		this.fwdRatioSubset = fwdRatioSubset;
	}

/**
	 * @return the destMin
	 */
	public int getParentMin() {
		return parentMin;
	}

	/**
	 * @param destMin the destMin to set
	 */
	public void setParentMin(int parentMin) {
		this.parentMin = parentMin;
	}



	/**
	 * @return the destMax
	 */
	public int getParentMax() {
		return parentMax;
	}

	/**
	 * @param destMax the destMax to set
	 */
	public void setParentMax(int parentMax) {
		this.parentMax = parentMax;
	}
	
	/**
	 * @return the destMin
	 */
	public int getChildMin() {
		return childMin;
	}

	/**
	 * @return the childMax
	 */
	public int getChildMax() {
		return childMax;
	}

	/**
	 * @param childMax the childMax to set
	 */
	public void setChildMax(int childMax) {
		this.childMax = childMax;
	}

	/**
	 * @return the seed
	 */
	public int getSeed() {
		return seed;
	}

	/**
	 * @param seed the seed to set
	 */
	public void setSeed(int seed) {
		this.seed = seed;
	}
	/**
	 * @return the messageSize
	 */
	public int getMessageSize() {
		return messageSize;
	}

	/**
	 * @param messageSize the messageSize to set
	 */
	public void setMessageSize(int messageSize) {
		this.messageSize = messageSize;
	}

	public List<String> getParents() {
		return parents;
	}

	public void setParents(List<String> parents) {
		this.parents = parents;
	}

	public List<String> getChildren() {
		return children;
	}

	public void setChildren(List<String> children) {
		this.children = children;
	}

	public void setChildMin(int childMin) {
		this.childMin = childMin;
	}
	
	

}
