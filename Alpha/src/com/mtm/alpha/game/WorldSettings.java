package com.mtm.alpha.game;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Random;

public class WorldSettings {
	
	public static final boolean DEBUG = true;

    public static final String KEY_FPS = "fps";
    public static final String KEY_ANTIALIASING = "antialiasing";
	public static int fps = 30;
    public static long threadSleep = 1000 / fps;
    public static boolean antialiasing = true;

    public static final String KEY_TILE_WIDTH = "tileWidth";
    public static final String KEY_TILE_HEIGHT = "tileHeight";
    public static final String KEY_PLAYER_WIDTH = "playerWidth";
    public static final String KEY_PLAYER_HEIGHT = "playerHeight";
    public static final String KEY_PLAYER_STROKE = "playerStroke";
    public static final String KEY_PLAYER_HORIZONTAL_POSITION = "playerHorizontalPosition";
    public static final String KEY_PLAYER_HORIZONTAL_POINTS_RATIO = "playerHorizontalPointsRatio";
    public static final String KEY_DIMENSION_HORIZONTAL = "dimensionHorizontal";
    public static final String KEY_DIMENSION_VERTICAL = "dimensionVertical";

    /**
     * tileWidth: SZEROKOSC_POLA_GRY (jeden kafelek)
     */
    public static int tileWidth = 60;
    /**
     * tileHeight: SZEROKOSC_POLA_GRY (jeden kafelek)
     */
    public static int tileHeight = 60;
    /**
     * playerWidth: SZEROKOSC_POSTACI
     */
    public static int playerWidth = 24;
    /**
     * playerHeight: WYSOKOSC_POSTACI
     */
    public static int playerHeight = 24;
    /**
     * playerStroke: WIELKOSC_OBRAMOWANIA_POSTACI
     */
    public static int playerStroke = 4;
    /**
     * playerHorizontalPosition: POZYCJA_W_POZIOMIE_POSTACI
     */
    public static int playerHorizontalPosition = 120;
    /**
     * dimensionHorizontal: SZEROKOSC_PLANSZY
     */
    public static int dimensionHorizontal = 16;
    /**
     * dimensionVertical: WYSOKOSC_PLANSZY
     */
    public static int dimensionVertical = 9;

    public static final String KEY_USERNAME = "username";
    public static final String KEY_USERID = "userid";
    public static String username = "Nick";
    public static long userid = -1;

    /**
     * world: KEY_WORLD pelna mapa swiata
     * Kolejnosc: poczatek do konca rzedu -> nastepna kolumna
     * Wartosci: jak w Tile TYPE_...
     */
    public static final String KEY_WORLD = "world";
    /**
     * width: KEY_WORLD_WIDTH szerokosc mapy w kafelkach
     */
    public static final String KEY_WORLD_WIDTH = "width";
    /**
     * ratio: KEY_WORLD_POINTS_RATIO mnoznik punktow co kafelek
     */
    public static final String KEY_WORLD_POINTS_RATIO = "ratio";
    /**
     * move: KEY_WORLD_STEP_MOVEMENT krok przesuniecia planszy co wykonanie logicStep
     */
    public static final String KEY_WORLD_STEP_MOVEMENT = "move";
    
    private static WorldSettings instance = null;
    private WorldSettings() {
    	loadSettings();
	}
	public static WorldSettings getInstance() {
		if (instance == null) 
			instance = new WorldSettings();
		return instance;
	}

    /**
     * Ładuj ustawienia z pliku
     * Plik <b>config_global.properties</b>
     */
    public void loadSettings() {
    	Properties properties = new Properties();
    	try (InputStream input = new FileInputStream("config_global.properties")) {
    		properties.load(input);
    		loadSettingsProperties(properties);
    		
    	} catch (FileNotFoundException io) {
    		commitSettings();
    	} catch (IOException io) {
    		io.printStackTrace();
    	}
    }
    public void loadSettingsProperties(Properties properties) {
    	fps = Integer.valueOf(properties.getProperty(KEY_FPS, String.valueOf(fps)));
		antialiasing = Boolean.valueOf(properties.getProperty(KEY_ANTIALIASING, String.valueOf(antialiasing)));

		tileWidth = Integer.valueOf(properties.getProperty(KEY_TILE_WIDTH, String.valueOf(tileWidth)));
		tileHeight = Integer.valueOf(properties.getProperty(KEY_TILE_HEIGHT, String.valueOf(tileHeight)));
		playerWidth = Integer.valueOf(properties.getProperty(KEY_PLAYER_WIDTH, String.valueOf(playerWidth)));
		playerHeight = Integer.valueOf(properties.getProperty(KEY_PLAYER_HEIGHT, String.valueOf(playerHeight)));
		playerStroke = Integer.valueOf(properties.getProperty(KEY_PLAYER_STROKE, String.valueOf(playerStroke)));
		playerHorizontalPosition = Integer.valueOf(properties.getProperty(KEY_PLAYER_HORIZONTAL_POSITION, String.valueOf(playerHorizontalPosition)));
		//playerHorizontalPointsRatio = Float.valueOf(properties.getProperty(KEY_PLAYER_HORIZONTAL_POINTS_RATIO, String.valueOf(playerHorizontalPointsRatio)));
		dimensionHorizontal = Integer.valueOf(properties.getProperty(KEY_DIMENSION_HORIZONTAL, String.valueOf(dimensionHorizontal)));
		dimensionVertical = Integer.valueOf(properties.getProperty(KEY_DIMENSION_VERTICAL, String.valueOf(dimensionVertical)));
		
		username = properties.getProperty(KEY_USERNAME, username);
		userid = Long.valueOf(properties.getProperty(KEY_USERID, String.valueOf(userid)));
    }
    /**
     * Eksportuj ustawienia do pliku
     * Plik <b>config_global.properties</b>
     */
    public void commitSettings() {
    	Properties properties = new Properties();
    	try (OutputStream output = new FileOutputStream("config_global.properties")) {
    		properties.setProperty(KEY_FPS, String.valueOf(fps));
    		properties.setProperty(KEY_ANTIALIASING, String.valueOf(antialiasing));

    		properties.setProperty(KEY_TILE_WIDTH, String.valueOf(tileWidth));
    		properties.setProperty(KEY_TILE_HEIGHT, String.valueOf(tileHeight));
    		properties.setProperty(KEY_PLAYER_WIDTH, String.valueOf(playerWidth));
    		properties.setProperty(KEY_PLAYER_HEIGHT, String.valueOf(playerHeight));
    		properties.setProperty(KEY_PLAYER_STROKE, String.valueOf(playerStroke));
    		properties.setProperty(KEY_PLAYER_HORIZONTAL_POSITION, String.valueOf(playerHorizontalPosition));
    		//properties.setProperty(KEY_PLAYER_HORIZONTAL_POINTS_RATIO, String.valueOf(playerHorizontalPointsRatio));
    		properties.setProperty(KEY_DIMENSION_HORIZONTAL, String.valueOf(dimensionHorizontal));
    		properties.setProperty(KEY_DIMENSION_VERTICAL, String.valueOf(dimensionVertical));

    		properties.setProperty(KEY_USERNAME, String.valueOf(username));
    		properties.setProperty(KEY_USERID, String.valueOf(userid));
    		properties.store(output, null);
    		
    	} catch (IOException io) {
    		io.printStackTrace();
    	}
    }

    /**
     * Ładuj mapę z pliku
     * Plik <b>config_world_</b>LEVEL<b>.properties</b>
     * @param level
     * @return mapa Tile[]
     */
    public static World loadWorld(int level) {
    	World world = null;
    	Properties properties = new Properties();
    	try (InputStream input = new FileInputStream("config_world_" + level + ".properties")) {
    		properties.load(input);
    		world = loadWorldProperties(properties, level);
    		
    	/*} catch (FileNotFoundException io) {
    		tilesMap = generateRandomWorld(8);
    		commitWorld(world); // *** dla testu*/
    	} catch (IOException io) {
    		io.printStackTrace();
    	}
    	return world;
    }
    public static World loadWorldProperties(Properties properties, int level) {
    	World world = new World(level);
    	world.worldWidth = Integer.valueOf(properties.getProperty(KEY_WORLD_WIDTH, String.valueOf(dimensionHorizontal)));
    	world.pointsRatio = Float.valueOf(properties.getProperty(KEY_WORLD_POINTS_RATIO, String.valueOf( 1.0f )));
    	world.stepMovement = Float.valueOf(properties.getProperty(KEY_WORLD_STEP_MOVEMENT, String.valueOf( 1.0f )));
		
		String worldProperty = properties.getProperty(KEY_WORLD);
		if (worldProperty == null || worldProperty.isEmpty()) return null;
		world.tilesMap = new Tile[worldProperty.length()]; int i = 0;
		for (byte tile : worldProperty.getBytes()) {
    		int n = i % WorldSettings.dimensionVertical;
    		int m = (i - n) / WorldSettings.dimensionVertical;
    		world.tilesMap[i] = new Tile(m, n, tile);
			i++;
		}
		return world;
    }
    
    /**
     * Eksportuj ustawienia do pliku
     * Plik <b>config_world_</b>LEVEL<b>.properties</b>
     * @param level
     * @return mapa Tile[]
     */
    public static void commitWorld(World world) {
    	if (world == null) return;
    	Properties properties = new Properties();
    	try (OutputStream output = new FileOutputStream("config_world_" + world.level + ".properties")) {
    		byte[] worldBytes = worldToBytes(world);
    		properties.setProperty(KEY_WORLD, new String(worldBytes, StandardCharsets.UTF_8));
    		properties.setProperty(KEY_WORLD_WIDTH, String.valueOf(world.worldWidth));
    		properties.setProperty(KEY_WORLD_POINTS_RATIO, String.valueOf(world.pointsRatio));
    		properties.setProperty(KEY_WORLD_STEP_MOVEMENT, String.valueOf(world.stepMovement));
    		properties.store(output, null);
    	} catch (IOException io) {
    		io.printStackTrace();
    	}
    }
    
    /**
     * Zrzuc swiat to tablicy bajtow
	 * Kolejnosc: poczatek do konca rzedu -> nastepna kolumna
     * @param world
     * @return
     */
    private static byte[] worldToBytes(World world) {
    	int i = 0;
    	byte[] worldBytes = new byte[world.tilesMap.length]; 
		for (Tile tile : world.tilesMap) {
			worldBytes[i] = tile.type;
			i++;
		}
		return worldBytes;
    }
    
    /**
     * Generuj mapę lokalnie
     * @param widthMult mnożnik szerokości kafelek na okno
     * @return mapa Tile[]
     */
    private static World generateRandomWorld(int widthMult, int level) {
    	World world = new World(level);
    	WorldSettings.getInstance();
    	Random r = new Random();
    	int width = WorldSettings.dimensionHorizontal * widthMult;
    	world.worldWidth = width;
    	int count = width * WorldSettings.dimensionVertical;
    	world.tilesMap = new Tile[count];
    	int safeTile = r.nextInt(WorldSettings.dimensionVertical);
    	int safeTileNext = safeTile;
    	for (int i = 0; i < count; i++) {
    		int n = i % WorldSettings.dimensionVertical;
    		int m = (i - n) / WorldSettings.dimensionVertical;
    		final byte tileType;
    		if (n == 0) {
    			safeTileNext -= r.nextInt(3) - 1;
    			safeTileNext = Math.min(Math.max(safeTileNext, 0), WorldSettings.dimensionVertical - 1);
    		}
    		if (n == safeTile || n == safeTileNext) {
    			switch (r.nextInt(20)) {
	    		case 0:
	    			tileType = Tile.TYPE_EXTRA_POINT; break;
	    		case 1:
	    			tileType = Tile.TYPE_EXTRA_LIVE; break;
	    		default:
	    			tileType = Tile.TYPE_EMPTY; break;
	    		}
    			if (n == safeTile)
    				safeTile = safeTileNext;	
    		} else {
	    		switch (r.nextInt(50)) {
	    		case 0: case 1:
	    			tileType = Tile.TYPE_EXTRA_POINT; break;
	    		case 2:
	    			tileType = Tile.TYPE_EXTRA_LIVE; break;
	    		case 3: case 4: case 5: case 6:
	    			tileType = Tile.TYPE_EMPTY; break;
	    		default:
	    			tileType = Tile.TYPE_OBSTACLE; break;
	    		}
    		}
    		world.tilesMap[i] = new Tile(m, n, tileType);
    	}
    	return world;
    }
    
}
