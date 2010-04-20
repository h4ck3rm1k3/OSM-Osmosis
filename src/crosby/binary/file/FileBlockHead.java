package crosby.binary.file;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.google.protobuf.ByteString;

import crosby.binary.Fileformat;
/** Represent the header of a fileblock when a set of fileblocks is read as in a stream
 * @author crosby
 *
 */
public class FileBlockHead extends FileBlockReference {
	protected FileBlockHead(String type, ByteString indexdata) {
		super(type,indexdata);
	}

	/** Read the header. After reading the header, either the contents must be skipped or read */
	static FileBlockHead readHead(DataInputStream input) throws IOException {
		int headersize = input.readInt();
		//System.out.format("Header size %d %x\n",headersize,headersize);
		byte buf[] = new byte[headersize];
		input.readFully(buf);
		//System.out.format("Read buffer for header of %d bytes\n",buf.length);
		Fileformat.FileBlockHeader header = Fileformat.FileBlockHeader.parseFrom(buf);
		FileBlockHead fileblock = new FileBlockHead(header.getType(),header.getIndexdata());

		fileblock.datasize = header.getDatasize();
		//data_offset = 
		return fileblock;
	}

	/** Assumes the stream is positioned over at the start of the data, skip over it. 
	 * @throws IOException */
	void skipContents(DataInputStream input) throws IOException {
		if (input.skip(getDatasize()) != getDatasize())
			assert false: "SHORT READ";
	}

	FileBlock readContents(DataInputStream input) throws IOException {
		byte buf[] = new byte[getDatasize()];
		input.readFully(buf);
		return parseFrom(buf);
	}
}
