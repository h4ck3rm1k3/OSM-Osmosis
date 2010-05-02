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

// // Iterates though all data in the osmfile and prints info about them.
// void ListPeople(const tutorial::AddressBook& address_book) {
//   for (int i = 0; i < address_book.person_size(); i++) {
//     const tutorial::Person& person = address_book.person(i);

//     cout << "Person ID: " << person.id() << endl;
//     cout << "  Name: " << person.name() << endl;
//     if (person.has_email()) {
//       cout << "  E-mail address: " << person.email() << endl;
//     }

//     for (int j = 0; j < person.phone_size(); j++) {
//       const tutorial::Person::PhoneNumber& phone_number = person.phone(j);

//       switch (phone_number.type()) {
//         case tutorial::Person::MOBILE:
//           cout << "  Mobile phone #: ";
//           break;
//         case tutorial::Person::HOME:
//           cout << "  Home phone #: ";
//           break;
//         case tutorial::Person::WORK:
//           cout << "  Work phone #: ";
//           break;
//       }
//       cout << phone_number.number() << endl;
//     }
//   }
// }

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

void outputBlob(Blob & Item)
{
      cerr << "OUTPUT Blob." << endl;
      cerr << "Byte Size " << Item.ByteSize() << endl;
      cerr << "Raw " << Item.has_raw() << endl;
      cerr << "Raw " << Item.raw() << endl;
      const char * p= Item.raw().c_str();
      while (*p)
	{
	  cerr << hex ;
	  cerr << setfill('0');
	  cerr << "RawByte: " << (int)*p << endl;
	  p++;
	}
      cerr << "RawSize " << Item.has_raw_size() << endl;
      cerr << "ZlibData " << Item.has_zlib_data() << endl;
      cerr << "LZMData " << Item.has_lzma_data() << endl;
      cerr << "BZip " << Item.has_bzip2_data() << endl;
}



void readFileBlockBlob(CodedInputStream *input)
{

  //google::protobuf::uint32 magic_number;
  //input->ReadLittleEndian32(&magic_number);
  //cerr << "magic_number " << magic_number << endl;

  Blob Item;
  //      FileBlockHeader Item;
  //  PrimitiveBlock Item;
  //  google::protobuf::uint32 magic_number;
  //  input->ReadLittleEndian32(&magic_number);
  //  cerr << "magic_number " << magic_number << endl;
      //           dumpObject(input);      
  if (!Item.ParseFromCodedStream(input)) 
    {
      cerr << "Failed to parse file with type Primitives" << endl;

    }
  else
    {
      cerr << " parse file OK Primitives." << endl;
              outputBlob(Item);

    }
}

// Blob Item; 


void dumpHeaderBlock(HeaderBlock & hdr)
{
  cerr << "BBOX"  << endl;
  cerr << hdr.bbox().left() << endl;
  cerr << hdr.bbox().right() << endl;
  cerr << hdr.bbox().top() << endl;
  cerr << hdr.bbox().bottom() << endl;
}

void readFileBlobHeaderBlock(ifstream & inputf, int datasize)
{
  Blob Item;
  HeaderBlock Contents;
  cerr << "at position:"   << hex << setfill('X')  << inputf.tellg() << endl;

  if (!inputf)
    {
      cerr << "before read blob file, fail"<< endl;
      return;
    }

  char * buffer = new char[datasize+1];
  inputf.read(buffer, datasize);
  if (!inputf)
    {
      cerr << "could not read buffer, fail"<< endl;
      return;
    }

  buffer[datasize]=0;// null terminated

  const char * p= buffer;
  while (*p)
    {
      cerr << hex ;
      cerr << setfill('0');
      cerr << "RawByte: " << (int)*p << endl;
      p++;
    }
  cerr << "at position:"  << hex << setfill('X') << inputf.tellg() << endl;
  CodedInputStream tempStream((const google::protobuf::uint8*)buffer, datasize);
  if (!Item.ParseFromCodedStream(&tempStream)) 
    {
      cerr << "Failed to parse file with Blob" << endl;    
    }
  else
    {
      cerr << " parse file OK Blob." << endl;
      outputBlob(Item);      
      const char * p= Item.raw().c_str();
      int s2 =Item.raw_size();
      int s = strlen(p);
      
      cerr << "got block" << p << " with length " << s <<  " and official length "<< s2 << endl;
      CodedInputStream tempStream((const google::protobuf::uint8*)p,s);
      //      dumpObject (&tempStream);
      if (Contents.ParseFromCodedStream(&tempStream)) 
	{
	  dumpHeaderBlock(Contents);
	}
      else
	{
	  cerr << "could not read header block" << endl;
	}
	  
    }
  delete [] buffer;
}

void outputHeader(FileBlockHeader & Item, ifstream & inputf)
{
  cerr << "type" << Item.type() << endl;
  cerr << "indexdata" <<  Item.indexdata() << endl;
  cerr << "datasize" << Item.datasize() << endl;

  readFileBlobHeaderBlock(inputf, Item.datasize());
}



void readFileHeader(const char * filename)
{
  FileBlockHeader Item;
 
  ifstream inputf(filename, ios::in | ios::binary);
  
  if (!inputf)
    {
      cerr << "cannot open file" << filename << endl;
      exit (-1);
    }
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
      
      if (inputf)
	{
	  const char * p= buffer;
	  while (*p)
	    {
	      cerr << hex ;
	      cerr << setfill('0');
	      cerr << "RawByte: " << (int)*p << endl;
	      p++;
	    }
	  
	  cerr << "at position:" << hex << setfill('X') << inputf.tellg() << endl;
	  
	  CodedInputStream tempStream((const google::protobuf::uint8*)buffer, magic_number);
	  if (!Item.ParseFromCodedStream(&tempStream)) 
	    {
	      cerr << "Failed to parse file with type Blob" << endl;
	    }
	  else
	    {
	      cerr << "Read Header" << endl;
	      outputHeader(Item, inputf);
	    }  
	  
	}
      delete [] buffer;
    }
  else
    {
      cerr << "Could not read buffer" << endl;
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

  readFileHeader(argv[1]);
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
