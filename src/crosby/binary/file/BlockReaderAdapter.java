package crosby.binary.file;

/** An adaptor that receives blocks from an input stream */
public interface BlockReaderAdapter {
	/** Does the reader understand this block? Does it want the data in it? 
	 * 
	 * A reference contains the metadata about a block and can saved --- or stored ---- for 
	 * future random access. However, at this point, the question is whether during a stream
	 * read of the file, does the user want this block?
	 * 
	 * handleBlock will be called on all blocks that are not skipped, in file order.  
	 * 
	 * */
	boolean skipBlock(FileBlockReference message);
	/** Called with the data in the block */
	void handleBlock(FileBlock message);

	/** Called when the file is fully read. */
	void complete();
}
