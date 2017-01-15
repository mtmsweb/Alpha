package com.mtm.alpha.game;

import java.util.List;

public class Ranking {
	private Players[] players = new Players[6];
	
	public boolean getFromResponse(List<String> response) {
		final int a = 5;
		if (response.size() < players.length * a) return false;
		
		System.out.println("getFromResponse");
		try {
			for (int i = 0; i < players.length; i++) {
				System.out.println("line: " + response.get(i * a + 1) + ", " + response.get(i * a + 2));
				boolean me = Boolean.valueOf(response.get(i * a + 0));
				int position = Integer.valueOf(response.get(i * a + 1));
				String name = response.get(i * a + 2);
				int level = Integer.valueOf(response.get(i * a + 3));
				int points = Integer.valueOf(response.get(i * a + 4));
				players[i] = new Players(me, position, name, level, points);
			}
			return true;
			
		}  catch (Exception e) {
	    	e.printStackTrace();
	    }
		return false;
	}

	public Players[] getPlayers() {
		return players;
	}
	
}
