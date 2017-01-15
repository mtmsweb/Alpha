package com.mtm.alpha.game;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

public class InputManager implements KeyListener {
	private Map<Integer, Boolean> keysMap;
	private StringBuilder tappedKeys;
	private boolean isTyping;
	private long lastActionKeyPressTime;
	private int lastActionKeyId;
	private static final long FORBIDDEN_CLICKING_TIME = 160;
	
	public InputManager() {
        //activeKeys = new HashSet<Integer> ();
		keysMap = new HashMap<> ();
    }
	
	@Override
	public void keyTyped(KeyEvent e) {
		if (isTyping) {
			char c = e.getKeyChar();
			if (c == KeyEvent.VK_BACK_SPACE) {
				if (tappedKeys.length() > 0) {
					tappedKeys.setLength(tappedKeys.length() - 1);
				}
			} else if (tappedKeys.length() <= 32 && c >= 0x20 && c != NetworkManager.LINE_END_CHARACTER) {
				tappedKeys.append(c);
			}
			System.out.println("keyTyped " + tappedKeys);
			e.consume();
		}
	}
	@Override
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		keysMap.put(keyCode, true);
		System.out.println("keyPressed");
	}
	@Override
	public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();
		keysMap.put(keyCode, false);
		lastActionKeyId = 0;
	}
	
	/**
	 * Podejmuje decyzje czy reagowac na wcisniety klawisz
	 * Bierze pod uwage czas od ostatniego zareagowania na dowolny klawisz
	 * oraz czy klawisz ktory jest analizowany zostal jako ostatni obsluzony
	 * Usuwanie informacji o obsluzeniu klawisza odbywa sie w keyReleased
	 * Jesli dwa klawisze jednoczesnie, to lipa ale dont worry: Kulka z gowna
	 */
	public boolean shouldHandleActionKey(int keyCode, long totalTime) {
		if (lastActionKeyId != keyCode 
			&& Math.abs(totalTime - lastActionKeyPressTime) > FORBIDDEN_CLICKING_TIME) {
			lastActionKeyId = keyCode; 
			lastActionKeyPressTime = totalTime;
			return true;
		}
		return false;
	}

	/**
	 * @return HashSet<Integer> activeKeys
	 */
	public Map<Integer, Boolean> getKeysMap(){
        return keysMap;
    }
	
	/**
	 * Podejmowanie decycji o reakcji na klawiature
	 */
	public static boolean isUpKey(int keyCode) {
		return keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W;
	}
	public static boolean isDownKey(int keyCode) {
		return keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S;
	}
	public static boolean isLeftKey(int keyCode) {
		return keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A;
	}
	public static boolean isRightKey(int keyCode) {
		return keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D;
	}
	public boolean isEscKey(int keyCode) {
		return (!isTyping && keyCode == KeyEvent.VK_SPACE) || keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER;
	}
	public static boolean isInputChangeKey(int keyCode) {
		return keyCode == KeyEvent.VK_M;
	}
	public static boolean isSpeedKey(int keyCode) {
		return keyCode == KeyEvent.VK_SHIFT;
	}
	public static boolean isEnterKey(int keyCode) {
		return keyCode == KeyEvent.VK_ENTER;
	}
	/**
	 * Obsluga pola tekstowego
	 * @param typing 
	 */
	public void setIsTyping(boolean typing){
		tappedKeys = new StringBuilder();
		isTyping = typing;
    }
	public void setTyppedText(String text) {
		setIsTyping(true);
		tappedKeys.append(text);
    }
	public String getTyppedText(){
        return tappedKeys.toString();
    }
}
