# Graph Coordinate Systems
Graph Coordinate System (GCS) implementation for the JVM in
the Scala language. Both the Orion and Rigel system have
been implemented, with necessary components to experiment
with your own GCS provided.

## Immediately Relevant
API Online: http://current.cs.ucsb.edu/rigel/scala-gcs/api/#package

Download JAR: http://current.cs.ucsb.edu/rigel/scala-gcs/scala-gcs-0.1.0-RC2.jar

The JAR file contains all non-JDK6 dependencies including the Scala
standard library and the dependencies used, so the only dependencies
needed to run the JAR is JDK 6.

## Usage
The two classes that will be of particular interest are
`sand.gcs.system.Orion` and `sand.gcs.system.Rigel`.

### Command Line Interface
The CLI for each is simple. When calling the `main`
method for the class, you must specify the following
parameters:

1. The filename of the file containing the list of distances to embed.
   This file must have 3 columns, formatted like so:

```
[landmark id] [destination id] [distance]
```
   
   For a graph of L landmarks and V vertices, there
   should be L * V lines in the file.

   An example file is provided called `RandomGraph100SP.txt`.

2. An integer denoting the dimensionality of the system.

3. A filename denoting the file to write the results of the
   embedding out to. The format of the output will be formatted like so:

```
[L if landmark, N if non-landmark] [node id] [coord #1] [coord #2] ... [coord #n]
```

   For a graph of V vertices, there will be V lines in the output file.

4. For Rigel, you will also need to specify an integer denoting the curvature
   of the system.

5. For both Orion and Rigel, you can optionally specify a file containing a
   single column which denotes the IDs of the landmark nodes that should be
   embedded first (the "primary" landmarks that the rest of the landmarks will
   be embedded against).


Examples:
```
# Embeds the distances specified in the MyDistances.txt file in a 10 dimension Euclidean
# (Orion) space and store the results in MyResults.txt
$ java -cp gcs.jar sand.gcs.system.Orion MyDistances.txt 10 MyResults.txt

# Embeds the distances specified in an 8 dimension -1 curvature hyperbolic (Rigel) space,
# using the primary landmarks specified in PrimaryLandmarks.txt, and store
# the results in MyResults.txt.
$ java -cp gcs.jar sand.gcs.system.Rigel MyDistances.txt 10 MyResults.txt -1 PrimaryLandmarks.txt
```

### Programmatic Interface
Please see the separate developer README for details.

## Configuration
The default configuration parameters can be found in
`src/main/resources/reference.conf`. You can override any
of the parameters by providing an `application.conf` file
in the directory in which you will be running the code,
typically the root project directory. Note that some
parameters will need to be provided in `application.conf`
if you are running the distributed versions of the system.
More details can be found below.

## Distributed
Please note that the distributed implementation is intended
to be used as a command line tool only!

For the distributed implementation, the following series of
steps should be taken.

1. In your `application.conf` file in the root project directory,
   specify a list of strings in the `gcs.deploy.active` scope
   denoting the addresses of your worker machines. For each of
   those active machines, specify an additional value for
   `gcs.deploy.[worker address].nr-of-workers` that denotes how
   many Worker "actors" you want to spawn on that machine. A
   sample configuration file can be found below.

```
gcs {
    deploy {
        active = ["worker1.address.here"]

        worker1.address.here.nr-of-workers = 8
        worker2.address.here.nr-of-workers = 8
    }
}
```

Here we specify we will only spawn one Worker machine, which is
the machine with address `worker1.address.here`. When the system
finishes starting up, we will have 8 worker "actors" spawned on that
machine (corresponding roughly to 8 worker threads).

Note that there is a second worker machine specified with `nr-of-workers`,
but because it is not specified in the `active` list, it will be
ignored. This is done so you can keep a "permanent" list of address-to-threads
mapping while being able to change the list of active workers.

2. Once the `application.conf` file is finished, start an instance of
   `sand.gcs.system.distributed.Worker` on each machine in the `active` list.

3. Once the Worker machines load the instance, start
   `sand.gcs.system.distributed.DistributedOrion/Rigel` on a machine that can
   communicate with the Worker machines. The interface for this is the same as
   the local versions. This machine will then begin by embedding
   the landmarks in a single-threaded manner, after which it will begin distributing
   the work to the remote machines. 

## Building from source
The project is built using the [Simple Build Tool](http://www.scala-sbt.org/).
It is an easy to use build tool that will grab all dependencies and install them
onto your machine. You can install SBT via a
[package manager](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html)
or
[manually](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html#manual-installation)
(it is downloaded as a single JAR file).

To begin grabbing all dependencies and compiling the code, run `sbt ~compile`.
To package all the dependencies needed into a single JAR, run `sbt assembly`.
The JAR can then be found in the `target/scala-2.10` folder.

## Bugs
Please submit any bug reports through GitHub via the Issues page for the
project. If you do not have/wish to create a GitHub account, please email
Adelbert Chang at adelbertc at gmail dot com.

## Publications
* Xiaohan Zhao, Alessandra Sala, Haitao Zheng, Ben Y. Zhao. [Efficient Shortest
  Paths on Massive Social Graphs](http://current.cs.ucsb.edu/rigel/documents/rigel.pdf).
  Proceedings of the The 7th International
  Conference on Collaborative Computing: Networking, Applications, and
  Worksharing (CollaborateCom 2011) (Invited Paper). Orlando, USA, Oct 2011.
* Xiaohan Zhao, Alessandra Sala, Haitao Zheng, Ben Y. Zhao. [Fast and Scalable
  Analysis of Massive Social Graphs](http://arxiv.org/abs/1107.5114).
  In arXiv preprint arXiv:1107.5114.
* Xiaohan Zhao, Alessandra Sala, Christo Wilson, Haitao Zheng, Ben Y. Zhao.
  [Orion: Shortest Path Estimation for Large Social Graphs](http://current.cs.ucsb.edu/rigel/documents/orion.pdf). 
  Proceedings of The 3rd Workshop on Online Social Networks (WOSN). Boston, MA, June 2010.

## License
Copyright 2013 SAND Lab @ UC Santa Barbara

Licensed under the BSD 3-clause License:
[http://opensource.org/licenses/BSD-3-Clause](http://opensource.org/licenses/BSD-3-Clause)
