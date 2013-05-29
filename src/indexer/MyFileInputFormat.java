/**
 * 
 */
package indexer;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * @author cis455
 *
 */
public class MyFileInputFormat extends FileInputFormat<Text, Text> {



	public boolean isSplittable(FileSystem fs, Path filename){
		return false;
	}

	/* (non-Javadoc)
	 * @see org.apache.hadoop.mapreduce.InputFormat#createRecordReader(org.apache.hadoop.mapreduce.InputSplit, org.apache.hadoop.mapreduce.TaskAttemptContext)
	 */
	@Override
	public RecordReader<Text, Text> createRecordReader(
			org.apache.hadoop.mapreduce.InputSplit arg0, TaskAttemptContext arg1)
			throws IOException, InterruptedException {
		return new MyRecordReader();
	}
}
