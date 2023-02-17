import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

public class SparkKMeansClustering {
    public static void main(String[] args) throws IOException {

        SparkConf conf = new SparkConf().setAppName("Spark KMeans clustering");
        JavaSparkContext javaSparkContext = new JavaSparkContext(conf);

        String inputFile = args[0];
        long stime;
        stime = System.currentTimeMillis();
        JavaRDD<String> lines = javaSparkContext.textFile(inputFile);
        /**
         * reads all the points in input file and stores them in points object
         */
        JavaRDD<Point> points = lines.map(line ->
        {
            String[] coords = line.split("\\s+");
            return new Point(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
        });
//        System.out.println("[Srilu] time taken for reading - " + (System.currentTimeMillis() - stime));
        // starting a timer
        long startTime = System.currentTimeMillis();

        /**
         * Adding centroids to a list named centroids
         */
        List<Point> centroids = new ArrayList<>();
        centroids.add(new Point(568501, 652085));
        centroids.add(new Point(586036, 676110));
        centroids.add(new Point(573455, 677009));
        centroids.add(new Point(577414, 685128));
        centroids.add(new Point(705584, 876110));
        centroids.add(new Point(984816, 669334));

        // creating a centroidToClusterPair to store the values of centroid and point in form of a pair
        JavaPairRDD<Point, Point> centroidToClusterPair;

        long timeTakenForDistanceCal = 0;
        long timeTakenForPointsPerCentroidCal = 0;
        long timeTakenForCentroidReCal = 0;
        long timeTakenForCentroidUpdate = 0;
//        AtomicInteger cUpdateTimes = new AtomicInteger();
        while (true)
        {
            // System.out.println("[K-Means] In while loop.......");
            stime = System.currentTimeMillis();
            // Generate JavaRDDPair with centroid as key and value as point that belong to centroid
            /**
             * Here we will be taking each point from Points object and will be calculating distance to every
             * centroid from our centroid list.
             * shortestDistance :I have stored the distance in an array because, creating a new string or int takes more time
             *                   and as the address of the variable changes the time to access the variable also increases.
             *                   In the case of list , we will be overriding the same digit again and again and the address of
             *                   the list remains same which decreases our access time.
             *                   I have assigned the value inside to list to MAXINT so that it becomes easy to override in future
             * closestCentroid : I have stored the closestCentroid point in the form of AtomicReference to decrease the access time
             * we will be accessing each point from centroids list and will be calculating
             * the "currentDistance" by using ("calculateDistance" --> calculates eucledian distance method which is implemented in Point class.
             * the centroid which has least distance to the point will be stored in "closestCentroid".
             * After all the centroid to single point distances are calculated, the least new centroid and point will be added to "centroidToClusterPair".
             *
             */
            centroidToClusterPair = points.mapToPair(point -> {
                final double[] shortestDistance = {Integer.MAX_VALUE};
                int count = 0;
                AtomicReference<Point> closestCentroid = new AtomicReference<>();
                centroids.forEach(centroid -> {
                    double currentDistance = Point.calculateDistance(point, centroid);
                    if (currentDistance < shortestDistance[0]) {
                        shortestDistance[0] = currentDistance;
                        closestCentroid.set(centroid);
                    }
                });

                return new Tuple2<Point, Point>(closestCentroid.get(), point);
            });

            // calculating the time taken for distance calculation
            timeTakenForDistanceCal += System.currentTimeMillis() - stime;

            //start time
            stime = System.currentTimeMillis();

            /**
             * we will be creating a hash map of how many times a key is repeated i.e..
             * count of how many points are assigned to single cluster is calculated.
             */
            Map<Point, Long> pointsPerCentroid = centroidToClusterPair.countByKey();

            // Time taken to calculate how many points are assigned to a cluster
            timeTakenForPointsPerCentroidCal += System.currentTimeMillis() - stime;
            stime = System.currentTimeMillis();

            /**
             * Here to calculate the new centroid of each cluster , we have to calculate the mean of all points
             * and it is difficult to calculate the mean without knowing which cluster it is assigned to.
             * So, i created a pair of points named "oldToNewCentroidUnDivided", and reduced all the points/tuples
             * in "centroidToClusterPair" by reducing with key.
             * Ex: input points size of cluster 1 : 20,000
             *     step 1 of reduceByKey: 10,000 and all the X and Y coordinates of all the points
             *     step 2 of reduceByKey: 5,000 and all the X and Y coordinates of all the points
             *                  .
             *                  .
             *                  .
             *     last step : final sum of all the X and Y coordinates of all the points in the cluster will be calculated
             * Note: didnt increment the count after point calculation/addition to avoid data redundancy
             */
            JavaPairRDD<Point, Point> oldToNewCentroidUnDivided = centroidToClusterPair.reduceByKey((point1, point2) -> {
                point1.x += point2.x;
                point1.y += point2.y;
                return point1;
            });
            timeTakenForCentroidReCal += System.currentTimeMillis() - stime;
            stime = System.currentTimeMillis();

            // boolean variable to keep track of when to stop calculating the centroids.
            boolean shouldStop = true;
            int i = 0;
            /**
             * Here we will be calculating the mean of the points in a cluster and check if we should move forward or not.
             * we will be taking each point from "oldToNewCentroidUnDivided" and extract the centroid and point from the key,value pair
             * As we already calculated the number of points assigned to each cluster using "pointsPerCentroid" ,
             * we will be dividing our point with pointsPerCentroid and check if it same as the centroid, if it is not same
             * then the boolean "centroidNotChanged" will be set to false and, shouldStop will also be set to False and
             * again the whole process of calculating "centroidToClusterPair" , "pointsPerCentroid"
             * and "oldToNewCentroidUnDivided" needs to be calculated.
             * if the boolean "centroidNotChanged" is True, as we initially set our shouldStop variable to True .
             * Then that is when we know that all the centroids have not changed and we will break from the loop
             */
            for (Map.Entry<Point, Point> s : oldToNewCentroidUnDivided.collectAsMap().entrySet())
            {
                Point centroid = s.getKey();
                Point point = s.getValue();
                long countOfPoints = (long) pointsPerCentroid.get(centroid);
                centroids.set(i, new Point(point.x / countOfPoints, point.y / countOfPoints));
                boolean centroidNotChanged = (centroid.x == (point.x / countOfPoints)) && (centroid.y == (point.y / countOfPoints));
                shouldStop = shouldStop && centroidNotChanged;
                i++;
            }
            timeTakenForCentroidUpdate += System.currentTimeMillis() - stime;

            if (shouldStop)
            {
                break;
            }
        }

        System.out.println("[Spark-KMeans] total time taken for timeTakenForDistanceCal " + timeTakenForDistanceCal + "ms");
        System.out.println("[Spark-KMeans] total time taken for timeTakenForPointsPerCentroidCal " + timeTakenForPointsPerCentroidCal + "ms");
        System.out.println("[Spark-KMeans] total time taken for timeTakenForCentroidReCal  " + timeTakenForCentroidReCal + "ms");
        System.out.println("[Spark-KMeans] total time taken for timeTakenForCentroidUpdate  " + timeTakenForCentroidUpdate + "ms");
//        System.out.println("[Spark-KMeans] total cUpdateTimes - " + cUpdateTimes);

        System.out.println("[Spark-KMeans] Total time taken for Spark based K-Means clustering " +
                (System.currentTimeMillis() - startTime) + "ms");
        AtomicInteger i = new AtomicInteger(1);
        centroidToClusterPair.groupByKey().collectAsMap().forEach((centroid, cluster) ->
        {

            System.out.println("[Srilu] Cluster " + i);
            System.out.println("[Srilu] Centroid - " + centroid);

            for (Point point : cluster) {
                System.out.println(point);
            }

            System.out.println("----------------");
            i.getAndIncrement();
        });
    }
}