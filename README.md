# SDN Controller Shortest-Path Routing

A Software-Defined Networking (SDN) controller which implements shortest-path routing in a packet-switched network.

---

## About

An SDN controller tool built using Floodlight, configured to ensure shortest-path routing which works with different types of network topologies. This implementation is tested using the Mininet network emulator, which is designed to emulate various topologies containing OpenFlow switches, each of which connect to an OpenFlow network controller. This SDN implementation works successfully with various network topologies, and is also capable of dynamically re-configuring itself to find the shortest path, when the topology undergoes a change.

The shortest path algorithm used for this project is Djikstra's shortest path algorithm. I used the implementation provided by Baeldung: https://www.baeldung.com/java-dijkstra to compute shortest paths, which provides a very useful API for defining nodes and connections in a graph.

This project was done as a part of the course CS557: Advanced Networking, at Colorado State University, during the Fall of 2018.

A brief description of what some of the functions implemented in the ShortestPathSwitching.java file do:

1. deviceAdded(): I initially get the newly added host's information, and iterate through all switches, computing the shortest path to get to this host's switch, in each case. I had to 'translate' the node information in the controller, to make it compatible with the interface of Baeldung's Dijkstra implementation. And for each switch, I used the obtained shortest path information to know what the optimal 'next-hop' switch was. After this, I simply added a rule with the host's MAC address (as the matching criteria) and the port through which the current switch is connected to the next-hop switch (as the action).

2. deviceRemoved(): I iterate through each switch, removing the rule associated with the host (which got removed).

3. deviceMoved(): I perform the same procedure as in 1, updating the route in each node for the moved device.

4. switchRemoved(): Here, I update the entire topology, after a switch is removed. I iterate through each host, and follow step 1 for each host.

5. linkDiscovery(): Same procedure as in step 4. The entire topology gets updated, in case a link is changed.

The various topology types, and their description are provided <a href = "https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/blob/main/assets/SDN-Brown.pdf">here</a>.  Sample output from the flow tables of some switched are provided below, with respective switch numbers:

### Switch Flow Tables - Sample Output

#### Someloops Topology: Static Case

1. Someloops - S1

mininet@mininet-VirtualBox:~/openflow$ sudo ovs-ofctl dump-flows s1 -O OpenFlow13
OFPST_FLOW reply (OF1.3) (xid=0x2):
 cookie=0x0, duration=9.678s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:03 actions=output:3
 cookie=0x0, duration=9.688s, table=0, n_packets=6, n_bytes=588, priority=1,ip,dl_dst=00:00:00:00:00:01 actions=output:1
 cookie=0x0, duration=9.722s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:04 actions=output:3
 cookie=0x0, duration=9.726s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:02 actions=output:2
 
2. Someloops - S6

mininet@mininet-VirtualBox:~/openflow$ sudo ovs-ofctl dump-flows s6 -O OpenFlow13
OFPST_FLOW reply (OF1.3) (xid=0x2):
 cookie=0x0, duration=41.566s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:03 actions=output:3
 cookie=0x0, duration=41.594s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:01 actions=output:4
 cookie=0x0, duration=41.597s, table=0, n_packets=6, n_bytes=588, priority=1,ip,dl_dst=00:00:00:00:00:04 actions=output:1
 cookie=0x0, duration=41.602s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:02 actions=output:3

#### Someloops Topology: Dynamic Case

1. Someloops - S4 - link between S4 and H3 is down

mininet@mininet-VirtualBox:~/openflow$ sudo ovs-ofctl dump-flows s4 -O OpenFlow13
OFPST_FLOW reply (OF1.3) (xid=0x2):
 cookie=0x0, duration=8.903s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:01 actions=output:4
 cookie=0x0, duration=8.905s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:04 actions=output:4
 cookie=0x0, duration=8.906s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:02 actions=output:3

2. Someloops - S4 - link between S4 and H3 is up again

mininet@mininet-VirtualBox:~/openflow$ sudo ovs-ofctl dump-flows s4 -O OpenFlow13
OFPST_FLOW reply (OF1.3) (xid=0x2):
 cookie=0x0, duration=2.369s, table=0, n_packets=0, n_bytes=0, priority=1,ip,dl_dst=00:00:00:00:00:03 actions=output:1
 cookie=0x0, duration=2.552s, table=0, n_packets=2, n_bytes=196, priority=1,ip,dl_dst=00:00:00:00:00:01 actions=output:4
 cookie=0x0, duration=2.552s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:04 actions=output:4
 cookie=0x0, duration=2.552s, table=0, n_packets=4, n_bytes=392, priority=1,ip,dl_dst=00:00:00:00:00:02 actions=output:3

### Built With

1. Floodlight: https://projectfloodlight.org
2. OpenFlow 1.0: https://www.opennetworking.org/sdn-resources/onf-specifications/openflow
3. Java 8: https://www.oracle.com/java/technologies/java8.html

## Getting Started

### Prerequisites

1. Java 8: https://www.oracle.com/java/technologies/java8.html
2. Python 3: https://www.anaconda.com/products/individual
3. Mininet: https://mininet.org
4. Floodlight: https://projectfloodlight.org
5. OpenFlow 1.0: https://www.opennetworking.org/sdn-resources/onf-specifications/openflow
6. VirtualBox: https://www.virtualbox.org/wiki/Downloads

### Installation

1. Ensure all the above-mentioned prerequisites are met.
2. Clone the GitHub repo:

    `git clone https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing.git`.

3. Download the Virtualbox .ova file from <a href = "https://drive.google.com/drive/folders/1zdoxMkhPzJ7dYGt-NbncSo8QyT5Npnhm?usp=sharing">here</a>.
4. Set up the Virtual machine in Virtualbox and boot it up.
5. Open up a terminal. Compile application using:

    `cd ~/openflow`

    `ant`

## Usage

1. Start Floodlight:

    `java -jar FloodlightWithApps.jar -cf shortestPathSwitching.prop`

2. Start Mininet:

    `sudo ./run_mininet.py single,3`

    The above command creates a topology with three hosts and one switch. More info regarding this can be found at <a href = "https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/blob/main/assets/SDN-Brown.pdf">this</a> document.

3. Various commands can now be run to test the routing algorithm. For example 'ping'. The initial ping will get dropped, as the rules would not have been configured yet, but after that, the rules will be in place, and the ping will successfully follow the shortest-path route.

## Roadmap

See the [open issues](https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/issues) for a list of proposed features (and known issues).

- [Top Feature Requests](https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/issues?q=label%3Aenhancement+is%3Aopen+sort%3Areactions-%2B1-desc) (Add your votes using the 👍 reaction)
- [Top Bugs](https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/issues?q=is%3Aissue+is%3Aopen+label%3Abug+sort%3Areactions-%2B1-desc) (Add your votes using the 👍 reaction)
- [Newest Bugs](https://github.com/vignesh-pagadala/sdn-controller-shortest-path-routing/issues?q=is%3Aopen+is%3Aissue+label%3Abug)

## Support

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

1. Dr. Joseph Gersch (Joe.Gersch@ColoState.Edu), for being an amazing course instructor, and providing us the opprtunity to work on such an amazing project.
2. Baeldung (https://www.baeldung.com/java-dijkstra): Used their code to implement Djikstra's in the switches.
