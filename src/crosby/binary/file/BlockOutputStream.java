package crosby.binary.file;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.CodedOutputStream;

import crosby.binary.file.FileBlock.CompressFlags;

public class BlockOutputStream {
	public BlockOutputStream(OutputStream output) {
		this.outwrite = new DataOutputStream(output);
		this.compression = CompressFlags.DEFLATE;
	}

	public void setCompress(FileBlock.CompressFlags flag) {
		compression = flag;
	}
	
	public void setCompress(String s) {
		if (s.equals("none"))
			compression = CompressFlags.NONE;
		else if (s.equals("deflate"))
			compression = CompressFlags.DEFLATE;
		else 
			throw new Error("Unknown compression type: "+s);
	}

	/** Write a block with the stream's default compression flag */
	public void write(FileBlock block) throws IOException {
		this.write(block,compression);
	}

	/** Write a specific block with a specific compression flags */
	public void write(FileBlock block,FileBlock.CompressFlags compression) throws IOException {
		block.writeTo(outwrite,compression);
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
	CompressFlags compression;
}
