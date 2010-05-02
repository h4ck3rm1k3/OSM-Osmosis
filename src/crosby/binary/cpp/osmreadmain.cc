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
      cout << "<Blob>" << endl;
      cout << "Byte Size " << Item.ByteSize() << endl;
      cout << "Raw " << Item.has_raw() << endl;
      //      cout << "Raw " << Item.raw() << endl;
      dumpDataP(Item.raw().c_str());
      cout << "RawSize " << Item.has_raw_size() << endl;
      cout << "ZlibData " << Item.has_zlib_data() << endl;
      cout << "LZMData " << Item.has_lzma_data() << endl;
      cout << "BZip " << Item.has_bzip2_data() << endl;
      cout << "</Blob>" << endl;
}

template <> void dumpData(Info const&n) {
  cout << "<info>" << endl;
  cout << "version:" << n.version()    << endl;
  cout << "timestamp:" << n.timestamp()    << endl;
  cout << "changeset:" << n.changeset()    << endl; 
  cout << "uid:" << n.uid()    << endl; 
  cout << "user_sid:" << n.user_sid()    << endl; 
  cout << "</info>" << endl;
}

template <> void dumpData(DenseNodes const&n) {
  cout << "<densenodes>" << endl;
  for(int i=0; i <n.id_size(); i++)    {  
    cout << "ID:" << n.id(i)    << endl;
    cout << "lat:"  << n.lat(i)  <<endl;
    cout << "lon:" << n.lon(i)  <<endl;

    dumpData(n.info(i));
  }
  cout << "</densenodes>" << endl;
  //  for(int i=0; i <n.info_size(); i++)    {    }
  
}
template <> void dumpData(Node const& n){
  cout << "<node>" << endl;
  cout << "ID:" << n.id()    << endl;
  cout << "lat:"  << n.lat()  <<endl;
  cout << "lon:" << n.lon()  <<endl;
  dumpData(n.info());
  for(int i=0; i <n.keys_size(); i++)    {  
    cout << "key:" << n.keys(i)  <<endl;
    cout << "val:" << n.vals(i)  <<endl;
  }
  cout << "</node>" << endl;
}

template <> void dumpData(Way const& n){
  cout << "<way>" << endl;
  cout << "ID:" << n.id()    << endl;
  dumpData(n.info());
  for(int i=0; i <n.refs_size(); i++)    {  
    cout << "ref:" << n.refs(i)  ;
  }
  cout << "</way>" << endl;
}

template <> void dumpData(Relation const& n){
  cout << "<relation>" << endl;
  cout << "ID:" << n.id()    << endl;

  dumpData(n.info());
  for(int i=0; i <n.keys_size(); i++)    {  
    cout << "key:" << n.keys(i)  <<endl;
    cout << "val:" << n.vals(i)  <<endl;
  }

  for(int i=0; i <n.roles_sid_size(); i++)    {  
    cout << "role_sid:" << n.roles_sid(i)  <<endl;
    cout << "memberid:" << n.memids(i)  <<endl;
    cout << "type:" << n.types(i)  <<endl;
  }
  cout << "</relation>" << endl;
}

template <> void dumpData( BBox const & bbox)
{
  cout << "<bbox>" << endl;
  cout << bbox.left() << endl;
  cout << bbox.right() << endl;
  cout << bbox.top() << endl;
  cout << bbox.bottom() << endl;
  cout << "</bbox>" << endl;
}

template <> void dumpData(ChangeSet const& n){
  cout << "<changeset>" << endl;
  cout << "ID:" << n.id()    << endl;
  cout << "created_id:" << n.created_at()    << endl;
  cout << "closetime_delta:" << n.closetime_delta()    << endl;
  cout << "open:" << n.open()    << endl;
  dumpData(n.bbox());
  dumpData(n.info());
  for(int i=0; i <n.keys_size(); i++)    {  
    cout << "key:" << n.keys(i)  <<endl;
    cout << "val:" << n.vals(i)  <<endl;
  }
  cout << "</changeset>" << endl;
}

template <> void dumpData(StringTable const & str){
  cout << "String Table" << endl;
}

//template <> void dumpField(PrimitiveGroup const & str){}
template <> void dumpData(PrimitiveGroup const & n){
  cout << "<PrimitiveGroup>" << endl;
  dumpData(n.dense());

  for(int i=0; i <n.nodes_size(); i++)    {  dumpData(n.nodes(i));    }
  for(int i=0; i <n.ways_size(); i++)    {  dumpData(n.ways(i));    }
  for(int i=0; i <n.relations_size(); i++)    {  dumpData(n.relations(i));    }
  for(int i=0; i <n.changesets_size(); i++)    {  dumpData(n.changesets(i));    }
  cout << "</PrimitiveGroup>" << endl;

}
template <> void dumpData(FileBlockHeader & Item){
  cout << "<FileBlockHeader>" << endl;
  cout << "type" << Item.type() << endl;
  cout << "indexdata" <<  Item.indexdata() << endl;
  cout << "datasize" << Item.datasize() << endl;
  cout << "</FileBlockHeader>" << endl;
}


template <> void dumpData(HeaderBlock & hdr)
{
  cout << "<header>"  << endl;
  dumpData(hdr.bbox());
  cout << "</header>"  << endl;
}

template <> void dumpData(PrimitiveBlock & blk)
{
  cout << "<PrimitiveBlock>"  << endl;
  dumpData(blk.stringtable());;
  cout << blk.primitivegroup_size()<< endl;
  cout << blk.granularity()<< endl;
  cout << blk.date_granularity()<< endl;
  
  for (int i=0; i < blk.primitivegroup_size(); i++)
    {
      dumpData(blk.primitivegroup(i));
    }
  cout << "</PrimitiveBlock>"  << endl;
}

template <class TContents> int readHeaderBlock(FileBlockHeader & Item, ifstream & inputf)
{
  dumpData(Item);

  Blob ItemBlob;
  TContents Contents;
  int status=0;
  cout << "at position:"   << hex << setfill('X')  << inputf.tellg() << endl;

  if (!inputf)
    {
      cerr << "before read blob file, fail"<< endl;
      return status;
    }
  int datasize=Item.datasize();
  char * buffer = new char[datasize+1];
  inputf.read(buffer, datasize);
  if (!inputf)
    {
      cerr << "could not read buffer, fail"<< endl;
      delete [] buffer;
      return status;
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
      //      int s2 =ItemBlob.raw_size();
      int s = strlen(p);
      
      //cerr << "got block" << p << " with length " << s <<  " and official length "<< s2 << endl;
      CodedInputStream tempStream((const google::protobuf::uint8*)p,s);
      //      dumpObject (&tempStream);
      if (Contents.ParseFromCodedStream(&tempStream)) 
	{
	  dumpData(Contents);
	  status=1;
	}
      else
	{
	  cerr << "could not read header block" << endl;
	 
	}	  
    }
  delete [] buffer;
  return status;

}

template <class T> int readFileBlock(ifstream & inputf)
{
  FileBlockHeader Item; 
  int magic_number_=-1;
  cerr << "reading new block at position:"  << hex << setfill('X')  << inputf.tellg() << endl;
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
	      status = readHeaderBlock<T>(Item, inputf);
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

  if (readFileBlock<HeaderBlock>(inputf))
    {
      // now read some blocks
      while (readFileBlock<PrimitiveBlock>(inputf))
	{
	  cerr << "Okey!";
	}

    }

  // Optional:  Delete all global objects allocated by libprotobuf.
  google::protobuf::ShutdownProtobufLibrary();

  return 0;
}
