package crosby.binary.file;

import java.io.DataInputStream;
import java.io.FileInputStream;
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
	
	public FileBlock parseData(byte buf[]) throws InvalidProtocolBufferException {
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

	public FileBlock read(InputStream input) throws IOException {
		if (input instanceof FileInputStream) {
			((FileInputStream)input).getChannel().position(data_offset);
			byte buf[] = new byte[getDatasize()];
			((DataInputStream)input).readFully(buf);
			return parseData(buf);
		} else {
			throw new Error("Random access binary reads require seekability");
		}
	}

	/** TODO: Convert this reference into a serialized representation that can be stored. */
	public ByteString serialize() {
		assert false; // TODO
		return null;
	}
	
	/** TODO: Parse a serialized representation of this block reference */
	static FileBlockReference parseFrom(ByteString b) {
		assert false; // TODO
		return null;
	}
	

	protected int datasize;
	/** Offset into the file of the data part of the block */
	long data_offset;
}
