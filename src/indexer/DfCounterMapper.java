/**
 * 
 */
package indexer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * @author cis455
 *
 */
public class DfCounterMapper extends Mapper<Object, Text, Text, IntWritable>{

	private Text word = new Text();
	private final static IntWritable one = new IntWritable(1);
	public void map(Object key, Text value, Context context) throws IOException, InterruptedException 
	{
		StringTokenizer itr = new StringTokenizer(value.toString());
		Set<String> uniqueWords = new HashSet<String>();
		while (itr.hasMoreTokens()) {
			word.set(itr.nextToken());
			uniqueWords.add(word.toString());
		}
		for(String x: uniqueWords){
			word.set(x);
			context.write(word, one);
		}
	}
}