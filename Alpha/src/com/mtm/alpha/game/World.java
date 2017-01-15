package com.mtm.alpha.game;

public class World {
	
	/**
	 * Opis planszy
	 * Kolejnosc: poczatek do konca rzedu -> nastepna kolumna
	 */
	
	public Tile[] tilesMap;
    /**
     * worldWidth: szerokosc w kafelkach
     */
	public int worldWidth;
    /**
     * level: LEVEL
     */
	public int level;
    /**
     * pointsRatio: MNOZNIK_PUNKTOW
     */
    public float pointsRatio = 1.0f;
    /**
     * stepMovement: KROK_PRZESUNIECIE
     */
    public float stepMovement = 1.0f;
	
	public World(int l) {
		level = l;
		worldWidth = WorldSettings.dimensionHorizontal;
	}
	
}
