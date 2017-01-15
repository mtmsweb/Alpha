package com.mtm.alpha.game;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NetworkManager implements Runnable {
    private Thread mThread;
    private NetworkListener mListener;

	private static final String NETWORK_SERVER = "localhost";
	private static final int NETWORK_PORT = 8081;
	private static final int TIMEOUT = 2000; // ms
	public static final char LINE_END_CHARACTER = 0x0A;
    public static enum REQUESTS {PING, RANKING, GETWORLD};
	
    private REQUESTS mRequest;
    private String[] mRequestParameters;
    
    /**
     * Komunikacja sieciowa realizowana jest przy użyciu TCP / IP na porcie 8080 (stala NETWORK_PORT).
     * Po nawiązaniu połączenia od klienta do Serwera wysyłana jest Komenda zakończona znakiem 0x0A (stala LINE_END_CHARACTER). 
     * Po niej wysyłane są linie z opcjonalnymi parametrami zależnymi od rodzaju Komendy. 
     * Pusta linia oznacza koniec parametrow.
     * Odpowiedź serwera to ciąg linii, kazda zakonczona znakiem z LINE_END_CHARACTER. 
     * Po przesłaniu wszystkich linii, serwer kończy połączenie.
     */
    
	public static interface NetworkListener {
        boolean networkResponse(REQUESTS request, List<String> response);
    }
	
	public static void initRequestTask(NetworkListener listener, REQUESTS request, String[] parameters) {		
    	new NetworkManager(listener, request, parameters);
    }
	
	public NetworkManager(NetworkListener listener, REQUESTS request, String[] parameters) {
        mListener = listener;
		mRequest = request;
    	mRequestParameters = parameters;
		mThread = new Thread(this);
    	mThread.start();
	}

	
	/**
	 * @param listener NetworkListener
	 */
    public void setNetworkListener(NetworkListener listener) {
        mListener = listener;
    }
	
    /**
     * Obsluga watku sieci
     * NETWORK_SERVER aders serwera
     * NETWORK_PORT port
     * TIMEOUT timeout w milisekundach
     */
	@Override
	public void run() {
        boolean success = false;
		while (!success) {
			// nc -l 8080
			Socket client = null;
			try {
				client = new Socket();
				client.connect(new InetSocketAddress(NETWORK_SERVER, NETWORK_PORT), TIMEOUT);
				
		        DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());
		        outToServer.writeUTF(requestToString(mRequest));
		        outToServer.writeByte(LINE_END_CHARACTER);
		        if (mRequestParameters != null) {
			        for (String line : mRequestParameters) {
				        outToServer.writeUTF(line);
				        outToServer.writeByte(LINE_END_CHARACTER);
			        }
		        }
		        outToServer.writeByte(LINE_END_CHARACTER);
		        
		        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
		        
		        switch (mRequest) {
				case PING: {
			    	
					/**
					 * Komenda:	PING
					 * Parametry:	IDENTYFIKATOR_UZYTKOWNIKA
					 * Odpowiedz:	playerWidth=SZEROKOSC_POSTACI
					 * 				antialiasing=ANTIALIASING
					 * 				fps=FPS
					 * 				dimensionVertical=WYSOKOSC_PLANSZY
					 * 				userid=IDENTYFIKATOR_UZYTKOWNIKA
					 * 				playerHorizontalPosition=POZYCJA_W_POZIOMIE_POSTACI
					 * 				tileHeight=WYSOKOSC_POLA_GRY
					 * 				playerHorizontalPointsRatio=MNOZNIK_PUNKTOW
					 * 				tileWidth=SZEROKOSC_POLA_GRY
					 * 				playerHeight=WYSOKOSC_POSTACI
					 * 				playerStroke=WIELKOSC_OBRAMOWANIA_POSTACI
					 * 				dimensionHorizontal=SZEROKOSC_PLANSZY
					 * Opis:			Komenda wysyłana przy uruchomieniu aplikacji. 
					 * W wyniku przesyłana jest podstawowa konfiguracja.
					 * IDENTYFIKATOR_UZYTKOWNIKA jest wymagany
					 */
					
					Properties properties = new Properties();
			    	properties.load(in);
					WorldSettings worldSettings = WorldSettings.getInstance();
					worldSettings.loadSettingsProperties(properties);
					worldSettings.commitSettings();
			    	success = mListener.networkResponse(mRequest, null);
			    	break;
				}
				case GETWORLD: {	
					
					/** 
					 * Komenda:	GETWORLD 
					 * Parametry:	NUMER_POZIOMU
					 * Odpowiedz:	width=SZEROKOSC_POZIOMU
					 * 				level=NUMER_POZIOMU
					 * 				world=SWIAT
					 * Opis:	Komenda wysyłana w celu pozyskania definicji kolejnego
					 * poziomu.
					 */
					
					int level = Integer.valueOf(mRequestParameters[0]);
					File file = new File( "config_world_" + level + ".properties" );
					BufferedWriter fileWriter = new BufferedWriter(new FileWriter(file));
					String l;
					while ( ( l = in.readLine() ) != null) {
						fileWriter.write( l + "\n" );
						System.out.println( l );
					}
					fileWriter.close();
					
			    	success = mListener.networkResponse(mRequest, null);
			    	break;
				}
				default: {
					
					/**
					 * Komenda:	RANKING
					 * Parametry:	IDENTYFIKATOR_UZYTKOWNIKA;NICK;LEVEL;PUNKTY
					 * Odpowiedz:	CZY_POZYCJA_DOTYCZY_GRACZA
					 * POZYCJA_W_RANKINGU
					 * 				NICK
					 * 				LEVEL
					 * 				PUNKTY
					 * 				x6
					 * Opis:	Komenda wysyłana po utracie wszystkich żyć lub gdy brak kolejnych poziomów.
					 * Przesyłane jest przynajmniej 6 górnych pozycji rankingu. Jeżeli otrzymano mniej niż 6 linii, ponowić zapytanie.
					 */
					
			        String line;
			        List<String> response = new ArrayList<String>();
			        while ( ( line = in.readLine() ) != null ) {
			        	if (line.isEmpty() || line.equals(LINE_END_CHARACTER))
							break;
			        	response.add(line.trim());
			        }
			    	success = mListener.networkResponse(mRequest, response);
			    	break;
				}
		        }
		        
	    		client.close();
		    } catch (ConnectException e) {
		    	e.printStackTrace();
		    	success = mListener.networkResponse(mRequest, null);
		    	
		    } catch (SocketTimeoutException e) {
		    	e.printStackTrace();
		    	success = mListener.networkResponse(mRequest, null);
		    	
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
			if (!success) {
				try {
					Thread.sleep(600);
				} catch (InterruptedException e2) {}
			}
        }
	}
	private static String requestToString(REQUESTS request) {
		switch (request) {
		case PING: default:
			return "PING";
		case RANKING:
			return "RANKING";
		case GETWORLD: 
			return "GETWORLD";
		}
	}
}
