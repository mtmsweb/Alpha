package com.mtm.alpha;

import java.util.Random;

public class WorldGenerator {

    /**
     * Generuj mapę lokalnie
     * @param widthMult mnożnik szerokości kafelek na okno
     * @return mapa Tile[]
     */
    public static World generateRandomWorld(int widthMult, int level) {
    	World world = new World(level);
    	world.pointsRatio = 1.0f + Math.min(level / 10, 1);
    	world.stepMovement = world.pointsRatio;

    	Random r = new Random();
    	int width = Settings.dimensionHorizontal * widthMult;
    	world.worldWidth = width;
    	int count = width * Settings.dimensionVertical;
    	world.tilesMap = new Tile[count];
    	int safeTile = r.nextInt(Settings.dimensionVertical);
    	int safeTileNext = safeTile;
    	for (int i = 0; i < count; i++) {
    		int n = i % Settings.dimensionVertical;
    		int m = (i - n) / Settings.dimensionVertical;
    		final byte tileType;
    		if (n == 0) {
    			safeTileNext -= r.nextInt(3) - 1;
    			safeTileNext = Math.min(Math.max(safeTileNext, 0), Settings.dimensionVertical - 1);
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
    public static byte[] worldToBytes(World world) {
    	int i = 0;
    	byte[] worldBytes = new byte[world.tilesMap.length]; 
		for (Tile tile : world.tilesMap) {
			worldBytes[i] = tile.type;
			i++;
		}
		return worldBytes;
    }
    
}
