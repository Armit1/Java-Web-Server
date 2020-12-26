import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class clientHandler implements Runnable{
private byte[] outBytes;
private String command;
private Socket clientSocket; 
private String cookie;


public clientHandler(Socket s){
 clientSocket = s;
}

//populates the response header

	private String populateCookieHeader(int length) { 
		
		
		
		String header = "";
		LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj); 
        String encodedDateTime = "";
        try {
         encodedDateTime = URLEncoder.encode(formattedDate, "UTF-8");
        }
        catch(UnsupportedEncodingException e){ 
        	
        } 
        
       
		header += "Content-Type: text/html\r\n" ;
        header+= "Set-Cookie: lasttime="+ encodedDateTime + "\r\n";
		header += "Content-Encoding: identity" + "\r\n";
		header += "Allow: GET, POST, HEAD" + "\r\n";
		header += "Expires: Wed, 21 Oct 2021 07:28:00 GMT\r\n";
		header += "Content-Length: " + length + "\r\n\r\n";

		return header;
		
	} 
	
	
//populates the response payload if the cookie has already been set
	
	private String populateFile() { 
		
		
		String htmlString = "<html>"+
				"<body>"+
				"<h1>CS 352 Welcome Page </h1>"+
				"<p>"+
				"  Welcome back! Your last visit was at: %YEAR-%MONTH-%DAY %HOUR-%MINUTE-%SECOND"+
				"<p>"+
				"</body>"+
				"</html>";
		LocalDateTime myDateObj = LocalDateTime.now();
		DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj); 
        htmlString = htmlString.replace("%YEAR-%MONTH-%DAY %HOUR-%MINUTE-%SECOND", formattedDate);
      
		return htmlString;
	}
    
    private int getCommand(String cmd) { 
    	
		if (cmd.equals("GET") || cmd.equals("POST") || cmd.equals("HEAD")){
			return 1;
		}
		else if (cmd.equals("PUT") || cmd.equals("DELETE") || cmd.equals("LINK") || cmd.equals("UNLINK")){
			return 2;
		}else{
			return 3;
		}
	}

    private float parseVersion(String s){ 
    	
       float version;
		try{ 
			
		 version = Float.parseFloat(s.substring(5));
		 return version; 
		 
		} catch(NumberFormatException e){ 
			
		 return Float.parseFloat("1.1");
		}
    } 
    
    
    //build the entire server response

    private String buildResponse(String clientInput) { 
    	
        if (clientInput == null) {
        	
            return "HTTP/1.0 400 Bad Request";
            
            
        }

        String[] tokens = clientInput.split("\\s+");

        float version;

        if(tokens.length != 3 || !tokens[0].toUpperCase().equals(tokens[0]) || tokens[1].charAt(0) != '/' || !tokens[2].substring(0,5).equals("HTTP/") || tokens[2].substring(5) == null) {
        	 
        
            return "HTTP/1.0 400 Bad Request";
        }
        try {
            version = Float.parseFloat(tokens[2].substring(5));
        } catch (NumberFormatException num) { 
        	
            return "HTTP/1.0 400 Bad Request";
        }

        
        if (version > 1.0 || version < 0.0)
            return "HTTP/1.0 505 HTTP Version Not Supported";

        //GET, POST or HEAD
        if(getCommand(tokens[0]) == 2)
            return "HTTP/1.0 501 Not Implemented";
        else if (getCommand(tokens[0]) == 3) {
        	
            return "HTTP/1.0 400 Bad Request";
        } 
        
        String fpath = "." + tokens[1];

        

        try { 
        	
        	if(fpath.equals("./")) { 
        		
        		String response = "";
        		
        		if(cookie !=null) {
        			outBytes = populateFile().getBytes();
        			response = "HTTP/1.0 200 OK" + '\r' + '\n' + populateCookieHeader(outBytes.length);
        			
        			
        		}
        		else { 
        			File f = new File("./index.html");
        			Path path = Paths.get("./index.html");
        			int length = Math.toIntExact(f.length());
        			outBytes = Files.readAllBytes(path); 
        			
        			response = "HTTP/1.0 200 OK" + '\r' + '\n' + populateCookieHeader(length);
        			
        		}
        		
        		return response;
        	}

            

        } catch (AccessDeniedException e) {
            return "HTTP/1.0 403 Forbidden";
        } catch (IOException io) {
            return "HTTP/1.0 500 Internal Server Error";
        }
        
        return "error";

    }



    public void run() { 
    	
        try { 
        	
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
            command = "";

            try { 
            	
                clientSocket.setSoTimeout(3000);
                command = inFromClient.readLine();
                String line = "";
                while (inFromClient.ready()) {
                    line = inFromClient.readLine();
                    
                    if(line.length() > 0 && line.contains(":")) { 
                    	
                    	int i = line.indexOf(":");
                		String prop = line.substring(0,i);
                		String val = line.substring(i+2); 
                		
                		if(prop.equals("Cookie")) { 
                			
                			cookie = URLDecoder.decode(val, "UTF-8");
          
                		}
                    	
                    }
                }
               
                String status = buildResponse(command);
                System.out.println("Sending response: " + status);
                byte[] bytes = status.getBytes();
                outToClient.write(bytes);
                outToClient.flush();

                if(status.contains("200 OK") && (command.contains("GET") || command.contains("POST")))
                    outToClient.write(outBytes);


            } catch (SocketTimeoutException e){
                byte[] bytes = "HTTP/1.0 408 Request Timeout".getBytes();
                outToClient.write(bytes);
            }
            inFromClient.close();
            outToClient.close();
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error handling client input.");
        }
    }



}



