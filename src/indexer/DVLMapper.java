/**
 * 
 */
package indexer;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * @author cis455
 *
 */
public class DVLMapper extends Mapper<Text, Text, Text, IntWritable>{

    private final static IntWritable squared = new IntWritable();
    private Text document = new Text();
	public void map(Text key, Text number, Context context) throws IOException, InterruptedException 
	{
	      	int num = Integer.valueOf(number.toString());
	      	String docHash = key.toString().split("\\s+")[1];
	      	document.set(docHash);
			squared.set(num*num);
			context.write(document, squared);
	}
}
