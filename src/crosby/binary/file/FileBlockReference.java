package crosby.binary.file;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import crosby.binary.Fileformat;

/** Represent a reference to a block, containing the metadata of the block 
 * 
 * We can turn this into a 'real' block by appropriately seeking into the file and doing a 'read'.
 * 
 * */
public class FileBlockReference extends FileBlockBase {
	protected FileBlockReference(String type, ByteString indexdata) {
		super(type,indexdata);
	}
	
	public FileBlock parseFrom(byte buf[]) throws InvalidProtocolBufferException {
		FileBlock out = FileBlock.newInstance(type, indexdata);
		Fileformat.Blob blob = Fileformat.Blob.parseFrom(buf);
		if (blob.hasRaw()) {
			out.data = blob.getRaw();
		}
		else if (blob.hasZlibData()) {
			byte buf2[] = new byte[blob.getRawSize()];
			Inflater decompresser = new Inflater();
			 decompresser.setInput(blob.getZlibData().toByteArray());
			 //decompresser.getRemaining();
			 try {
				decompresser.inflate(buf2);
			} catch (DataFormatException e) {
				e.printStackTrace();
				throw new Error(e);
			}
			 assert (decompresser.finished());
			 decompresser.end();
			 out.data = ByteString.copyFrom(buf2);
		}			 
		return out;
	}
	
	
	public int getDatasize() {
		return datasize;
	}

	static FileBlockReference newInstance(FileBlockBase base, long offset, int length) {
		FileBlockReference out = new FileBlockReference(base.type, base.indexdata);
		out.datasize = length;
		out.data_offset = offset;
		return out;
	}

	public FileBlock read(DataInputStream input) throws IOException {
		// TODO: SEEK.
		assert false;
		byte buf[] = new byte[getDatasize()];
		input.readFully(buf);
		return parseFrom(buf);
	}
	

	protected int datasize;
	/** Offset into the file of the data part of the block */
	long data_offset;
}
