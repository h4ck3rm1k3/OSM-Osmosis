#include <iostream>
#include <fstream>
#include <string>
#include "osmformat.pb.h"
#include "fileformat.pb.h"
#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>
#include <google/protobuf/wire_format_lite_inl.h>
#include <google/protobuf/repeated_field.h>
#include <iomanip>
#include <netinet/in.h>

using namespace google::protobuf::io;
using namespace std;

template <class T> void dumpData(T & item);

void dumpDataP(const char * p)
{
  return ; // no debug
  while (*p)
    {
      cerr << hex ;
      cerr << setfill('0');
      cerr << "RawByte: " << (int)*p << endl;
      p++;
    }

}

// generic dumper... only use for debugging, because it reads ahead
void dumpObject (CodedInputStream *input)
{
  ::google::protobuf::uint32 tag;
  while ((tag = input->ReadTag()) != 0) {
    int number = ::google::protobuf::internal::WireFormatLite::GetTagFieldNumber(tag);
    int type = ::google::protobuf::internal::WireFormatLite::GetTagWireType(tag);
    cerr << "tag:" << tag  << endl;
    cerr << "number:" << number  << endl;
    cerr << "type:" << type  << endl;
  }
  cerr << "last tag:" << tag  << endl;

}

// generic stream dumper, 
void dumpsteam(const char * filename)
{
  fstream inputf(filename, ios::in | ios::binary);

  //  ::google::protobuf::io::CodedInputStream input;
  //int fd = open("myfile", O_RDONLY);
  ZeroCopyInputStream* raw_input = new  IstreamInputStream(& inputf);

  CodedInputStream * input = new CodedInputStream(raw_input);

  //Skip(int count)
    google::protobuf::uint32 magic_number;
    input->ReadLittleEndian32(&magic_number);
    cerr << "magic_number " << magic_number << endl;
    dumpObject(input);

  delete input;
  delete raw_input;
}

template <class T> void readFile(const char * filename, const char * name)
{
  // Read the existing address book.
  T Item;

  fstream inputf(filename, ios::in | ios::binary);
  ZeroCopyInputStream* raw_input = new  IstreamInputStream(& inputf);

  CodedInputStream * input = new CodedInputStream(raw_input);

  //Skip(int count)
    google::protobuf::uint32 magic_number;
    input->ReadLittleEndian32(&magic_number);
    cerr << "magic_number " << magic_number << endl;

  if (!Item.ParseFromCodedStream(input)) {
    cerr << "Failed to parse file with type " << name << endl;
    //    dumpsteam(filename);
    // exit (-1);
  }
  else
    {
      cerr << " parse file OK."  << name << endl;
    }
}


template <> void dumpData(Blob & Item)
{
      cerr << "OUTPUT Blob." << endl;
      cerr << "Byte Size " << Item.ByteSize() << endl;
      cerr << "Raw " << Item.has_raw() << endl;
      //      cerr << "Raw " << Item.raw() << endl;
      dumpDataP(Item.raw().c_str());
      cerr << "RawSize " << Item.has_raw_size() << endl;
      cerr << "ZlibData " << Item.has_zlib_data() << endl;
      cerr << "LZMData " << Item.has_lzma_data() << endl;
      cerr << "BZip " << Item.has_bzip2_data() << endl;
}


template <> void dumpData(StringTable const & str){
  cerr << "String Table" << endl;
}
template <> void dumpData(PrimitiveGroup const & str){
  cerr << "PrimitiveGroup" << endl;
}
template <> void dumpData(FileBlockHeader & Item){
  cerr << "FileBlockHeader" << endl;
  cerr << "type" << Item.type() << endl;
  cerr << "indexdata" <<  Item.indexdata() << endl;
  cerr << "datasize" << Item.datasize() << endl;

}


template <> void dumpData(HeaderBlock & hdr)
{
  cerr << "BBOX"  << endl;
  cerr << hdr.bbox().left() << endl;
  cerr << hdr.bbox().right() << endl;
  cerr << hdr.bbox().top() << endl;
  cerr << hdr.bbox().bottom() << endl;
}

template <> void dumpData(PrimitiveBlock & blk)
{
  cerr << "Primitive Block"  << endl;
  dumpData(blk.stringtable());;
  cerr << blk.primitivegroup_size()<< endl;
  cerr << blk.granularity()<< endl;
  cerr << blk.date_granularity()<< endl;
  
  for (int i=0; i < blk.primitivegroup_size(); i++)
    {
      dumpData(blk.primitivegroup(i));
    }

}

template <class TContents> void readHeaderBlock(FileBlockHeader & Item, ifstream & inputf)
{
  dumpData(Item);
  //  readFileBlobHeaderBlock(inputf, Item.datasize());
  Blob ItemBlob;
  TContents Contents;
  cerr << "at position:"   << hex << setfill('X')  << inputf.tellg() << endl;

  if (!inputf)
    {
      cerr << "before read blob file, fail"<< endl;
      return;
    }
  int datasize=Item.datasize();
  char * buffer = new char[datasize+1];
  inputf.read(buffer, datasize);
  if (!inputf)
    {
      cerr << "could not read buffer, fail"<< endl;
      return;
    }

  buffer[datasize]=0;// null terminated

  dumpDataP(buffer);

  cerr << "at position:"  << hex << setfill('X') << inputf.tellg() << endl;
  CodedInputStream tempStream((const google::protobuf::uint8*)buffer, datasize);
  if (!ItemBlob.ParseFromCodedStream(&tempStream)) 
    {
      cerr << "Failed to parse file with Blob" << endl;    
    }
  else
    {
      cerr << " parse file OK Blob." << endl;
      dumpData(ItemBlob);      
      const char * p= ItemBlob.raw().c_str();
      int s2 =ItemBlob.raw_size();
      int s = strlen(p);
      
      //cerr << "got block" << p << " with length " << s <<  " and official length "<< s2 << endl;
      CodedInputStream tempStream((const google::protobuf::uint8*)p,s);
      //      dumpObject (&tempStream);
      if (Contents.ParseFromCodedStream(&tempStream)) 
	{
	  dumpData(Contents);
	}
      else
	{
	  cerr << "could not read header block" << endl;
	}	  
    }
  delete [] buffer;

}

template <class T> int readFileBlock(ifstream & inputf)
{
  FileBlockHeader Item; 
  int magic_number_=-1;
  cerr << "at position:"  << hex << setfill('X')  << inputf.tellg() << endl;
  inputf.read(reinterpret_cast < char * > (&magic_number_),4);
  uint32_t  magic_number = ntohl(magic_number_);

  if (inputf)
    {
      cerr << "magic_number " << magic_number << endl;
      cerr << "at position:" << hex << setfill('X') << inputf.tellg() << endl;
      
      char * buffer = new char[magic_number+1];
      inputf.read(buffer, magic_number);
      buffer[magic_number]=0;// null terminated
      int status =0;
      if (inputf)
	{
	  const char * p= buffer;
	  dumpDataP(p);
  
	  cerr << "at position:" << hex << setfill('X') << inputf.tellg() << endl;
	  
	  CodedInputStream tempStream((const google::protobuf::uint8*)buffer, magic_number);
	  if (!Item.ParseFromCodedStream(&tempStream)) 
	    {
	      cerr << "Failed to parse file with type Blob" << endl;
	    }
	  else
	    {
	      cerr << "Read Header" << endl;
	      readHeaderBlock<T>(Item, inputf);
	      status =1;
	    }	  
	}
      delete [] buffer;
      return status;
    }
  else
    {
      cerr << "Could not read buffer" << endl;
      return 0;
    }
}


#define TRY(X) readFile<X >(argv[1], # X  "");
int main(int argc, char* argv[]) 
{

  GOOGLE_PROTOBUF_VERIFY_VERSION; // version check

  if (argc != 2) {
    cerr << "Usage:  " << argv[0] << " OSM_FILE" << endl;
    return -1;
  }

  ifstream inputf(argv[1], ios::in | ios::binary);
  
  if (!inputf)
    {
      cerr << "cannot open file" << argv[1] << endl;
      exit (-1);
    }

  if (readFileBlock<FileBlockHeader>(inputf))
    {
      // now read some blocks
      while (readFileBlock<PrimitiveBlock>(inputf))
	{
	  cerr << "Okey!";
	}

    }
  
  //  TRY(google::protobuf::Message)
  //  TRY(HeaderBlock)
  //    TRY(FileHeader)
  //    TRY(FileBlockHeader)
  // TRY(Blob)
  // TRY(PrimitiveBlock)
  // TRY(FileDirectory)
// TRY(BBox)
// TRY(StringTable)
// TRY(Info)
// TRY(ChangeSet)
// TRY(Node)
// TRY(DenseNodes)
// TRY(Way)
// TRY(Relation)
// TRY(PrimitiveGroup)
// TRY(PrimitiveBlock)


  // Optional:  Delete all global objects allocated by libprotobuf.
  google::protobuf::ShutdownProtobufLibrary();

  return 0;
}
