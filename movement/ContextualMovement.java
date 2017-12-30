package movement;

import java.util.List;

import core.Coord;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.ConnectionListener;

import core.DTNHost;
import core.NetworkInterface;
import movement.util.NodePriority;
import movement.util.NodeChooser;
import movement.util.NodeTracker;

public class ContextualMovement extends MovementModel implements ConnectionListener{

	private Coord loc;
	private NodePriority nodePriority;
	private NodeTracker nodeTracker;
	private double distanceTolerance;
	private boolean letTransmissionFinish;
	private boolean randomAll;
	private boolean listening;
	
	/**
	 * 
	 * 
	 * 
	 * @param s Parameter for configuration @see Settigns
	 */
	public ContextualMovement(Settings s){
	super(s);
	int coords[];
	//reading settings
	//starting point
	coords = s.getCsvInts("nodeLocation", 2);
	this.loc = new Coord(coords[0],coords[1]);
	listening = false;

	//distanceTolerance
	if (s.contains("distanceTolerance")) {
		distanceTolerance = s.getInt("distanceTolerance");		
	} else {
	        distanceTolerance = 0;
	}
	if (distanceTolerance < 0) {
		distanceTolerance = 0;
		System.out.println("no negative value for distanceTolerance allowed...using zero tolerance");
	}
	
	if (s.contains("letTransmissionFinish")) {
		letTransmissionFinish = s.getBoolean("letTransmissionFinish");		
	} else {
	        letTransmissionFinish = false;
	}
	
	if (s.contains("randomAll")) {
		randomAll = s.getBoolean("randomAll");		
	} else {
	        randomAll = true;
	}
	
	String priority = new String();
	if (s.contains("nodePriority")) {
		priority = s.getSetting("nodePriority");		
	} else {
	        priority = "nothing set";
	}
	
	if (priority == "random") {
		nodePriority = NodePriority.RANDOM;
	} else if (priority == "mostMessages") {
		nodePriority = NodePriority.MOSTMESSAGES;
	} else if (priority == "closest") {
		nodePriority = NodePriority.CLOSEST;
	} else if (priority == "timecritical") {
		nodePriority = NodePriority.TIMECRITICAL;
	} else if (priority == "priorityMessages") {
		nodePriority = NodePriority.PRIORITYMESSAGES;
	} else { 
		nodePriority = NodePriority.INVALID;
	}
	
	if (nodePriority == NodePriority.INVALID) {
		System.out.println("no valid nodePriority...choosing nodes randomly");
		nodePriority = NodePriority.RANDOM;
	}
	
	nodeTracker = null;
	
	System.out.println("ContextualMovment initialized");
	
	}
	
	public ContextualMovement(ContextualMovement cm) {
		super(cm);
		this.loc = cm.loc;
		this.nodePriority = cm.nodePriority;
		this.nodeTracker = null;
		this.distanceTolerance = cm.distanceTolerance;
		this.letTransmissionFinish = cm.letTransmissionFinish;
		this.listening = false;
		this.randomAll = cm.randomAll;
	}
	
	public int getClosestNode() {
		if (nodeTracker == null) {
			nodeTracker = new NodeTracker(this);
		}
		
		
		
		List<NetworkInterface> interfaces = this.getHost().getInterfaces();
		double interfaceRange = Double.MAX_VALUE;
		for(NetworkInterface element : interfaces) {
			if (element.getTransmitRange() < interfaceRange) {
				interfaceRange = element.getTransmitRange();
			}
		}
		
		return nodeTracker.getClosestNode(interfaceRange, distanceTolerance);
	}
		
	public Coord getInitialLocation(){
		return loc;
	}
	
	public ContextualMovement replicate() {
		return new ContextualMovement(this);
	}
	
	public Path getPath(){
		if (!listening) {
			listening = true;
			SimScenario.getInstance().addConnectionListener(this);
		}
		if (nodeTracker == null){
			nodeTracker = new NodeTracker(this);
		}
		
		
		//Read PacketData
		int nextNode = NodeChooser.ChooseNode(this.getHost().getMessageCollection(), nodePriority, this);
		//determine next node
		//Find next node

		Coord nextloc = nodeTracker.NodeLocation(nextNode);
		
		//create Path
		Path p = new Path(this.generateSpeed());
		p.addWaypoint(this.getHost().getLocation());
		p.addWaypoint(nextloc);
		return p;
		
		
	}
	
	public boolean getRandomAll(){
		return randomAll;
	}
	
	
	public void hostsConnected(DTNHost host1, DTNHost host2){
		boolean newnode = false;
		int nodeadr = 0;
		
		
		if (host1 == this.getHost()){
			newnode = this.nodeTracker.AcuireMovementData(SimClock.getTime(), host2.getAddress(), host2.CopyMovementmodel(), host2.getLocation());
			nodeadr = host2.getAddress();
		}
		if (host2 == this.getHost()){
			newnode = this.nodeTracker.AcuireMovementData(SimClock.getTime(), host1.getAddress(), host1.CopyMovementmodel(), host2.getLocation());
			nodeadr = host1.getAddress();
		}
		
		if (newnode) {
			System.out.print("Nodetracker now know movement of Node ");
			System.out.println(nodeadr);
		}
		
	}
	
	public void hostsDisconnected(DTNHost host1, DTNHost host2){
		
	}
	 	
	
}
