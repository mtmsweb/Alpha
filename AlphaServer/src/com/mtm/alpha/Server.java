package com.mtm.alpha;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class Server implements Runnable {
    private Thread mThread;
    private ServerSocket mSocket;
	private List<Players> mPlayers = new ArrayList<Players> (7);

	private static final int NETWORK_PORT = 8081;
	private static final char LINE_END_CHARACTER = 0x0A;
	
	public void initializeAll() {
		try {
			mSocket = new ServerSocket(NETWORK_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		mThread = new Thread(this);
		mThread.start();
		
		for (int i = 0; i < 6; i++) {
			mPlayers.add( new Players(false, "-", 0, 0) );
		}
	}

	@Override
	public void run() {
		System.out.println("Server started");
		while (true) {
			try {
				Socket connectionSocket = mSocket.accept();
				System.out.println("Client connected");
				
	            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream(), "UTF-8"));
	            OutputStream outToClient = connectionSocket.getOutputStream();
	            
	            String line;
		        List<String> request = new ArrayList<String>();
		        while ( ( line = inFromClient.readLine() ) != null ) {
		        	if (line.isEmpty() || line.equals(LINE_END_CHARACTER))
						break;
					request.add(line.trim());
		        }
		        
		        printRequest(request);
		        
		        String[] response = handleRequest(request);
		        printResponse(response);
		        for (String line_ : response) {
		        	//outToClient.writeUTF(line_);
		        	outToClient.write(line_.getBytes("UTF-8"));
		        	outToClient.write(LINE_END_CHARACTER);
		        }
	        	outToClient.write(LINE_END_CHARACTER);
	        			        
		        outToClient.close();
		        System.out.println("Socket closed \n");
	            
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void printRequest(List<String> request) {
		System.out.println("Request: ");
		for (String line : request) {
			System.out.println(line);
		}
	}
	private void printResponse(String[] response) {
		System.out.println("Response: ");
		for (String line : response) {
			System.out.println(line);
		}
	}
	
	private String[] handleRequest(List<String> request) {
		if (request == null || request.size() < 1)
			return new String[] {};
		
		if (request.get(0).equals("PING")) {
			return new String[] {
					"playerWidth=24",
					"antialiasing=true",
					"fps=30",
					"dimensionVertical=9",
					"playerHorizontalPosition=120",
					"tileHeight=60",
					"playerHorizontalPointsRatio=1.0",
					"tileWidth=60",
					"playerHeight=24",
					"playerStroke=4",
					"dimensionHorizontal=16"
			};
		} else if (request.get(0).equals("RANKING")) {
			if (request.size() != 4)
				return new String[] {"ERR:NO PARAMS"};
			
			try {
				Players player = new Players(true, request.get(1), Integer.valueOf(request.get(2)), Integer.valueOf(request.get(3)));
				mPlayers.add(player);
				
				Collections.sort(mPlayers, new Comparator<Players>() {
                    @Override
                    public int compare(Players p1, Players p2) {
                        return - p1.points + p2.points;
                    }
                });
			} catch (Exception e) {
				return new String[] {"ERR:PARAMS INVALID"};
			}
			
			String[] response = new String[6 * 5];
			for (int playerIndex = 0; playerIndex < 6; playerIndex ++) {
				final int index = playerIndex * 5;
        		Players player = mPlayers.get(playerIndex);
        		response[index + 0] = String.valueOf(player.me);
        		response[index + 1] = String.valueOf(playerIndex);
        		response[index + 2] = player.name;
        		response[index + 3] = String.valueOf(player.level);
        		response[index + 4] = String.valueOf(player.points);
        		player.me = false;
        	}
        	return response;
			
		} else if (request.get(0).equals("GETWORLD")) {
			if (request.size() != 2)
				return new String[] {"ERR:NO PARAMS"};

			try {
				int level = Integer.valueOf(request.get(1));
				File file = new File( "config_world_" + level + ".properties" );
				
				if (!file.exists()) {
					World world = WorldGenerator.generateRandomWorld(2, level);
					
					Properties properties = new Properties();
			    	try (OutputStream output = new FileOutputStream(file)) {
			    		byte[] worldBytes = WorldGenerator.worldToBytes(world);
			    		properties.setProperty(Settings.KEY_WORLD, new String(worldBytes, StandardCharsets.UTF_8));
			    		properties.setProperty(Settings.KEY_WORLD_WIDTH, String.valueOf(world.worldWidth));
			    		properties.setProperty(Settings.KEY_WORLD_POINTS_RATIO, String.valueOf(world.pointsRatio));
			    		properties.setProperty(Settings.KEY_WORLD_STEP_MOVEMENT, String.valueOf(world.stepMovement));
			    		properties.store(output, null);
			    	} catch (IOException io) {
			    		io.printStackTrace();
			    	}
				}
		    	
		    	List<String> responseWorld = new ArrayList<String> (2);
		    	try (BufferedReader br = new BufferedReader(new FileReader(file))) {
		    	    String line;
		    	    while ((line = br.readLine()) != null) {
		    	    	responseWorld.add(line);
		    	    }
		    	}

		    	String[] response = new String[responseWorld.size()];
		    	response = responseWorld.toArray(response);
		    	return response;

			} catch (Exception e) {
				return new String[] {"ERR:PARAMS INVALID"};
			}
		}
		
		return new String[] {};
	}
}
