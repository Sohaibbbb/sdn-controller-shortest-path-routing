package edu.brown.cs.sdn.apps.sps;

/**
 *  Submission for CS557 project
 *  Vignesh M. Pagadala
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.print.attribute.standard.Destination;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.instruction.OFInstruction;
import org.openflow.protocol.instruction.OFInstructionActions;
import org.openflow.protocol.instruction.OFInstructionApplyActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.brown.cs.sdn.apps.util.Host;
import edu.brown.cs.sdn.apps.util.SwitchCommands;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.ImmutablePort;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceListener;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.routing.Link;

/**
 * Class to simulate graph (to apply Dijkstra's algorithm)
 * Source: Baeldung (www.baeldung.com/java-dijkstra)
 */
class Graph {
	private Set<Node> nodes = new HashSet<>();
    public void addNode(Node nodeA) 
    {
        nodes.add(nodeA);
    }
}

/**
 * Class to simulate nodes (to apply Dijkstra's algorithm)
 * Source: Baeldung (www.baeldung.com/java-dijkstra)
 */
class Node {
    private String name;
    private List<Node> shortestPath = new LinkedList<>();
    private Integer distance = Integer.MAX_VALUE;
    Map<Node, Integer> adjacentNodes = new HashMap<>();
    public void addDestination(Node destination, int distance) {
        adjacentNodes.put(destination, distance);
    }
    public Node(String name) {
        this.name = name;
    }
    public Integer getDistance()
    {
    	return this.distance;
    }
    public void setDistance(Integer dist)
    {
    	this.distance = dist;
    }
    public Map<Node, Integer> getAdjacentNodes()
    {
    	return this.adjacentNodes;
    }
    public List<Node> getShortestPath()
    {
    	return shortestPath;
    }
    public void setShortestPath(List<Node> sp)
    {
    	this.shortestPath = sp;
    }
    public String getName()
    {
    	return this.name;
    }
}

public class ShortestPathSwitching implements IFloodlightModule, IOFSwitchListener, 
		ILinkDiscoveryListener, IDeviceListener, InterfaceShortestPathSwitching
{
	public static final String MODULE_NAME = ShortestPathSwitching.class.getSimpleName();
	// Interface to the logging system
    private static Logger log = LoggerFactory.getLogger(MODULE_NAME);
    // Interface to Floodlight core for interacting with connected switches
    private IFloodlightProviderService floodlightProv;
    // Interface to link discovery service
    private ILinkDiscoveryService linkDiscProv;
    // Interface to device manager service
    private IDeviceService deviceProv;
    // Switch table in which rules should be installed
    private byte table;
    // Map of hosts to devices
    private Map<IDevice,Host> knownHosts;
    
    /**
     *  Applies Dijkstra's algorithm to find shortest paths to a given node.
     * Source: Baeldung (www.baeldung.com/java-dijkstra)
	 */
	public static Graph calculateShortestPathFromSource(Graph graph, Node source) 
	{
	    source.setDistance(0);
	    Set<Node> settledNodes = new HashSet<>();
	    Set<Node> unsettledNodes = new HashSet<>();
	    unsettledNodes.add(source);
	    while (unsettledNodes.size() != 0) {
	        Node currentNode = getLowestDistanceNode(unsettledNodes);
	        unsettledNodes.remove(currentNode);
	        for (Map.Entry < Node, Integer> adjacencyPair:
	          currentNode.getAdjacentNodes().entrySet()) {
	            Node adjacentNode = adjacencyPair.getKey();
	            Integer edgeWeight = adjacencyPair.getValue();
	            if (!settledNodes.contains(adjacentNode)) {
	                CalculateMinimumDistance(adjacentNode, edgeWeight, currentNode);
	                unsettledNodes.add(adjacentNode);
	            }
	        }
	        settledNodes.add(currentNode);
	    }
	    return graph;
	}
	
	/**
	 * Function to get minimum distance.
	 * Source: Baeldung (www.baeldung.com/java-dijkstra)
	 */ 
	private static Node getLowestDistanceNode(Set < Node > unsettledNodes) 
	{
	    Node lowestDistanceNode = null;
	    int lowestDistance = Integer.MAX_VALUE;
	    for (Node node: unsettledNodes) {
	        int nodeDistance = node.getDistance();
	        if (nodeDistance < lowestDistance) {
	            lowestDistance = nodeDistance;
	            lowestDistanceNode = node;
	        }
	    }
	    return lowestDistanceNode;
	}
	
	/** 
	 * Function to get minimum distance.
	 * Source: Baeldung (www.baeldung.com/java-dijkstra)
	 */
	private static void CalculateMinimumDistance(Node evaluationNode, Integer edgeWeigh, Node sourceNode) 
	{
	    Integer sourceDistance = sourceNode.getDistance();
	    if (sourceDistance + edgeWeigh < evaluationNode.getDistance()) 
	    {
	        evaluationNode.setDistance(sourceDistance + edgeWeigh);
	        LinkedList<Node> shortestPath = new LinkedList<>(sourceNode.getShortestPath());
	        shortestPath.add(sourceNode);
	        evaluationNode.setShortestPath(shortestPath);
	    }
	}
	
	/**
     * Loads dependencies and initializes data structures.
     */
	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException 
    {           
		log.info(String.format("Initializing %s...", MODULE_NAME));
		Map<String,String> config = context.getConfigParams(this);
        this.table = Byte.parseByte(config.get("table"));
		this.floodlightProv = context.getServiceImpl(
				IFloodlightProviderService.class);
        this.linkDiscProv = context.getServiceImpl(ILinkDiscoveryService.class);
        this.deviceProv = context.getServiceImpl(IDeviceService.class);
        
        this.knownHosts = new ConcurrentHashMap<IDevice,Host>();
        
        /*********************************************************************/
        /* TODO: Initialize other class variables, if necessary              */
        
        /* Nothing needed here. */
        
        /*********************************************************************/
	}

	/**
     * Subscribes to events and performs other startup tasks.
     */
	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException 
	{
		log.info(String.format("Starting %s...", MODULE_NAME));
		this.floodlightProv.addOFSwitchListener(this);
		this.linkDiscProv.addListener(this);
		this.deviceProv.addListener(this);
		
		/*********************************************************************/
		/* TODO: Perform other tasks, if necessary                           */
		
		/*Nothing needed here. */
		
		/*********************************************************************/
	}
	
	/**
	 * Get the table in which this application installs rules.
	 */
	public byte getTable()
	{ return this.table; }
	
    /**
     * Get a list of all known hosts in the network.
     */
    private Collection<Host> getHosts()
    { return this.knownHosts.values(); }
	
    /**
     * Get a map of all active switches in the network. Switch DPID is used as
     * the key.
     */
	private Map<Long, IOFSwitch> getSwitches()
    { return floodlightProv.getAllSwitchMap(); }
	
    /**
     * Get a list of all active links in the network.
     */
    private Collection<Link> getLinks()
    { return linkDiscProv.getLinks().keySet(); }

    /**
     * Event handler called when a host joins the network.
     * @param device information about the host
     */
    
    /**
     * Utility function to get key from value in a HashMap.
     * Source: Baeldung (www.baeldung.com/java-map-key-from-value)
     */
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
    
	@Override
	public void deviceAdded(IDevice device) 
	{
		Host host = new Host(device, this.floodlightProv);
		
		/*****************************************************************/
			/* TODO: Update routing: add rules to route to new host          */
		
		Integer c;
		// We only care about a new host if we know its IP
		if (host.getIPv4Address() != null && host.isAttachedToSwitch() == true)
		{
			log.info(String.format("Host %s added", host.getName()));
			this.knownHosts.put(device, host);
			Map<Long, IOFSwitch> switches = this.getSwitches();
			// Grab the switch that this host is connected to.
			IOFSwitch hostswitch = host.getSwitch();
			// Get match criteria, in this case, the address of the destination host.
			OFMatch ofm = new OFMatch();
			ofm.setDataLayerType(Ethernet.TYPE_IPv4);
			//ofm.setNetworkDestination(host.getIPv4Address());
			
			// Format MAC address.
			StringBuilder macaddr = new StringBuilder(Long.toHexString(host.getMACAddress()));
			while(macaddr.length() < 12)
			{
				macaddr.insert(0, "0");
			}
			// Insert colons
			String newmacaddr = macaddr.toString();
			newmacaddr = newmacaddr.replaceAll("(.{2})", "$1" + ':').substring(0,17);
			ofm.setDataLayerDestination(newmacaddr);	
			// Iterate through all switches. If current switch is encountered, include rule to output to host port.
			// Set action to be done. Varies based on switch, so include this in loop.
			for(Long s : switches.keySet())
			{
				IOFSwitch switchobj = switches.get(s);
				// If this is the same as the switch the host is connected to, install rule.
				if(switchobj.equals(hostswitch))
				{
					OFAction ofa = new OFActionOutput(host.getPort());
					OFInstruction i = new OFInstructionApplyActions(Arrays.asList(ofa));
					SwitchCommands.installRule(switchobj, this.getTable(), SwitchCommands.DEFAULT_PRIORITY, ofm, Arrays.asList(i));
				}
				// Else, find shortest path from switchobj to hostswitch. Use Dijkstra's algorithm for this.
				else
				{
					Graph graph = new Graph();
					// Determine network topology using getLinks()
					Collection<Link> links = this.getLinks();
					// Create Map mapping iterator value with (long) node IDs
					Map<Integer, Long> nodeidmap = new HashMap<Integer, Long>();
					// Iterate through links. Initialize nodes.
					Node[] node = new Node[100];
					c = 0;
					int sflag;
					int dflag;
					Integer sourceiterator = 0;
					Integer destiterator = 0;
					for(Link l : links)
					{
						sflag = 0;
						dflag = 0;
						Long sourcenode = l.getSrc();
						Long destnode = l.getDst();			
						// Search Map to see if these nodes already exist.
						if(nodeidmap.containsValue(sourcenode))
						{
							// If true, then get the corresponding iterator c
							sourceiterator = getKeyByValue(nodeidmap, sourcenode);
							sflag = 1;
							// Source node already exists.
						}
						if(nodeidmap.containsValue(destnode))
						{
							destiterator = getKeyByValue(nodeidmap, destnode);
							dflag = 1;
						}
						if(sflag == 0)
						{
							// Add source node
							node[c] = new Node(c.toString());
							// Create map
							nodeidmap.put(c, sourcenode);
							c = c+1;
						}
						if(dflag == 0)
						{
							// Add dest node
							node[c] = new Node(c.toString());
							// Create map
							nodeidmap.put(c, destnode);
							c = c+1;
						}
						// Create links
						if(sflag == 0 && dflag == 0)
						{
							node[c-2].addDestination(node[c-1], 1);
						}
						else if(sflag == 0 && dflag == 1)
						{
							node[c - 1].addDestination(node[destiterator], 1);
						}
						else if(sflag == 1 && dflag == 0)
						{
							node[sourceiterator].addDestination(node[c-1], 1);
						}
						else
						{
							node[sourceiterator].addDestination(node[destiterator], 1);
						}
					}
					// Loop from c=0 to < c. Add nodes to graph.
					for(int i = 0; i < c; i++)
					{
						graph.addNode(node[i]);
					}
					// Calculate shortest paths to every other switch from current switch (switchobj).
					// Get node index value for switchobj.
					int sourcenodeid = getKeyByValue(nodeidmap, s);
					graph = calculateShortestPathFromSource(graph, node[sourcenodeid]); 
					// Now, get shortest path attribute of each node. This tells us the shortest path of every node in graph to switchobj.
					// Need to find shortest path from switchobj to hostswitch.
					// Grab integer of hostswitch.
					List<Node> hnodespath = node[getKeyByValue(nodeidmap, hostswitch.getId())].getShortestPath();
					
					// Printing out path to host switch.
					/*
					System.out.println("Path to host switch: \n");
					for(Node everyn : hnodespath)
					{
						System.out.print(" " + nodeidmap.get(Integer.parseInt(everyn.getName())));
					}
					System.out.println();
					*/
					
					// Printing out next hop switch.
					//System.out.println("Next hop switch: ");
					
					Long nextHopSwitch = hostswitch.getId();
					
					// If length of hnodespath is 1, then next hop is host switch itself.
					if(hnodespath.size() == 1)
					{
						nextHopSwitch = hostswitch.getId();
					}
					else
					{
						nextHopSwitch = nodeidmap.get(Integer.parseInt(hnodespath.get(1).getName()));
					}
					// Need to find port through which switchobj is connected to nextHopSwitch (above).
					Integer outport = 0;
					for(Link l : links)
					{
						if(l.getSrc() == switchobj.getId() && l.getDst() == nextHopSwitch)
						{
							outport = l.getSrcPort();
						}
					}
					// Formulate action.
					OFAction ofa = new OFActionOutput(outport);
					OFInstruction i = new OFInstructionApplyActions(Arrays.asList(ofa));
					// Install the rule.
					SwitchCommands.installRule(switchobj, this.getTable(), SwitchCommands.DEFAULT_PRIORITY, ofm, Arrays.asList(i));
				}
			}
			/*****************************************************************/
		}
	}

	/**
     * Event handler called when a host is no longer attached to a switch.
     * @param device information about the host
     */
	@Override
	public void deviceRemoved(IDevice device) 
	{
		Host host = this.knownHosts.get(device);
		if (null == host)
		{
			host = new Host(device, this.floodlightProv);
			this.knownHosts.put(device, host);
		}
		log.info(String.format("Host %s is no longer attached to a switch", 
				host.getName()));
		
		/*********************************************************************/
		/* TODO: Update routing: remove rules to route to host               */
		
		// Iterate through all switches and remove rule associated with host 'host'.
		Map<Long, IOFSwitch> switches = this.getSwitches();
		OFMatch ofm = new OFMatch();
		ofm.setDataLayerType(Ethernet.TYPE_IPv4);
		if(host.getIPv4Address() != null)
		{
			//ofm.setNetworkDestination(host.getIPv4Address());
			// Get MAC address.
			StringBuilder macaddr = new StringBuilder(Long.toHexString(host.getMACAddress()));
			while(macaddr.length() < 12)
			{
				macaddr.insert(0, "0");
			}
			// Insert colons
			String newmacaddr = macaddr.toString();
			newmacaddr = newmacaddr.replaceAll("(.{2})", "$1" + ':').substring(0,17);
			//System.out.println("\n\n Host's MAC address: " + newmacaddr);
			ofm.setDataLayerDestination(newmacaddr);
			for(Long s : switches.keySet())
			{
				IOFSwitch switchobj = switches.get(s);
				// Remove rule associated with host host from switch switchobj
				SwitchCommands.removeRules(switchobj, table, ofm);
			}	
		}
		/*********************************************************************/
	}

	/**
     * Event handler called when a host moves within the network.
     * @param device information about the host
     */
	@Override
	public void deviceMoved(IDevice device) 
	{
		Host host = this.knownHosts.get(device);
		if (null == host)
		{
			host = new Host(device, this.floodlightProv);
			this.knownHosts.put(device, host);
		}
		if (!host.isAttachedToSwitch())
		{
			this.deviceRemoved(device);
			return;
		}
		log.info(String.format("Host %s moved to s%d:%d", host.getName(),
				host.getSwitch().getId(), host.getPort()));
		
		/*********************************************************************/
		/* TODO: Update routing: change rules to route to host               */
		
		Integer c;
		if (host.getIPv4Address() != null && host.isAttachedToSwitch() == true)
		{
			log.info(String.format("Host %s added", host.getName()));
			this.knownHosts.put(device, host);
			
			/*****************************************************************/
     			/* TODO: Update routing: add rules to route to new host          */
			
			Map<Long, IOFSwitch> switches = this.getSwitches();
			// Grab the switch that this host is connected to.
			IOFSwitch hostswitch = host.getSwitch();
			// Iterate through all switches. If current switch is encountered, include rule to output to host port.
			// Get match criteria, in this case, the address of the destination host.
			OFMatch ofm = new OFMatch();
			ofm.setDataLayerType(Ethernet.TYPE_IPv4);
			//ofm.setNetworkDestination(host.getIPv4Address());
			StringBuilder macaddr = new StringBuilder(Long.toHexString(host.getMACAddress()));
			while(macaddr.length() < 12)
			{
				macaddr.insert(0, "0");
			}
			// Insert colons
			String newmacaddr = macaddr.toString();
			newmacaddr = newmacaddr.replaceAll("(.{2})", "$1" + ':').substring(0,17);
			//System.out.println("\n\n Host's MAC address: " + newmacaddr);
			ofm.setDataLayerDestination(newmacaddr);
			// Set action to be done. Varies based on switch, so include this in loop.
			for(Long s : switches.keySet())
			{
				IOFSwitch switchobj = switches.get(s);
				// If this is the same as the switch the host is connected to, install rule.
				if(switchobj.equals(hostswitch))
				{
					OFAction ofa = new OFActionOutput(host.getPort());
					OFInstruction i = new OFInstructionApplyActions(Arrays.asList(ofa));
					SwitchCommands.installRule(switchobj, this.getTable(), SwitchCommands.DEFAULT_PRIORITY, ofm, Arrays.asList(i));
				}
				// Else, find shortest path from switchobj to hostswitch. Use Dijkstra's algorithm for this.
				else
				{
					Graph graph = new Graph();
					// Determine network topology using getLinks()
					Collection<Link> links = this.getLinks();
					// Create Map mapping iterator value with (long) node IDs
					Map<Integer, Long> nodeidmap = new HashMap<Integer, Long>();
					// Iterate through links. Initialize nodes.
					Node[] node = new Node[100];
					c = 0;
					int sflag;
					int dflag;
					Integer sourceiterator = 0;
					Integer destiterator = 0;
					for(Link l : links)
					{
						sflag = 0;
						dflag = 0;
						Long sourcenode = l.getSrc();
						Long destnode = l.getDst();
						// Search Map to see if these nodes already exist.
						if(nodeidmap.containsValue(sourcenode))
						{
							// If true, then get the corresponding iterator c
							sourceiterator = getKeyByValue(nodeidmap, sourcenode);
							sflag = 1;
							// Source node already exists.
						}
						if(nodeidmap.containsValue(destnode))
						{
							destiterator = getKeyByValue(nodeidmap, destnode);
							dflag = 1;
						}
						if(sflag == 0)
						{
							// Add source node
							node[c] = new Node(c.toString());
							// Create map
							nodeidmap.put(c, sourcenode);
							c = c+1;
						}
						if(dflag == 0)
						{
							// Add dest node
							node[c] = new Node(c.toString());
							// Create map
							nodeidmap.put(c, destnode);
							c = c+1;
						}
						// Create links
						if(sflag == 0 && dflag == 0)
						{
							node[c-2].addDestination(node[c-1], 1);
						}
						else if(sflag == 0 && dflag == 1)
						{
							node[c - 1].addDestination(node[destiterator], 1);
						}
						else if(sflag == 1 && dflag == 0)
						{
							node[sourceiterator].addDestination(node[c-1], 1);
						}
						else
						{
							node[sourceiterator].addDestination(node[destiterator], 1);
						}
					}
					// Loop from c=0 to < c. Add nodes to graph.
					for(int i = 0; i < c; i++)
					{
						graph.addNode(node[i]);
					}
					// Calculate shortest paths to every other switch from current switch (switchobj).
					// Get node index value for switchobj
					int sourcenodeid = getKeyByValue(nodeidmap, s);
					graph = calculateShortestPathFromSource(graph, node[sourcenodeid]); 
					// Now, get shortest path attribute of each node. This tells us the shortest path of every node in graph to switchobj.
					
					// For printing out shortest path.
					//System.out.println("Current switch: " + switchobj.getId());
					/*
					for(int i = 0; i < c; i++)
					{
						Long nid = nodeidmap.get(i);
						System.out.println("Switch: " + nid);
						
						List<Node> spath = node[i].getShortestPath();
						for(Node everyn : spath)
						{
							System.out.print(" " + nodeidmap.get(Integer.parseInt(everyn.getName())));
						}
						System.out.println();
					}
					*/
					// Need to find shortest path from switchobj to hostswitch.
					// Grab integer of hostswitch.
					List<Node> hnodespath = node[getKeyByValue(nodeidmap, hostswitch.getId())].getShortestPath();
					// For printing out shortest path.
					/*
					System.out.println("Path to host switch: \n");
					for(Node everyn : hnodespath)
					{
						System.out.print(" " + nodeidmap.get(Integer.parseInt(everyn.getName())));
					}
					System.out.println();
					*/
					//System.out.println("Next hop switch: ");
					Long nextHopSwitch = hostswitch.getId();
					// If length of hnodespath is 1, then next hop is host switch itself.
					if(hnodespath.size() == 1)
					{
						nextHopSwitch = hostswitch.getId();
					}
					else
					{
						nextHopSwitch = nodeidmap.get(Integer.parseInt(hnodespath.get(1).getName()));
					}
					// Need to find port through which switchobj is connected to nextHopSwitch (above).
					Integer outport = 0;
					for(Link l : links)
					{
						if(l.getSrc() == switchobj.getId() && l.getDst() == nextHopSwitch)
						{
							outport = l.getSrcPort();
						}
					}
					OFAction ofa = new OFActionOutput(outport);
					OFInstruction i = new OFInstructionApplyActions(Arrays.asList(ofa));
					SwitchCommands.installRule(switchobj, this.getTable(), SwitchCommands.DEFAULT_PRIORITY, ofm, Arrays.asList(i));
				}
			}
		}
		/*********************************************************************/
	}
	
    /**
     * Event handler called when a switch joins the network.
     * @param DPID for the switch
     */
	@Override		
	public void switchAdded(long switchId) 
	{
		IOFSwitch sw = this.floodlightProv.getSwitch(switchId);
		log.info(String.format("Switch s%d added", switchId));
		
		/*********************************************************************/
		/* TODO: Update routing: change routing rules for all hosts          */
		
		// Nothing needs to be done here. Taken care of in linkDiscoveryUpdate.
		
		/*********************************************************************/
	}

	/**
	 * Event handler called when a switch leaves the network.
	 * @param DPID for the switch
	 */
	@Override
	public void switchRemoved(long switchId) 
	{
		IOFSwitch sw = this.floodlightProv.getSwitch(switchId);
		log.info(String.format("Switch s%d removed", switchId));
		/*********************************************************************/
		/* TODO: Update routing: change routing rules for all hosts          */
		Integer c;
		// Update routing tables in all switches.
		// Iterate through set of hosts.
		for(Host host : this.getHosts())
		{
			if(host.getIPv4Address() == null || host.isAttachedToSwitch() == false)
			{
				continue;
			}
			//System.out.println("\n\nHost " +host.getName());
			Map<Long, IOFSwitch> switches = this.getSwitches();
			// Grab the switch that this host is connected to.
			IOFSwitch hostswitch = host.getSwitch();
			// Iterate through all switches. If current switch is encountered, include rule to output to host port.
			// Get match criteria, in this case, the address of the destination host.
			OFMatch ofm = new OFMatch();
			ofm.setDataLayerType(Ethernet.TYPE_IPv4);
			//ofm.setNetworkDestination(host.getIPv4Address());
			StringBuilder macaddr = new StringBuilder(Long.toHexString(host.getMACAddress()));
			while(macaddr.length() < 12)
			{
				macaddr.insert(0, "0");
			}
			// Insert colons
			String newmacaddr = macaddr.toString();
			newmacaddr = newmacaddr.replaceAll("(.{2})", "$1" + ':').substring(0,17);
			//System.out.println("\n\n Host's MAC address: " + newmacaddr);
			ofm.setDataLayerDestination(newmacaddr);
			// Set action to be done. Varies based on switch, so include this in loop.
			for(Long s : switches.keySet())
			{
				IOFSwitch switchobj = switches.get(s);
				// If this is the same as the switch the host is connected to, install rule.
				if(switchobj.equals(hostswitch))
				{
					OFAction ofa = new OFActionOutput(host.getPort());
					OFInstruction i = new OFInstructionApplyActions(Arrays.asList(ofa));
					SwitchCommands.installRule(switchobj, this.getTable(), SwitchCommands.DEFAULT_PRIORITY, ofm, Arrays.asList(i));
				}
				// Else, find shortest path from switchobj to hostswitch. Use Dijkstra's algorithm for this.
				else
				{
					Graph graph = new Graph();
					// Determine network topology using getLinks()
					Collection<Link> links = this.getLinks();
					// Create Map mapping iterator value with (long) node IDs
					Map<Integer, Long> nodeidmap = new HashMap<Integer, Long>();
					// Iterate through links. Initialize nodes.
					Node[] node = new Node[100];
					c = 0;
					int sflag;
					int dflag;
					Integer sourceiterator = 0;
					Integer destiterator = 0;
					for(Link l : links)
					{
						sflag = 0;
						dflag = 0;
						Long sourcenode = l.getSrc();
						Long destnode = l.getDst();
						// Search Map to see if these nodes already exist.
						if(nodeidmap.containsValue(sourcenode))
						{
							// If true, then get the corresponding iterator c
							sourceiterator = getKeyByValue(nodeidmap, sourcenode);
							sflag = 1;
							// Source node already exists.
						}
						if(nodeidmap.containsValue(destnode))
						{
							destiterator = getKeyByValue(nodeidmap, destnode);
							dflag = 1;
						}
						if(sflag == 0)
						{
							// Add source node
							node[c] = new Node(c.toString());
							// Create map
							nodeidmap.put(c, sourcenode);
							c = c+1;
						}
						if(dflag == 0)
						{
							// Add dest node
							node[c] = new Node(c.toString());
							// Create map
							nodeidmap.put(c, destnode);
							c = c+1;
						}
						// Create links
						if(sflag == 0 && dflag == 0)
						{
							node[c-2].addDestination(node[c-1], 1);
						}
						else if(sflag == 0 && dflag == 1)
						{
							node[c - 1].addDestination(node[destiterator], 1);
						}
						else if(sflag == 1 && dflag == 0)
						{
							node[sourceiterator].addDestination(node[c-1], 1);
						}
						else
						{
							node[sourceiterator].addDestination(node[destiterator], 1);
						}
					}
					// Loop from c=0 to < c. Add nodes to graph.
					for(int i = 0; i < c; i++)
					{
						graph.addNode(node[i]);
					}
					// Calculate shortest paths to every other switch from current switch (switchobj).
					// Get node index value for switchobj
					int sourcenodeid = getKeyByValue(nodeidmap, s);
					graph = calculateShortestPathFromSource(graph, node[sourcenodeid]); 
					// Now, get shortest path attribute of each node. This tells us the shortest path of every node in graph to switchobj.
					// Need to find shortest path from switchobj to hostswitch.
					// Grab integer of hostswitch.
					List<Node> hnodespath = node[getKeyByValue(nodeidmap, hostswitch.getId())].getShortestPath();
					Long nextHopSwitch = hostswitch.getId();
					// If length of hnodespath is 1, then next hop is host switch itself.
					if(hnodespath.size() == 1)
					{
						nextHopSwitch = hostswitch.getId();
					}
					else
					{
						nextHopSwitch = nodeidmap.get(Integer.parseInt(hnodespath.get(1).getName()));
					}
					// Need to find port through which switchobj is connected to nextHopSwitch (above).
					Integer outport = 0;
					for(Link l : links)
					{
						if(l.getSrc() == switchobj.getId() && l.getDst() == nextHopSwitch)
						{
							outport = l.getSrcPort();
						}
					}
					OFAction ofa = new OFActionOutput(outport);
					OFInstruction i = new OFInstructionApplyActions(Arrays.asList(ofa));
					SwitchCommands.installRule(switchobj, this.getTable(), SwitchCommands.DEFAULT_PRIORITY, ofm, Arrays.asList(i));
				}
			}
		}
		/*********************************************************************/
	}

	/**
	 * Event handler called when multiple links go up or down.
	 * @param updateList information about the change in each link's state
	 */
	@Override
	public void linkDiscoveryUpdate(List<LDUpdate> updateList) 
	{
		for (LDUpdate update : updateList)
		{
			// If we only know the switch & port for one end of the link, then
			// the link must be from a switch to a host
			if (0 == update.getDst())
			{
				log.info(String.format("Link s%s:%d -> host updated", 
					update.getSrc(), update.getSrcPort()));
			}
			// Otherwise, the link is between two switches
			else
			{
				log.info(String.format("Link s%s:%d -> %s:%d updated", 
					update.getSrc(), update.getSrcPort(),
					update.getDst(), update.getDstPort()));
			}
		}
		
		/*********************************************************************/
		/* TODO: Update routing: change routing rules for all hosts          */
		
		Integer c;		
		// Iterate through set of hosts.
		for(Host host : this.getHosts())
		{
			if(host.getIPv4Address() == null || host.isAttachedToSwitch() == false)
			{
				continue;
			}
			Map<Long, IOFSwitch> switches = this.getSwitches();
			// Grab the switch that this host is connected to.
			IOFSwitch hostswitch = host.getSwitch();
			// Iterate through all switches. If current switch is encountered, include rule to output to host port.
			// Get match criteria, in this case, the address of the destination host.
			OFMatch ofm = new OFMatch();
			ofm.setDataLayerType(Ethernet.TYPE_IPv4);
			//ofm.setNetworkDestination(host.getIPv4Address());
			StringBuilder macaddr = new StringBuilder(Long.toHexString(host.getMACAddress()));
			while(macaddr.length() < 12)
			{
				macaddr.insert(0, "0");
			}
			// Insert colons
			String newmacaddr = macaddr.toString();
			newmacaddr = newmacaddr.replaceAll("(.{2})", "$1" + ':').substring(0,17);
			//System.out.println("\n\n Host's MAC address: " + newmacaddr);
			ofm.setDataLayerDestination(newmacaddr);
			// Set action to be done. Varies based on switch, so include this in loop.
			for(Long s : switches.keySet())
			{
				IOFSwitch switchobj = switches.get(s);
				// If this is the same as the switch the host is connected to, install rule.
				if(switchobj.equals(hostswitch))
				{
					OFAction ofa = new OFActionOutput(host.getPort());
					OFInstruction i = new OFInstructionApplyActions(Arrays.asList(ofa));
					SwitchCommands.installRule(switchobj, this.getTable(), SwitchCommands.DEFAULT_PRIORITY, ofm, Arrays.asList(i));
				}
				// Else, find shortest path from switchobj to hostswitch. Use Dijkstra's algorithm for this.
				else
				{
					Graph graph = new Graph();
					// Determine network topology using getLinks()
					Collection<Link> links = this.getLinks();
					// Create Map mapping iterator value with (long) node IDs
					Map<Integer, Long> nodeidmap = new HashMap<Integer, Long>();
					// Iterate through links. Initialize nodes.
					Node[] node = new Node[100];
					c = 0;
					int sflag;
					int dflag;
					Integer sourceiterator = 0;
					Integer destiterator = 0;
					for(Link l : links)
					{
						sflag = 0;
						dflag = 0;
						Long sourcenode = l.getSrc();
						Long destnode = l.getDst();
						// Search Map to see if these nodes already exist.
						if(nodeidmap.containsValue(sourcenode))
						{
							// If true, then get the corresponding iterator c
							sourceiterator = getKeyByValue(nodeidmap, sourcenode);
							sflag = 1;
							// Source node already exists.
						}
						if(nodeidmap.containsValue(destnode))
						{
							destiterator = getKeyByValue(nodeidmap, destnode);
							dflag = 1;
						}
						if(sflag == 0)
						{
							// Add source node
							node[c] = new Node(c.toString());
							// Create map
							nodeidmap.put(c, sourcenode);
							c = c+1;
						}
						if(dflag == 0)
						{
							// Add dest node
							node[c] = new Node(c.toString());
							// Create map
							nodeidmap.put(c, destnode);
							c = c+1;
						}
						// Create links
						if(sflag == 0 && dflag == 0)
						{
							node[c-2].addDestination(node[c-1], 1);
						}
						else if(sflag == 0 && dflag == 1)
						{
							node[c - 1].addDestination(node[destiterator], 1);
						}
						else if(sflag == 1 && dflag == 0)
						{
							node[sourceiterator].addDestination(node[c-1], 1);
						}
						else
						{
							node[sourceiterator].addDestination(node[destiterator], 1);
						}
					}
					// Loop from c=0 to < c. Add nodes to graph.
					for(int i = 0; i < c; i++)
					{
						graph.addNode(node[i]);
					}
					// Calculate shortest paths to every other switch from current switch (switchobj).
					// Get node index value for switchobj
					int sourcenodeid = getKeyByValue(nodeidmap, s);
					graph = calculateShortestPathFromSource(graph, node[sourcenodeid]); 
					// Now, get shortest path attribute of each node. This tells us the shortest path of every node in graph to switchobj.
					// Need to find shortest path from switchobj to hostswitch.
					// Grab integer of hostswitch.
					List<Node> hnodespath = node[getKeyByValue(nodeidmap, hostswitch.getId())].getShortestPath();
					Long nextHopSwitch = hostswitch.getId();
					// If length of hnodespath is 1, then next hop is host switch itself.
					if(hnodespath.size() == 1)
					{
						nextHopSwitch = hostswitch.getId();
					}
					else
					{
						nextHopSwitch = nodeidmap.get(Integer.parseInt(hnodespath.get(1).getName()));
					}
					// Need to find port through which switchobj is connected to nextHopSwitch (above).
					Integer outport = 0;
					for(Link l : links)
					{
						if(l.getSrc() == switchobj.getId() && l.getDst() == nextHopSwitch)
						{
							outport = l.getSrcPort();
						}
					}
					OFAction ofa = new OFActionOutput(outport);
					OFInstruction i = new OFInstructionApplyActions(Arrays.asList(ofa));
					SwitchCommands.installRule(switchobj, this.getTable(), SwitchCommands.DEFAULT_PRIORITY, ofm, Arrays.asList(i));
				}
			}
		}
		/*********************************************************************/
	}

	/**
	 * Event handler called when link goes up or down.
	 * @param update information about the change in link state
	 */
	@Override
	public void linkDiscoveryUpdate(LDUpdate update) 
	{ this.linkDiscoveryUpdate(Arrays.asList(update)); }
	
	/**
     * Event handler called when the IP address of a host changes.
     * @param device information about the host
     */
	@Override
	public void deviceIPV4AddrChanged(IDevice device) 
	{ this.deviceAdded(device); }

	/**
     * Event handler called when the VLAN of a host changes.
     * @param device information about the host
     */
	@Override
	public void deviceVlanChanged(IDevice device) 
	{ /* Nothing we need to do, since we're not using VLANs */ }
	
	/**
	 * Event handler called when the controller becomes the master for a switch.
	 * @param DPID for the switch
	 */
	@Override
	public void switchActivated(long switchId) 
	{ /* Nothing we need to do, since we're not switching controller roles */ }

	/**
	 * Event handler called when some attribute of a switch changes.
	 * @param DPID for the switch
	 */
	@Override
	public void switchChanged(long switchId) 
	{ /* Nothing we need to do */ }
	
	/**
	 * Event handler called when a port on a switch goes up or down, or is
	 * added or removed.
	 * @param DPID for the switch
	 * @param port the port on the switch whose status changed
	 * @param type the type of status change (up, down, add, remove)
	 */
	@Override
	public void switchPortChanged(long switchId, ImmutablePort port,
			PortChangeType type) 
	{ /* Nothing we need to do, since we'll get a linkDiscoveryUpdate event */ }

	/**
	 * Gets a name for this module.
	 * @return name for this module
	 */
	@Override
	public String getName() 
	{ return this.MODULE_NAME; }

	/**
	 * Check if events must be passed to another module before this module is
	 * notified of the event.
	 */
	@Override
	public boolean isCallbackOrderingPrereq(String type, String name) 
	{ return false; }

	/**
	 * Check if events must be passed to another module after this module has
	 * been notified of the event.
	 */
	@Override
	public boolean isCallbackOrderingPostreq(String type, String name) 
	{ return false; }
	
    /**
     * Tell the module system which services we provide.
     */
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() 
	{
		Collection<Class<? extends IFloodlightService>> services =
					new ArrayList<Class<? extends IFloodlightService>>();
		services.add(InterfaceShortestPathSwitching.class);
		return services; 
	}

	/**
     * Tell the module system which services we implement.
     */
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> 
			getServiceImpls() 
	{ 
        Map<Class<? extends IFloodlightService>, IFloodlightService> services =
        			new HashMap<Class<? extends IFloodlightService>, 
        					IFloodlightService>();
        // We are the class that implements the service
        services.put(InterfaceShortestPathSwitching.class, this);
        return services;
	}

	/**
     * Tell the module system which modules we depend on.
     */
	@Override
	public Collection<Class<? extends IFloodlightService>> 
			getModuleDependencies() 
	{
		Collection<Class<? extends IFloodlightService >> modules =
	            new ArrayList<Class<? extends IFloodlightService>>();
		modules.add(IFloodlightProviderService.class);
		modules.add(ILinkDiscoveryService.class);
		modules.add(IDeviceService.class);
        return modules;
	}
}
