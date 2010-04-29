#include <iostream>
#include <fstream>
#include <string>
#include "osmformat.pb.h"
#include "fileformat.pb.h"

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

template <class T> void readFile(const char * filename, const char * name)
{
  // Read the existing address book.
  T Item;
  fstream input(filename, ios::in | ios::binary);
  if (!Item.ParseFromIstream(&input)) {
    cerr << "Failed to parse file with type " << name << endl;
    //    exit (-1);
  }
  else
    {
      cerr << " parse file OK." << endl;
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
  //  TRY(google::protobuf::Message)
TRY(Blob)
TRY(PrimitiveBlock)
TRY(FileDirectory)
TRY(FileHeader)
TRY(FileBlockHeader)
TRY(BBox)
TRY(StringTable)
TRY(Info)
TRY(ChangeSet)
TRY(Node)
TRY(DenseNodes)
TRY(Way)
TRY(Relation)
TRY(PrimitiveGroup)
TRY(PrimitiveBlock)
TRY(HeaderBlock)

  // Optional:  Delete all global objects allocated by libprotobuf.
  google::protobuf::ShutdownProtobufLibrary();

  return 0;
}
