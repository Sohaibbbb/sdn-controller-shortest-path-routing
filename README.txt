	
	
CS557, Fall 2018 - Project 1: Software Defined Networking
---------------------------------------------------------
* by Vignesh M. Pagadala
* Vignesh.Pagadala@colostate.edu
--------------------------------

i) How Far I Got
----------------
* I completed all project requirements - managed to get Floodlight to install shortest-path routes in the switches, for all topology types in the static case, and in the dynamic case as well. Works perfectly with all topologies mentioned in the project description.

ii) Changes Made to Controller
------------------------------
1. I used Dijkstra's shortest path algorithm, to compute shortest paths from a given switch to every other switch in the network. I used the implementation provided by 'Baeldung' (https://www.baeldung.com/java-dijkstra) to compute shortest paths. This implementation provided an interface for defining nodes and connections in a graph. 

2. In the deviceAdded() function (in ShortestPathSwitching.java), I initially got the newly added host's information, and iterated through all switches, computing the shortest path to get to this host's switch, in each case. I had to 'translate' the node information in the controller, to make it compatible with the interface of Baeldung's Dijkstra implementation. And for each switch, I used the obtained shortest path information to know what the optimal 'next-hop' switch was. After this, I simply added a rule with the host's MAC address (as the matching criteria) and the port through which the current switch is connected to the next-hop switch (as the action). 

3. In the deviceRemoved() function, I iterated through each switch, removing the rule associated with the host (which got removed).

4. In the deviceMoved() function, I did the same procedure as in 1, updating the route in each node for the moved device.

5. In the switchRemoved() function, I chose to update the entire topology, after a switch was removed. I iterated through each host, and followed 1 for each host. 

6. Same as above was done in the linkDiscovery update function. I chose to update the entire topology, in the case that a link was changed.   

iii) Switch Flow Tables - Sample Output
---------------------------------------

'someloops' topology:
--------------------

Static Case
-----------

someloops - s1
--------------------
mininet@mininet-VirtualBox:~/openflow$ sudo ovs-ofctl dump-flows s1 -O OpenFlow13
OFPST_FLOW reply (OF1.3) (xid=0x2):
 cookie=0x0, duration=9.678s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:03 actions=output:3
 cookie=0x0, duration=9.688s, table=0, n_packets=6, n_bytes=588, priority=1,ip,dl_dst=00:00:00:00:00:01 actions=output:1
 cookie=0x0, duration=9.722s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:04 actions=output:3
 cookie=0x0, duration=9.726s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:02 actions=output:2

someloops - s6
--------------
mininet@mininet-VirtualBox:~/openflow$ sudo ovs-ofctl dump-flows s6 -O OpenFlow13
OFPST_FLOW reply (OF1.3) (xid=0x2):
 cookie=0x0, duration=41.566s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:03 actions=output:3
 cookie=0x0, duration=41.594s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:01 actions=output:4
 cookie=0x0, duration=41.597s, table=0, n_packets=6, n_bytes=588, priority=1,ip,dl_dst=00:00:00:00:00:04 actions=output:1
 cookie=0x0, duration=41.602s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:02 actions=output:3

Dynamic Case
-------------

someloops - s4 - link between s4 and h3 is down
-----------------------------------------------
mininet@mininet-VirtualBox:~/openflow$ sudo ovs-ofctl dump-flows s4 -O OpenFlow13
OFPST_FLOW reply (OF1.3) (xid=0x2):
 cookie=0x0, duration=8.903s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:01 actions=output:4
 cookie=0x0, duration=8.905s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:04 actions=output:4
 cookie=0x0, duration=8.906s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:02 actions=output:3

someloops - s4 - link between s4 and h3 is up again
---------------------------------------------------
mininet@mininet-VirtualBox:~/openflow$ sudo ovs-ofctl dump-flows s4 -O OpenFlow13
OFPST_FLOW reply (OF1.3) (xid=0x2):
 cookie=0x0, duration=2.369s, table=0, n_packets=0, n_bytes=0, priority=1,ip,dl_dst=00:00:00:00:00:03 actions=output:1
 cookie=0x0, duration=2.552s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:01 actions=output:4
 cookie=0x0, duration=2.552s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:04 actions=output:4
 cookie=0x0, duration=2.552s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:02 actions=output:3


