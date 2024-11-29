# Introduction
- In this assignment, i implement the code based on the wiki of bittorent.
- I use DataOutputStream to writeUTF so the protocol might be different as i didn't encode something such as Message of handshake to send. So if you want to improve the source, you can implement encode and then using OutputStream to send byte of encode string.
- - So there are 4 main files: MyClient.java, MyServer.java, Bencode.java and Metainfo.java
  - To run these files, just need to
  '''
javac *.java
java MyClient.java
java MyServer.java
 
 '''
 
