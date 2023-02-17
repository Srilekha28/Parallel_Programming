import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ShortestPath {
    public static void main(String[] args)
    {
        String inputFile = args[0];
        String sourceVertexId = args[1];
        String destVertexId = args[2];

        SparkConf conf = new SparkConf().setAppName("BFS-based Shortest Path Search");
        JavaSparkContext javaSparkContext = new JavaSparkContext(conf);
        JavaRDD<String> lines = javaSparkContext.textFile(inputFile);

        // starting a timer
        long startTime = System.currentTimeMillis();

        /**
         * Read every line from input graph txt file,
         * for each line: vertex=destVertex1,linkWeight1;destVertex2,linkWeight2
         * we create a "vertex : Data object"
         * Data object contains - neighbors, status, distance, prev distance
         * For initialization for source vertex, we will be setting distance/prev as 0
         * For all other vertices, distance/prev will be set to MAXInt
         */
        JavaPairRDD<String, Data> network = lines.mapToPair(line -> {
            /**
             * Splitting the file based on lines , and will be dividing the line into vertexId on left
             * and separate it using "=" , where the right part of the line will be neighbours
             * of the vertex and their corresponding weights which will be stored in "connectivityInfo"
             * Ex: input : 0=3,4;1,3
             *     processing : split the input line by using "="
             *     processed output: 0 , 3,4;1,3
             *     fianl output produced : vertex = 0
             *                             connectivityInfo = 3,4;1,3
             */
            String[] lineSplit = line.split("=");
            String vertex = lineSplit[0];
            String connectivityInfo = lineSplit[1];

            /**
             * we will create a tuple for connectivityInfo.
             * There might be a possibility that one verterx can have multiple neighbours in that case
             * we will differentiate neighbours by ";" symbol.
             * each neighbour of the vertex will have its own vertexId and weight which can be abstracted
             * by their respective positions.
             * Ex : input : 3,4;1,3
             *      processing : split the input line by using ";"
             *      connection1 = 3,4 ; connection2 = 1,3
             *      split each connection using ","
             *      processed output : 3 , 4
             *      final output produced : destVertex = 3
             *                              linkWeight = 4
             *     form a tuple of (destVertex , linkWeight) and add it to neighbors list
             */
            final List<Tuple2<String, Integer>> neighbors = new ArrayList<>();
            for (String connection : connectivityInfo.split(";"))
            {
                String[] connSplit = connection.split(",");
                String destVertex = connSplit[0];
                String linkWeight = connSplit[1];
                neighbors.add(new Tuple2<>(destVertex, Integer.parseInt(linkWeight)));
            }

            /**
             * Here if the sourceVertexId is same as vertex then
             *      we return a tuple which consists neighbors of the vertex , and
             *      will set the distance to zero and the previous distance to 0 and
             *      set the STATUS as "ACTIVE"
             * If sourceVertexId and vertex are different then
             *      we return a tuple which consists the neighbors of the vertex, and
             *      set the distance and previous distance to maxvalues
             *      -because we dont know what the actual distances and values
             *      and set the status of the vertex as "INACTIVE"
             */
            if (vertex.equals(sourceVertexId))
            {
                return new Tuple2<>(vertex, new Data(neighbors, 0, 0, "ACTIVE"));
            }
            else
            {
                return new Tuple2<>(vertex, new Data(neighbors, Integer.MAX_VALUE, Integer.MAX_VALUE, "INACTIVE"));
            }
        });

        /**
         *  To claculate the distance from one vertex to others, first we have to know
         *  which vertex is in "ACTIVE" state.
         *  To know the state of the vertex i have written a isActiveVertexPresent function
         *  which checks return the actual status of the vertex.
         *  If the status of the network is "ACTIVE" then we will create a propagatedNetwork
         *  which will contain the vertexes that are being visited.
         *  we will create a tuple for propagatedNodes to
         *  get neighbor data node, so that we can change prev distance and include it to the new data.
         *  we will update network everytime by reducing the propagatedNetwork and
         *  after the updation of new data we will set the previous vertex to "INACTIVE"
         *  explicitly to stop the loop, else the loop will never end.
         */
        while (isActiveVertexPresent(network))
        {
            JavaPairRDD<String, Data> propagatedNetwork = network.flatMapToPair( vertex ->
            {
                String vertexId = vertex._1();
                Data data = vertex._2();

                List<Tuple2<String, Data>> propagatedNodes = new ArrayList<>();

                if (ACTIVE.equals(data.status))
                {
                    propagatedNodes = data.neighbors.stream().map(neighbor ->
                    {
                        // get neighbor data node, so that we can change prev distance and include it
                        return new Tuple2<>(neighbor._1, new Data(null, Integer.MAX_VALUE, data.distance + neighbor._2(), INACTIVE));
                    }).collect(Collectors.toList());
                }
                propagatedNodes.add(vertex);
                return propagatedNodes.iterator();
            });

            /**
             * updated new network will be produced by reducing the propagatedNetwork using merge method from Data.java
             */
            network = propagatedNetwork.reduceByKey(Data::merge);

            //
            network = network.mapValues(value ->
            {
                if (value.distance > value.prev)
                {
                    return new Data(value.neighbors, value.prev, value.prev, ACTIVE);
                }
                else
                {
                    return new Data(value.neighbors, value.distance, value.prev, INACTIVE);
                }
            });
        }

         Integer distance =    network.collect().stream().filter(tuple -> {
            return destVertexId.equals(tuple._1());
        }).findFirst().map(stringDataTuple2 -> stringDataTuple2._2().distance).orElse(-1);

        System.out.println("[ShortestPath Log] Total time taken " + (System.currentTimeMillis() - startTime) + "ms");
        System.out.printf("[ShortestPath Log] from %s to %s takes distance = %s%n", sourceVertexId, destVertexId, distance);
    }

    /**
     * method to check the status of the vertex
     * @param network
     * @return TRUE/FALSE
     */
    private static boolean isActiveVertexPresent(JavaPairRDD<String, Data> network)
    {
        return network.collect().stream().anyMatch(tuple -> ACTIVE.equals(tuple._2().status));
    }

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";
}
