# Developer README
The exposed API focuses around the same two classes
as the CLI: `sand.gcs.system.Orion` and `sand.gcs.system.Rigel`.

For details, please view the Scaladoc API at
http://current.cs.ucsb.edu/rigel/scala-gcs/api/#package.

## Embedding
Three methods are exposed for embedding distances into
a newly spawned instance of a GCS: `embed`, `embedLandmarks`,
and `embedNonLandmarks`. 

## Saving/Loading
### From Memory
The `storeCoordinate` method will take a node ID (`Int`) and a
coordinate embedding and store (or override) the value into
the coordinate system. You will need to specify an additional
`Boolean` parameter indicating if it is a landmark or a
non-landmark.

### From Disk
Two methods are exposed for saving and loading the GCS to disk,
the aptly named `save` and `load` methods. They both
take a filename as a `String` and save/load the data to that
file.

The files are formatted like so:

```
[L if landmark, N if non-landmark] [node id] [coord #1] [coord #2] ... [coord #n]
```

## Querying
The methods `query` and `distanceBetween` will take two node IDs
and return the embedded distance between the two. The two methods
are aliases for the same method, so either one works.
