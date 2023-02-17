import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class JavaWordCountFlatMap {
 
    public static void main(String[] args) {
        // configure spark
        SparkConf sparkConf = new SparkConf().setAppName("Java Word Count FlatMap")
	    .setMaster("local[2]").set("spark.executor.memory","2g");
        // start a spark context
        JavaSparkContext sc = new JavaSparkContext(sparkConf);
        
        // provide path to input text file
        String path = "sample.txt";
        
        // read text file to RDD
        JavaRDD<String> lines = sc.textFile(path);

	// Java 8 with lambdas: split the input string into words
        /**
         * Here I have implemented a words javaRDD which contains only strings
         * created an array list and added the words to it
         * divided the words by splitting it with a space
         * and return the list iterator
         */
        JavaRDD<String> words = lines.flatMap(new FlatMapFunction<String, String>() {
        @Override
        public Iterator<String> call(String line) throws Exception {
            ArrayList<String> list = new ArrayList<String>();
            for (String word: line.split(" ")){
                list.add(word);
            }
             return list.iterator();
        }
    });
        
        // print #words
	System.out.println( "#words = " + words.count( ) );
    }
 
}
