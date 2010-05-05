#make clean
make CXXFLAGS="-O0 -g"
gdb   --command=rundebug.gdb