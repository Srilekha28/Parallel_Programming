import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;

import java.io.*;
import java.util.*;

public class InvertedIndexing2 {

    /* Mapper method will divide the file/document into chunks and
     * map the keyword against its count.
     */
    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {

        JobConf conf;
        public void configure( JobConf job ) {
            this.conf = job;
        }

        public void map(LongWritable docId, Text value, OutputCollector<Text, Text> output,
                        Reporter reporter) throws IOException {
            // retrieve # keywords from JobConf
            int argc = Integer.parseInt(conf.get("argc"));
            // dividing the file into multiple chunks
            FileSplit fileSplit = (FileSplit) reporter.getInputSplit();
            // retreiving the filename from the path obtained by the above fileSplit
            String filename = "" + fileSplit.getPath( ).getName( );
            //creating a dictionary of keywords and extracting the arguments by
            // explicitly calculating it in the main function
            Set<String> keywords = new HashSet<>();
            for ( int i = 0; i < argc; i++ ) {
                keywords.add(conf.get("keyword" + i));
            }

            // creates a dictionary where the count of keywords is stored
            java.util.Map<String, Integer> keywordCount = new HashMap<>();
            // creates a list of words in a chunk obtained from a document
            String[] words = value.toString().split(" +");

            /* Here the loop will take word from words list
             * and check if that word is already present keywords dictionary ( which
             * contains the words given during runtime). and if the word is already present
             * in keywords dictionary then we will check if that word is also present in keywordCount
             * if yes then we will just incerement the count otherwise we will add that word to keywordCount dictionary
             */
            for (String word : words)
            {
                if (keywords.contains(word))
                {
                    if (keywordCount.containsKey(word))
                    {
                        keywordCount.put(word, keywordCount.get(word) + 1);
                    }
                    else
                    {
                        keywordCount.put(word, 1);
                    }
                }
            }

            /* mapper will be collecting the output in a specific format i.e..
             * ("TCP fileA", "fileA count")
             */
            keywordCount.forEach((keyword, count) ->
            {
                try {
                    output.collect(new Text(keyword + SPACE + filename), new Text(filename + SPACE + count));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /* Combiner method will take the output collected by the mapper as
     * input and will reduce the dictionary count by taking keyword as base
     * and add the count of the keywords with same filename.
     */
    public static class Combiner extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
        @Override
        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {

            String keyword = key.toString().split(SPACE)[0];
            // dictionary to store the reduced values of keywords and count
            java.util.Map<String, Integer> fileToKeywordCountMap = new HashMap<>();

            /* the loop will check if there are anymore values left in the output collected from mapper or not
             * if fileToKeywordCountMap already has the filename obtained from mapper then we will icrement the cpunt
             * else we will add that filename with count to the fileToKeywordCountMap dictionary.
             */
            while (values.hasNext())
            {
                Text curr = values.next();
                String filename = curr.toString().split(SPACE)[0];
                Integer count = Integer.parseInt(curr.toString().split(SPACE)[1]);

                if (fileToKeywordCountMap.containsKey(filename))
                {
                    fileToKeywordCountMap.put(filename, fileToKeywordCountMap.get(filename) + count);
                }
                else
                {
                    fileToKeywordCountMap.put(filename, count);
                }
            }

            /* Combiner will be collecting the output in a specific format
             * i.e.. if input from mapper is : ("TCP fileA", "fileA 3") (TCP fileA", "fileA 5")
             * combiner will store it as : (TCP fileA", "fileA 8").
             */
            fileToKeywordCountMap.forEach((fileName, count) -> {
                try {
                    output.collect(new Text(keyword + SPACE + fileName), new Text(fileName + SPACE + count));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /* Reduce method will be taking the output of combiner as input
     * and will do the last reduction on the data based on keyword.
     * Ex: input: (TCP fileA", "fileA 8") , (TCP fileB", "fileB 10")
     *     output: (TCP : "fileA 8", "fileB 10")
     */
    public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
        private final Text word = new Text();
        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            StringBuilder strBuilder = new StringBuilder();
            String keyword = key.toString().split(SPACE)[0];
            word.set(keyword);
            while (values.hasNext())
            {
                strBuilder.append(values.next().toString()).append(SPACE);
            }
            output.collect(word, new Text(strBuilder.toString()));
        }
    }

    // Partitioner method will be dividing the document into chunks
    public static class Partitioner extends MapReduceBase implements org.apache.hadoop.mapred.Partitioner<Text, Text>
    {
        @Override
        public int getPartition(Text key, Text value, int numPartitions) {
            final String keyword = key.toString().split(SPACE)[0];
            return keyword.hashCode() % numPartitions;
        }
    }

    public static void main(String[] args) throws Exception {
        JobConf conf = new JobConf(InvertedIndexing2.class);
        conf.setJobName("invertedIndexing");

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(SequenceFile.class);

        conf.setMapperClass(Map.class);
        conf.setCombinerClass(Combiner.class);
        conf.setReducerClass(Reduce.class);
        conf.setPartitionerClass(Partitioner.class);

        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        conf.set( "argc", String.valueOf( args.length - 2 ) ); // argc maintains #keywords
        for ( int i = 0; i < args.length - 2; i++ )
            conf.set( "keyword" + i, args[i + 2] );

        JobClient.runJob(conf);
    }

    private static final String SPACE = " ";
}
