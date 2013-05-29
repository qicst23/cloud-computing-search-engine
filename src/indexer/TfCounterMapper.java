/**
 * 
 */
package indexer;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

/**
 * @author cis455
 *
 */
public class TfCounterMapper  extends Mapper<Object, Text, Text, IntWritable>{

    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();
	public void map(Object key, Text value, Context context) throws IOException, InterruptedException 
	{
	      StringTokenizer itr = new StringTokenizer(value.toString());
			FileSplit fileSplit = (FileSplit)context.getInputSplit();
			String filename = fileSplit.getPath().getName();
	      while (itr.hasMoreTokens()) {
	        word.set(itr.nextToken() + "  " + filename);
	        context.write(word, one);
	      }
	}
}