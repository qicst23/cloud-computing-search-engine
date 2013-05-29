/**
 * 
 */
package indexer;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * @author cis455
 *
 */
public class MyRecordReader extends RecordReader<Text, Text> {

	private FileSplit filesplit;
	private Configuration config;
	private boolean finished = false;
	private Text key = new Text();
	private Text value = new Text();
	/* (non-Javadoc)
	 * @see org.apache.hadoop.mapreduce.RecordReader#close()
	 */
	@Override
	public void close() throws IOException {
		
	}

	/* (non-Javadoc)
	 * @see org.apache.hadoop.mapreduce.RecordReader#getCurrentKey()
	 */
	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		return key;
	}

	/* (non-Javadoc)
	 * @see org.apache.hadoop.mapreduce.RecordReader#getCurrentValue()
	 */
	@Override
	public Text getCurrentValue() throws IOException, InterruptedException {
		return value;
	}

	/* (non-Javadoc)
	 * @see org.apache.hadoop.mapreduce.RecordReader#getProgress()
	 */
	@Override
	public float getProgress() throws IOException, InterruptedException {
		if(finished) return 1; else return 0;
	}

	/* (non-Javadoc)
	 * @see org.apache.hadoop.mapreduce.RecordReader#initialize(org.apache.hadoop.mapreduce.InputSplit, org.apache.hadoop.mapreduce.TaskAttemptContext)
	 */
	@Override
	public void initialize(InputSplit arg0, TaskAttemptContext arg1)
			throws IOException, InterruptedException {
		filesplit = (FileSplit) arg0;
		config = arg1.getConfiguration();
	}

	/* (non-Javadoc)
	 * @see org.apache.hadoop.mapreduce.RecordReader#nextKeyValue()
	 */
	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		if(finished)
			return false;
		else{
		            byte[] file = new byte[(int)filesplit.getLength()];
		            Path path = filesplit.getPath();
		            FileSystem fs = path.getFileSystem(config);
		 
		            FSDataInputStream in = null;
		            try{
		                in = fs.open(path);
		                IOUtils.readFully(in, file, 0, file.length); 
		                key.set(path.getName());
		                value.set(file, 0, file.length);
		            } finally {
		                IOUtils.closeStream(in);
		            }
		            finished = true;
		            return true;
		}
	}

}
