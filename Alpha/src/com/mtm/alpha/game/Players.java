package com.mtm.alpha.game;

public class Players {

	public boolean me;
	public int position;
	public String name;
	public int level;
	public int points;
    
	public Players(boolean me, int position, String name, int level, int points){
        this.me = me;
        this.position = position;
        this.name = name;
        this.level =level;
        this.points = points;
    }
    
}
