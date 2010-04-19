package crosby.binary.file;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.google.protobuf.ByteString;

import crosby.binary.Fileformat;
import crosby.binary.Fileformat.Blob;
import crosby.binary.Fileformat.FileBlockHeader;
import crosby.binary.Fileformat.FileBlockHeader.Builder;

public class FileBlock {
	/** An adaptor that receives blocks from an input stream */
	public interface Adaptor {
		/** Does the reader understand this block? Does it want the data in it? 
		 * 
		 * Data is not actually in the block and not available to a fetch.
		 * 
		 * Note that with an index, it may be that the reader is passed a lot of 'skipBlock' callbacks.
		 * Then, the blocks are returned in file order through handleblock.
		 * */
		boolean skipBlock(FileBlock message);
		/** Called with the data in the block */
		void handleBlock(FileBlock message);
	}


	/** Identifies the type of the data within a block */
	public final String type;
	/** Contains the contents of a block for use or further processing */
	private ByteString data; // serialized Format.Blob 

	/** Block metadata, stored in the index block and as a prefix for every block. */
	public final ByteString indexdata;

	private FileBlock(String type, ByteString blob, ByteString indexdata) {
		this.type = type;
		this.data = blob;
		this.indexdata = indexdata;
	}

	public static FileBlock newInstance(String type, ByteString blob, ByteString indexdata) {
		return new FileBlock(type,blob,indexdata);

	}

	enum CompressFlags {NONE, DEFLATE};

	protected void deflateInto(crosby.binary.Fileformat.Blob.Builder blobbuilder) {
		int size = data.size();
		Deflater deflater = new Deflater();
		deflater.setInput(data.toByteArray());
		deflater.finish();
		byte out[] = new byte[size];
		deflater.deflate(out);

		if (!deflater.finished()) {
			// Buffer wasn't long enough. Be noisy.
			System.out.println("Compressed buffer too short causing extra copy");
			out = Arrays.copyOf(out, size+size/64+16);
			deflater.deflate(out, deflater.getTotalOut(), out.length-deflater.getTotalOut());
			assert(deflater.finished());
		}
		ByteString compressed = ByteString.copyFrom(out,0,deflater.getTotalOut());
		blobbuilder.setZlibData(compressed);	
		deflater.end();
	}
	public void writeTo(DataOutputStream outwrite, CompressFlags flags) throws IOException {
		Fileformat.FileBlockHeader.Builder builder = Fileformat.FileBlockHeader.newBuilder();
		if (indexdata != null)
			builder.setIndexdata(indexdata);
		builder.setType(type);

		Fileformat.Blob.Builder blobbuilder = Fileformat.Blob.newBuilder();
		if (flags == CompressFlags.NONE) {
			blobbuilder.setRaw(data);
		} else {
			blobbuilder.setRawSize(data.size());
			if (flags == CompressFlags.DEFLATE)
				deflateInto(blobbuilder);
			else
				assert false : "TODO"; // TODO
		}	
		Fileformat.Blob blob = blobbuilder.build();

		builder.setDatasize(blob.getSerializedSize());
		Fileformat.FileBlockHeader message = builder.build();
		int size = message.getSerializedSize();

		//System.out.format("Outputed header size %d bytes, header of %d bytes, and blob of %d bytes\n",
		//		size,message.getSerializedSize(),blob.getSerializedSize());
		outwrite.writeInt(size);
		message.writeTo(outwrite); 		
		blob.writeTo(outwrite);

		// No longer store the blob, just the metadata.
		blob = null;
		// TODO: Track the locations of the blocks so that I can 
		// also store an inbox block at the end.
	}

	/** Reads or skips a fileblock. */
	static void process(DataInputStream input, Adaptor callback) throws IOException {
		int headersize = input.readInt();
		//System.out.format("Header size %d %x\n",headersize,headersize);
		byte buf[] = new byte[headersize];
		input.readFully(buf);
		//System.out.format("Read buffer for header of %d bytes\n",buf.length);
		Fileformat.FileBlockHeader header = Fileformat.FileBlockHeader.parseFrom(buf);

		//System.out.println(header);

		FileBlock fileblock = new FileBlock(header.getType(),null,header.getIndexdata());
		if (callback.skipBlock(fileblock)) {
			//System.out.format("Attempt to skip %d bytes\n",header.getDatasize());
			if (input.skip(header.getDatasize()) != header.getDatasize())
				assert false: "SHORT READ";
			return;
		} else {
			//System.out.format("Attempt to read fully %d (%x) bytes\n",header.getDatasize(),header.getDatasize());
			buf = new byte[header.getDatasize()];
			input.readFully(buf);
			Fileformat.Blob blob = Fileformat.Blob.parseFrom(buf);
			if (blob.hasRaw()) {
				fileblock.data = blob.getRaw();
			}
			else if (blob.hasZlibData()) {
				byte out[] = new byte[blob.getRawSize()];
				Inflater decompresser = new Inflater();
				 decompresser.setInput(blob.getZlibData().toByteArray());
				 //decompresser.getRemaining();
				 try {
					decompresser.inflate(out);
				} catch (DataFormatException e) {
					e.printStackTrace();
					throw new Error(e);
				}
				 assert (decompresser.finished());
				 decompresser.end();
				 fileblock.data = ByteString.copyFrom(out);
			}			 

			//return new FileBlock(header.getType(),blob,header.getIndexdata());
			callback.handleBlock(fileblock);
		}

	}

	public ByteString getData() {
		return data;
	}

	public String getType() {
		return type;
	}
}
