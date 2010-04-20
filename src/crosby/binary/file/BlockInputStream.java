package crosby.binary.file;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.protobuf.ByteString;

import crosby.binary.Fileformat;

public class BlockInputStream {		
	// TODO: Should be seekable input stream!
	public BlockInputStream(InputStream input, FileBlock.Adaptor adaptor) {
		this.input = new DataInputStream(input);
		this.adaptor = adaptor;
	}

	public void process() throws IOException {
		while (input.available() > 0) {
			FileBlock.process(input,adaptor);
		}
		adaptor.complete();
	}


	public void close() throws IOException {
		input.close();
	}

	DataInputStream input;
	FileBlock.Adaptor adaptor;
}
