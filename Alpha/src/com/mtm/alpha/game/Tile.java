package com.mtm.alpha.game;

public class Tile {
	
	/**
	 * Kafelki planszy
	 */

	public static final int TYPE_EMPTY = 1;
	public static final int TYPE_OBSTACLE = TYPE_EMPTY + 1;
	public static final int TYPE_EXTRA_POINT = TYPE_OBSTACLE + 1;
	public static final int TYPE_EXTRA_LIVE = TYPE_EXTRA_POINT + 1;
	
	public int col;
	public int row;
	public byte type;
    
	public Tile(int m, int n, byte t){
        col = m;
        row = n;
        type = t;
    }
    
}
