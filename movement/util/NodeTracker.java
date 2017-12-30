package movement.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import movement.MovementModel;
import core.Coord;
import core.DTNHost;
import core.SimClock;
import core.SimScenario;


public class NodeTracker {

	private MovementModel parent;
	private HashMap<Integer, Coord> locations;
	private HashMap<Integer, MovementData> movementData;	
	
	public NodeTracker(MovementModel CallingMovement){
		parent = CallingMovement;
		locations = new HashMap<Integer, Coord>();
		movementData = new HashMap<Integer, MovementData>();
		refreshLocations();
		
	}
	
	public void refreshLocations(){
		List<DTNHost> allHosts = SimScenario.getInstance().getHosts();
		for (Iterator<DTNHost> i = allHosts.iterator(); i.hasNext();) {
			DTNHost node = i.next();
			locations.put(new Integer(node.getAddress()), node.getLocation());
		}
	}
	
 	public Coord NodeLocation(int NodeAddress){
		if (locations.containsKey(NodeAddress)){
			return locations.get(NodeAddress);
		}
		else {
			System.out.println("Address" + NodeAddress + " not found");
			return null;
		}
	}
	
	public boolean AcuireMovementData(double time, int nodeAddress, MovementModel movementModel, Coord nodePos){
		boolean alreadyHaveKey = movementData.containsKey(nodeAddress);
		MovementData data = new MovementData();
		data.timestamp = time;
		data.nodeAddress = nodeAddress;
		data.movementModel = movementModel;
		data.lastknownPos = nodePos;
		movementData.put(nodeAddress, data);
		return !alreadyHaveKey;		
	}
	
	public int getClosestNode(double minDist, double tolerance){
		if (locations.isEmpty()) {
			return parent.getHost().getAddress();
		}
		
		Coord Ownloc = parent.getHost().getLocation();
		//create Map with addresses and distances
		int nearestNode = parent.getHost().getAddress();
		HashMap<Integer, Double> distances = new HashMap<Integer, Double>();
		for(Entry<Integer, Coord> e : locations.entrySet()){
			double dist = Math.sqrt(Math.pow(Ownloc.getX()-e.getValue().getX(), 2)+ Math.pow(Ownloc.getY()-e.getValue().getY(), 2));
			if ((dist > minDist) && (e.getKey() != parent.getHost().getAddress())) {
				distances.put(e.getKey(), dist);
			}	
		}
		
		//find shortest distance
		double shortest = Collections.min(distances.values());		
		shortest = shortest*(1+tolerance/100);

		
		//create list with all address with the shortest distance
		HashSet<Integer> candidateNodes = new HashSet<Integer>();
		for (Entry<Integer, Double> element : distances.entrySet()){
			if (element.getValue() <= shortest) {
				candidateNodes.add(element.getKey());
			}
		}
		//draw one out of this list randomly
		int index = new Random(SimClock.getIntTime()).nextInt(candidateNodes.size());
		int i = 0;
		for (Integer dest: candidateNodes) {
			if (i == index) {
				nearestNode = dest;
				break;
			}
			i = i + 1;
		}
		//return final node
		return nearestNode;
		
	}
	
		
}
	

