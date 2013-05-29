/**
 * 
 */
package indexer;

import java.io.IOException;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * @author cis455
 *
 */
public class DVLReducer extends Reducer<Text,IntWritable,Text, DoubleWritable> {
	private DoubleWritable result = new DoubleWritable();

	public void reduce(Text key, Iterable<IntWritable> values, 
			Context context
			) throws IOException, InterruptedException {
		int sum = 0;
		for (IntWritable val : values) {
			sum += val.get();
		}
		result.set(Math.sqrt((double)sum));
		context.write(key, result);
	}
}
