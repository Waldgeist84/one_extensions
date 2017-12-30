package movement.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import movement.ContextualMovement;
import core.DTNHost;
import core.Message;
import core.SimClock;
import core.SimScenario;

/**
 *  Class for choosing next node to go for ContextualMovement
 * 
 * 
 * 
 * @author SpeakerSeeker
 * @version 1.0
 *
 */


public final class NodeChooser {
	
	private NodeChooser(){
		
	}
	
	/**
	 * Returns the address of the chosen node
	 * 
	 * If an unknown priority is given, a random address(out of messages "to" fields) will be returned
	 * 
	 * If the collection of messages is empty the address to return will be chosen out of all addresses
	 * in the simulation except OwnAddress
	 * 
	 * This method just switches to private methods depending on priority
	 * 
	 * @param Messages A collection of messages for reading metadata
	 * @param Priority specifies a priority to choose next node, for possible priorities @see NodePriority
	 * @param OwnAddress the address of the calling node
	 * 
	 * @return the address of the chosen node
	 */
	public static int ChooseNode(Collection<Message> Messages, NodePriority Priority, ContextualMovement CallingMovement){
		switch(Priority) {
			case RANDOM: return ChooseNodeRandom(Messages, CallingMovement);
			case MOSTMESSAGES: return ChooseNodeMostMessages(Messages, CallingMovement);
			case CLOSEST: return ChooseNodeClosest(Messages, CallingMovement); 
			case TIMECRITICAL: return ChooseNodeTimecritical(Messages,CallingMovement);
			case PRIORITYMESSAGES: return ChooseNodePriorityMessages(Messages, CallingMovement);
			default: return ChooseNodeRandom(Messages, CallingMovement);
		}	
	}
	
	
	public static int ChooseNode(Collection<Message> Messages, ContextualMovement CallingMovement) {
		return ChooseNodeRandom(Messages, CallingMovement);
	}
	
	/**
	 * 
	 * 
	 * @param OwnAddress
	 * @return ChooseNodeRandom with null as the Messages parameter
	 */
	
	
	private static int ChooseNodeRandom(Collection<Message> Messages, ContextualMovement CallingMovement){
		int FinalNode = 0;
		HashSet<Integer> destinations = new HashSet<Integer>();
		if ((Messages.isEmpty()) || (CallingMovement.getRandomAll())) {
			List<DTNHost> allHosts = SimScenario.getInstance().getHosts();
			for (Iterator<DTNHost> i = allHosts.iterator(); i.hasNext();) {
				int nodeaddr = i.next().getAddress();
				if (nodeaddr != CallingMovement.getHost().getAddress()) {
					destinations.add(new Integer(nodeaddr));
				}
			}
		}
		else {
			for (Iterator<Message> i = Messages.iterator(); i.hasNext();) {
				destinations.add(new Integer(i.next().getTo().getAddress()));      
			}
		}
		int index = new Random(SimClock.getIntTime()).nextInt(destinations.size());
		int i = 0;
		for (Integer dest: destinations) {
			if (i == index) {
				FinalNode = dest;
				break;
			}
			i = i + 1;
		}
		return FinalNode;
	}
	
	/**
	 * return the address of the node which address occur most in messages "to" fields
	 * if several addresses occur most, one of them is chosen randomly
	 * 
	 * @param Messages
	 * @return
	 */
	private static int ChooseNodeMostMessages(Collection<Message> Messages, ContextualMovement CallingMovement){
		if (Messages.isEmpty()) {
			System.out.println("No Messages");
			return ChooseNodeRandom(Messages, CallingMovement);
		}
		//Create map with destinations and count of messages
		
		
		HashMap<Integer, Integer> Nodes = new HashMap<Integer, Integer>(); //key: NodeAddress value: count
		for (Iterator<Message> i = Messages.iterator(); i.hasNext();) {
			Message m = i.next();
			if (Nodes.containsKey(m.getTo().getAddress())){
				int count = Nodes.get(m.getTo().getAddress());
				Nodes.put(m.getTo().getAddress(), count+1);
			}
			else {
				Nodes.put(m.getTo().getAddress(), 1);
			}
		}
		
		//extract all destinations with highest counts
		int maxVal = Collections.max(Nodes.values());
		
		int FinalNode = 0;
		HashSet<Integer> destinations = new HashSet<Integer>();
		for (Entry<Integer, Integer> entry : Nodes.entrySet()) {
			if (entry.getValue() == maxVal) {
				destinations.add(entry.getKey());
			}
		}
		
		int index = new Random(SimClock.getIntTime()).nextInt(destinations.size());
		int i = 0;
		for (Integer dest: destinations) {
			if (i == index) {
				FinalNode = dest;
				break;
			}
			i = i + 1;
		}
		return FinalNode;
		
	}
	
	/**
	 * this method returns the address of the node with the smallest distance to this node
	 * out of all addresses to be found in Messages "to" fields
	 * 
	 * if several node have the smallest distance, one of them is chosen randomly
	 *  
	 * @param Messages @see ChooseNode
	 * @return will return random node @see ChooseNodeRandom
	 */	
	private static int ChooseNodeClosest(Collection<Message> Messages, ContextualMovement CallingMovement){
		return CallingMovement.getClosestNode();
		
	}
	
	/**
	 * no field for timecritical in messages  at the moment @see Message
	 *  
	 * @param Messages @see ChooseNode
	 * @return will return random node @see ChooseNodeRandom
	 */
	private static int ChooseNodeTimecritical(Collection<Message> Messages, ContextualMovement CallingMovement){
		return ChooseNodeRandom(Messages, CallingMovement);
	}
	
	/**
	 * no field for priority in messages  at the moment @see Message
	 *  
	 * @param Messages @see ChooseNode
	 * @return will return random node @see ChooseNodeRandom
	 */
	private static int ChooseNodePriorityMessages(Collection<Message> Messages, ContextualMovement CallingMovement){
		return ChooseNodeRandom(Messages, CallingMovement);
	}
	
}
