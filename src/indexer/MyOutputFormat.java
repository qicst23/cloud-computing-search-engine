/**
 * 
 */
package indexer;

import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.lib.MultipleOutputFormat;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.util.Progressable;

/**
 * @author cis455
 *
 */
public class MyOutputFormat <K, V> extends MultipleOutputFormat<K, V> {

	protected static class MyOutputFormatWriter<K, V> implements RecordWriter<K, V> {
		private static final String utf8 = "UTF-8";

		private DataOutputStream out;

		public MyOutputFormatWriter (DataOutputStream _out) throws IOException {
			this.out = _out;
		}

		/**
		 * Write the object to the byte stream, handling Text as a special case.
		 *
		 * @param o
		 *          the object to print
		 * @throws IOException
		 *           if the write throws, we pass it on
		 */
		private void writeObject(Object o) throws IOException {
			if (o instanceof Text) {
				Text to = (Text) o;
				out.write(to.getBytes(), 0, to.getLength());
			} else {
				out.write(o.toString().getBytes(utf8));
			}
		}

		private void writeKey(Object o) throws IOException {
			writeObject(o);
			out.write("\t".getBytes(utf8));
		}

		public synchronized void write(K key, V value) throws IOException {

			boolean nullKey = key == null || key instanceof NullWritable;
			boolean nullValue = value == null || value instanceof NullWritable;

			if (nullKey && nullValue) {
				return;
			}

			Object keyObj = key;

			if (nullKey) {
				keyObj = "value";
			}

			writeKey(keyObj);

			if (!nullValue) {
				writeObject(value);
			}
		}

		public synchronized void close(Reporter reporter) throws IOException {
				out.close();
		}
	}

	public RecordWriter<K, V> getRecordWriter(FileSystem ignored, JobConf job,
			String name, Progressable progress) throws IOException {
		Path file = FileOutputFormat.getTaskOutputPath(job, name);
		FileSystem fs = file.getFileSystem(job);
		FSDataOutputStream fileOut = fs.create(file, progress);
		return new RecordWriter<K, V>(fileOut);
	}

	/* (non-Javadoc)
	 * @see org.apache.hadoop.mapred.lib.MultipleOutputFormat#getBaseRecordWriter(org.apache.hadoop.fs.FileSystem, org.apache.hadoop.mapred.JobConf, java.lang.String, org.apache.hadoop.util.Progressable)
	 */
	@Override
	protected RecordWriter<K, V> getBaseRecordWriter(FileSystem arg0,
			JobConf arg1, String arg2, Progressable arg3) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
