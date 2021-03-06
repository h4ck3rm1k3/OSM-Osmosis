package crosby.binary.file;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.google.protobuf.ByteString;

import crosby.binary.Fileformat;

/**
 * Intermediate representation of the header of a fileblock when a set of
 * fileblocks is read as in a stream. The data in the fileblock must be either
 * skipped (where the returned value is a reference to the fileblock) or parsed.
 * 
 * @author crosby
 * 
 */
public class FileBlockHead extends FileBlockReference {
    protected FileBlockHead(String type, ByteString indexdata) {
        super(type, indexdata);
    }

    /**
     * Read the header. After reading the header, either the contents must be
     * skipped or read
     */
    static FileBlockHead readHead(InputStream input) throws IOException {
        DataInputStream datinput = new DataInputStream(input);
        int headersize = datinput.readInt();
        System.out.format("Header size %d %x\n",headersize,headersize);
        byte buf[] = new byte[headersize];
        datinput.readFully(buf);
         System.out.format("Read buffer for header of %d bytes\n",buf.length);
        Fileformat.FileBlockHeader header = Fileformat.FileBlockHeader
                .parseFrom(buf);
        FileBlockHead fileblock = new FileBlockHead(header.getType(), header
                .getIndexdata());

        fileblock.datasize = header.getDatasize();
        // data_offset =
        fileblock.input = input;
        if (input instanceof FileInputStream)
            fileblock.data_offset = ((FileInputStream) input).getChannel()
                    .position();

        return fileblock;
    }

    /**
     * Assumes the stream is positioned over at the start of the data, skip over
     * it.
     * 
     * @throws IOException
     */
    void skipContents(InputStream input) throws IOException {
        if (input.skip(getDatasize()) != getDatasize())
            assert false : "SHORT READ";
    }

    /**
     * Assumes the stream is positioned over at the start of the data, read it
     * and return the complete FileBlock
     * 
     * @throws IOException
     */
    FileBlock readContents(InputStream input) throws IOException {
        DataInputStream datinput = new DataInputStream(input);
        byte buf[] = new byte[getDatasize()];
        datinput.readFully(buf);
        return parseData(buf);
    }
}
