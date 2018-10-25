# SDN Controller Shortest-Path Routing

A Software-Defined Networking (SDN) controller which implements shortest-path routing in a packet-switched network.


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

---

## About

An SDN controller tool built using Floodlight, programmed to ensure shortest-path routing which works with different types of network topologies. This implementation is tested using the Mininet network emulator, which is designed to emulate various topologies containing OpenFlow switches, each of which connect to an OpenFlow network controller. This SDN implementation works successfully with various network topologies, and is also capable of dynamically re-configuring itself to find the shortest path, when the topology undergoes a change.

The shortest path algorithm used for this project is Djikstra's shortest path algorithm. I used the implementation provided by Baeldung: https://www.baeldung.com/java-dijkstra to compute shortest paths, which provides a very useful API for defining nodes and connections in a graph.

A brief description of what some of the functions implemented in the ShortestPathSwitching.java file do:

1. deviceAdded(): I initially get the newly added host's information, and iterate through all switches, computing the shortest path to get to this host's switch, in each case. I had to 'translate' the node information in the controller, to make it compatible with the interface of Baeldung's Dijkstra implementation. And for each switch, I used the obtained shortest path information to know what the optimal 'next-hop' switch was. After this, I simply added a rule with the host's MAC address (as the matching criteria) and the port through which the current switch is connected to the next-hop switch (as the action).

2. deviceRemoved(): I iterate through each switch, removing the rule associated with the host (which got removed).

3. deviceMoved(): I perform the same procedure as in 1, updating the route in each node for the moved device.

4. switchRemoved(): Here, I update the entire topology, after a switch is removed. I iterate through each host, and follow step 1 for each host.

5. linkDiscovery(): Same procedure as in step 4. The entire topology gets updated, in case a link is changed.

### Built With

> **[?]**
> Please provide the technologies that are used in the project.

## Getting Started

### Prerequisites

> **[?]**
> What are the project requirements/dependencies?

### Installation

> **[?]**
> Proceed to describe how to install and get started with the project.

## Usage

> **[?]**
> How does one go about using it?
> Provide various use cases and code examples here.

## Roadmap

See the [open issues](https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/issues) for a list of proposed features (and known issues).

- [Top Feature Requests](https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/issues?q=label%3Aenhancement+is%3Aopen+sort%3Areactions-%2B1-desc) (Add your votes using the 👍 reaction)
- [Top Bugs](https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/issues?q=is%3Aissue+is%3Aopen+label%3Abug+sort%3Areactions-%2B1-desc) (Add your votes using the 👍 reaction)
- [Newest Bugs](https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/issues?q=is%3Aopen+is%3Aissue+label%3Abug)

## Support

> **[?]**
> Provide additional ways to contact the project maintainer/maintainers.

Reach out to the maintainer at one of the following places:

- [GitHub issues](https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/issues/new?assignees=&labels=question&template=04_SUPPORT_QUESTION.md&title=support%3A+)
- The email which is located [in GitHub profile](https://github.com/vignesh-pagadala)

## Project assistance

If you want to say **thank you** or/and support active development of SDN Controller Shortest-Path Routing:

- Add a [GitHub Star](https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing) to the project.
- Tweet about the SDN Controller Shortest-Path Routing on your Twitter.
- Write interesting articles about the project on [Dev.to](https://dev.to/), [Medium](https://medium.com/) or personal blog.

Together, we can make SDN Controller Shortest-Path Routing **better**!

## Contributing

First off, thanks for taking the time to contribute! Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make will benefit everybody else and are **greatly appreciated**.

We have set up a separate document containing our [contribution guidelines](docs/CONTRIBUTING.md).

Thank you for being involved!

## Authors & contributors

The original setup of this repository is by [Vignesh Pagadala](https://github.com/vignesh-pagadala).

For a full list of all authors and contributors, check [the contributor's page](https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/contributors).

## Security

SDN Controller Shortest-Path Routing follows good practices of security, but 100% security can't be granted in software.
SDN Controller Shortest-Path Routing is provided **"as is"** without any **warranty**. Use at your own risk.

_For more info, please refer to the [security](docs/SECURITY.md)._

## License

This project is licensed under the **MIT license**.

See [LICENSE](LICENSE) for more information.

## Acknowledgements

> **[?]**
> If your work was funded by any organization or institution, acknowledge their support here.
> In addition, if your work relies on other software libraries, or was inspired by looking at other work, it is appropriate to acknowledge this intellectual debt too.
