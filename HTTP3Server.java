import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.BlockingQueue;
import java.nio.file.*;
import java.lang.*;
import java.text.*;


public class HTTP3Server implements Runnable{

public static void main(String [] args){
   int portnumber;
   boolean listening = true;
   try{
	portnumber = Integer.parseInt(args[0]);
   }catch(Exception NumberFormatException){
	portnumber = -1;
   }

   if(portnumber == -1){
	System.out.println("Error: Please use format - java HTTP1Server <port>");
   }else{
	
	ExecutorService pool = new ThreadPoolExecutor(5, 50, 100, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
	

    try{
	ServerSocket serverSocket = new ServerSocket(portnumber);
	System.out.println("Server listening on port: " + portnumber); 

	while(listening){
		Socket clientSocket = serverSocket.accept();
		System.out.println("Connected");
		try{
			pool.execute(new clientHandler(clientSocket));
		}catch(RejectedExecutionException t){
		try{
		PrintWriter toClient = new PrintWriter(clientSocket.getOutputStream(), true);
		toClient.println("HTTP/1.0 503 Service Unavailable");
		}catch(IOException i){
		System.out.println("Invalid client input");		
		}
		}	
	}
   }catch(IOException e){
	System.out.println("Error: Could not accept connection");
   }
}}
public void run(){

}
}

