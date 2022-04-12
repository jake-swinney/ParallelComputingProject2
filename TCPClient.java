import java.io.*;
import java.net.*;
import java.nio.file.StandardOpenOption;

public class TCPClient
{
    public static void main(String[] args) throws IOException
    {
        if (args.length == 0)
        {
            System.out.println("One argument required: fileName");
            System.exit(1);
        }

        String fileName = args[0];
        File f = new File(fileName);
        if (!f.exists() || f.isDirectory())
        {
            System.out.println("File could not be opened");
            System.exit(1);
        }

        // Variables for setting up connection and communication
        Socket Socket = null; // socket to connect with ServerRouter
        PrintWriter out = null; // for writing to ServerRouter
        BufferedReader in = null; // for reading form ServerRouter
        InetAddress addr = InetAddress.getLocalHost();
        String host = addr.getHostAddress(); // Client machine's IP
        String routerName = "192.168.0.100"; // ServerRouter host name - Raspberry Pi
        int SockNum = 5555; // port number

        // Tries to connect to the ServerRouter
        try
        {
            Socket = new Socket(routerName, SockNum);
            out = new PrintWriter(Socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(Socket.getInputStream()));
        }
        catch (UnknownHostException e)
        {
            System.err.println("Don't know about router: " + routerName);
            System.exit(1);
        }
        catch (IOException e)
        {
            System.err.println("Couldn't get I/O for the connection to: " + routerName);
            System.exit(1);
        }

        // Variables for message passing
        String fromServer; // messages received from ServerRouter
        String fromUser; // messages sent to ServerRouter
        String address = "192.168.0.101"; // destination IP (Server) - laptop

        // Communication process (initial sends/receives)
        //out.println(address); // initial send (IP of the destination Server)
        out.println("5554");
        String rcvdAddr = in.readLine();
        int rcvdPort = Integer.parseInt(in.readLine());
        Socket.close();

        Socket outSocket = new Socket(rcvdAddr, rcvdPort);
        out = new PrintWriter(outSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(outSocket.getInputStream()));

        String rtTime = "";
        fromServer = in.readLine(); // receive RTTime
        if (fromServer.startsWith("!RTTime"))
        {
            rtTime = fromServer.substring(9);
        }

        out.println(host); // Client sends the IP of its machine as initial send

        if (fileName.endsWith(".txt"))
        {
            long fileSize = f.length();

            Reader reader = new FileReader(fileName);
            BufferedReader fromFile =  new BufferedReader(reader); // reader for the string file
            long t0, t1, t;

            t = 0;
            t0 = System.currentTimeMillis();

            int charCount = 0;
            int numMsg = 0;
            long startTime = System.currentTimeMillis();

            // Communication while loop
            while ((fromServer = in.readLine()) != null)
            {
                System.out.println("Server: " + fromServer);
                t1 = System.currentTimeMillis();
                if (fromServer.equals("Bye.")) // exit statement
                    break;
                else if(fromServer.startsWith("!RTTime"))
                {
                    rtTime = fromServer.substring(9);
                }

                t = t1 - t0;
                System.out.println("Cycle time: " + t);

                fromUser = fromFile.readLine(); // reading strings from a file
                if (fromUser != null)
                {
                    numMsg++;
                    charCount += fromUser.length();
                    System.out.println("Client: " + fromUser);
                    out.println(fromUser); // sending the strings to the Server via ServerRouter
                    t0 = System.currentTimeMillis();
                }
                else
                {
                    System.out.println("File read finished. Sending 'Bye.'.");
                    out.println("Bye.");
                    fromFile.close();
                    break;
                }
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            int avgMsgSize = charCount / numMsg;
            long avgMsgTime = totalTime / numMsg;

            //Write stuff to CSV file
            File csv = new File("data.csv");
            csv.createNewFile();

            BufferedWriter outCsv = new BufferedWriter(new FileWriter(csv, true));
            outCsv.write(fileName + "," + avgMsgSize + "," + avgMsgTime + "," + rtTime + "," + fileSize + "\n");
            outCsv.close();
        }
        else
        {
            // Receive first message (server's host) from server
            while ((fromServer = in.readLine()) != null)
            {
                System.out.println("Server: " + fromServer);
                break;
            }

            // Send the server the name of the file
            fromUser = "!FILENAME:" + fileName;
            System.out.println("Client: " + fromUser);
            out.println(fromUser);

            // Read the file as bytes
            byte[] data = new byte[(int) f.length()];
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
            bis.read(data, 0, data.length);
            bis.close();

            // Send the server the size of the file
            fromUser = "!BYTES:" + data.length;
            System.out.println(fromUser);
            out.println(fromUser);
            OutputStream os = Socket.getOutputStream();
            os.write(data, 0, data.length);
            os.flush();

            System.out.println("Sent " + data.length + " bytes.");
            long t0 = System.currentTimeMillis();

            while ((fromServer = in.readLine()) != null)
            {
                System.out.println("Server: " + fromServer);
                if (fromServer.equals("Bye."))
                    break;
            }

            long t1 = System.currentTimeMillis();
            long t = t1 - t0;

            System.out.println("RTT: " + t);

            //Write stuff to CSV file
            File csv = new File("data.csv");
            csv.createNewFile();

            BufferedWriter outCsv = new BufferedWriter(new FileWriter(csv, true));
            outCsv.write(fileName + "," + data.length + "," + t + "," + rtTime + "\n");
            outCsv.close();
        }

        System.out.println("Closing connection.");

        // closing connections
        outSocket.close();
        out.close();
        in.close();
        Socket.close();
    }
}
