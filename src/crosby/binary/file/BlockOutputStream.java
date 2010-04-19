package crosby.binary.file;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.CodedOutputStream;

public class BlockOutputStream {
	public BlockOutputStream(OutputStream output) {
		this.outwrite = new DataOutputStream(output);
	}
	
	public void write(FileBlock block) throws IOException {
		block.writeTo(outwrite,FileBlock.CompressFlags.DEFLATE);
		writtenblocks.add(block);
	}

	public void flush() throws IOException {
		outwrite.flush();	
	}

	public void close() throws IOException {
		outwrite.flush();
		outwrite.close();
	}
	
	DataOutputStream outwrite;
	List<FileBlock> writtenblocks = new ArrayList<FileBlock>();
}
