package crosby.binary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import crosby.binary.file.BlockOutputStream;
import crosby.binary.file.FileBlock;

/** Generic serializer common code
 * 
 *  Serialize a set of blobs and process them. Subclasses implement handlers for different API's 
 *  (osmosis, mkgmap, splitter, etc.)
 *  
 *  All data is converted into PrimGroupWriterInterface objects, which are then ordered to
 *  process their data at the appropriate time.
 *  */


public class BinarySerializer {

	/** Interface used to write a gropu of primitives. One of these for each group */
	protected interface PrimGroupWriterInterface {
		/** Add all of the strings in this primitive group to the stringtable */
		public void addStringsToStringtable();
		/** Store the data in this primitive group into the given primitive block */
		public void serialize(Osmformat.PrimitiveBlock.Builder group);
	}

	public void configGranularity(int granularity) {
		this.granularity = granularity;
	}

	public void configOmit(boolean omit_metadata) {
			this.omit_metadata = omit_metadata;
	}

	public void configBatchLimit(int batch_limit) {
		this.batch_limit = batch_limit;
	}
	
	// Paramaters affecting the output size.
	protected final int MIN_DENSE = 10;
	protected int batch_limit = 4000;

	// Parmaters affecting the output.

	protected int granularity=100;
	protected int date_granularity=1000;
	protected boolean omit_metadata = false;

	/** How many primitives have been seen in this batch */
	protected int batch_size=0;
	protected int total_entities=0;
	private StringTable stringtable = new StringTable();
	protected List <PrimGroupWriterInterface> groups = new ArrayList<PrimGroupWriterInterface>();
	protected BlockOutputStream output;

	public BinarySerializer(BlockOutputStream output) {
		this.output = output;
	}

	public StringTable getStringTable() {
		return stringtable;
	}

	public void flush() throws IOException {
		processBatch();
		output.flush();
	}

	public void close() throws IOException {
		flush();
		output.close();
	}
	
	long debug_bytes = 0;
	
	public void processBatch() {
		//System.out.format("Batch of %d groups: ",groups.size());
		if (groups.size() == 0)
			return;
		Osmformat.PrimitiveBlock.Builder primblock = Osmformat.PrimitiveBlock.newBuilder();
		stringtable.clear();
		// Preprocessing: Figure out the stringtable.
		for (PrimGroupWriterInterface i : groups)
			i.addStringsToStringtable();

		stringtable.finish();
		// Now, start serializing.
		for (PrimGroupWriterInterface i : groups) {
			i.serialize(primblock);
		}
		primblock.setStringtable(stringtable.serialize());
		primblock.setGranularity(this.granularity);
		primblock.setDateGranularity(this.date_granularity);

		Osmformat.PrimitiveBlock message=primblock.build();
		
		
		//System.out.println(message);
		debug_bytes += message.getSerializedSize();
		System.out.format("    =======>  %.2f / %.2f   (%dk)\n",
				message.getSerializedSize()/1024.0, 
				debug_bytes/1024/1024.0,
				total_entities/1000);
		//if (message.getSerializedSize() > 1000000)
		//System.out.println(message);

		
		try {
			output.write(FileBlock.newInstance("OSMData",message.toByteString(),null));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Error(e);
		} finally {
			batch_size = 0;
			groups.clear();
		}
		//System.out.format("\n");
	}
	
	public long mapRawDegrees(double degrees) {
		return (long)((degrees/.000000001));
	}
	public int mapDegrees(double degrees) {
		return (int)((degrees/.0000001)/(granularity/100));
	}
}
