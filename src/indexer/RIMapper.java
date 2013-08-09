/**
 * 
 */
package indexer;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.tartarus.snowball.SnowballStemmer;

/**
 * @author Yayang Tian
 * http://cis.upenn.edu/~yaytian
 */
public class RIMapper extends Mapper<Object, Text, Text, Text>{
	private Text outKey = new Text();
	private Text outValue = new Text();
	public void map(Object key, Text value, Context context) throws IOException, InterruptedException 
	{
		StringTokenizer itr = new StringTokenizer(value.toString());
		FileSplit fileSplit = (FileSplit)context.getInputSplit();
		String docHash = fileSplit.getPath().getName();
		String word;
		while (itr.hasMoreTokens()) {
			word = itr.nextToken();
			//tf & dvl
			outKey.set("\"\"\"\"" + docHash);
			outValue.set(word);
			context.write(outKey, outValue);
			//df
			outKey.set(word);
			outValue.set(docHash);
			context.write(outKey, outValue);
		}
	}
}
