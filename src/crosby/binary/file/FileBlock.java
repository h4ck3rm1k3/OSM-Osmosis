package crosby.binary.file;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.Deflater;

import com.google.protobuf.ByteString;

import crosby.binary.Fileformat;

/** A full fileblock object contains both the metadata and data of a fileblock */
public class FileBlock extends FileBlockBase {
    /** Contains the contents of a block for use or further processing */
    ByteString data; // serialized Format.Blob

    private FileBlock(String type, ByteString blob, ByteString indexdata) {
        super(type, indexdata);
        this.data = blob;
    }

    public static FileBlock newInstance(String type, ByteString blob,
            ByteString indexdata) {
        return new FileBlock(type, blob, indexdata);
    }

    public static FileBlock newInstance(String type, ByteString indexdata) {
        return new FileBlock(type, null, indexdata);
    }

    protected void deflateInto(crosby.binary.Fileformat.Blob.Builder blobbuilder) {
        int size = data.size();
        Deflater deflater = new Deflater();
        deflater.setInput(data.toByteArray());
        deflater.finish();
        byte out[] = new byte[size];
        deflater.deflate(out);

        if (!deflater.finished()) {
            // Buffer wasn't long enough. Be noisy.
            System.out
                    .println("Compressed buffer too short causing extra copy");
            out = Arrays.copyOf(out, size + size / 64 + 16);
            deflater.deflate(out, deflater.getTotalOut(), out.length
                    - deflater.getTotalOut());
            assert (deflater.finished());
        }
        ByteString compressed = ByteString.copyFrom(out, 0, deflater
                .getTotalOut());
        blobbuilder.setZlibData(compressed);
        deflater.end();
    }

    public FileBlockPosition writeTo(OutputStream outwrite, CompressFlags flags)
            throws IOException {
        Fileformat.FileBlockHeader.Builder builder = Fileformat.FileBlockHeader
                .newBuilder();
        if (indexdata != null)
            builder.setIndexdata(indexdata);
        builder.setType(type);

        Fileformat.Blob.Builder blobbuilder = Fileformat.Blob.newBuilder();
        if (flags == CompressFlags.NONE) {
            blobbuilder.setRaw(data); // set the data in the blog
        } else {
            blobbuilder.setRawSize(data.size());
            if (flags == CompressFlags.DEFLATE)
                deflateInto(blobbuilder);
            else
                assert false : "TODO"; // TODO
        }
        Fileformat.Blob blob = blobbuilder.build(); // create the blob of data

        builder.setDatasize(blob.getSerializedSize()); // put the blob in the header
        Fileformat.FileBlockHeader message = builder.build(); // create a message from the header
        int size = message.getSerializedSize(); // get the size of the header (13 for this examples)

	//Outputed header size 13 bytes, header of 13 bytes, and blob of 32 bytes
        System.out.format("Outputed header size %d bytes, header of %d bytes, and blob of %d bytes\n",
			  size,message.getSerializedSize(),blob.getSerializedSize());

	// write the size to the stream (4 bytes)
        (new DataOutputStream(outwrite)).writeInt(size);

	// write the FileBlockHeader to the file
        message.writeTo(outwrite);
        long offset = -1;

        if (outwrite instanceof FileOutputStream)
            offset = ((FileOutputStream) outwrite).getChannel().position();

	// write the blob to the file
        blob.writeTo(outwrite);
        return FileBlockPosition.newInstance(this, offset, size);
    }

    /** Reads or skips a fileblock. */
    static void process(InputStream input, BlockReaderAdapter callback)
            throws IOException {
        FileBlockHead fileblock = FileBlockHead.readHead(input);
        if (callback.skipBlock(fileblock)) {
             System.out.format("Attempt to skip %d bytes\n",fileblock.getDatasize());
            fileblock.skipContents(input);
        } else {
            callback.handleBlock(fileblock.readContents(input));
        }
    }

    public ByteString getData() {
        return data;
    }
}
