package com.mtm.alpha.ui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.mtm.alpha.game.Animator;
import com.mtm.alpha.game.Animator.FrameListener;
import com.mtm.alpha.game.AudioManager;
import com.mtm.alpha.game.InputManager;
import com.mtm.alpha.game.NetworkManager;
import com.mtm.alpha.game.NetworkManager.NetworkListener;
import com.mtm.alpha.game.NetworkManager.REQUESTS;
import com.mtm.alpha.game.Players;
import com.mtm.alpha.game.Ranking;
import com.mtm.alpha.game.Tile;
import com.mtm.alpha.game.World;
import com.mtm.alpha.game.WorldSettings;

/**
 * © 2016 mTm Michał Macioła Development
 * Contact at mtmsweb@gmail.com
 * License Apache 2.0
 * https://github.com/mtmsweb/Alpha
 */

public class GameScene extends Canvas implements FrameListener, MouseMotionListener, NetworkListener {

	private static final long serialVersionUID = 1L;
	
    /**
	 * Animator
	 * pelna animacji : interface logicStep i animationStep
	 */
	private Animator mAnimator;

	/**
	 * InputManager
	 * Obsluga klwiatury
	 * Mapa: keysMap
	 */
    private InputManager mInputManager;
	/**
	 * AudioManager
	 * Obsluga dzwieku
	 */
    private AudioManager mAudioManager;
	/**
	 * Ranking
	 * 6 rekordow (Players[6]) 
	 * Parsowanie getFromResponse
	 */
    private Ranking mRanking;

	/**
	 * Wymiary planszy z ustawien
	 */
    private int mSceneWidth;
    private int mSceneHeight;
    /** 
     * Skala okna, do animationStep
     */
    private float mHorizontalScale;
    private float mVerticalScale; 

    /** 
     * Aktualna mapa, zawiera level i worldWidth
     */
    private World mWorld;

    /**
     * Pozycja mapy. Liczy od <b>dimensionHorizontal * tileWidth</b> w dol
     */
    private int mTilesMapPositionHorizontal;
    /**
     * Pozycja postaci
     */
    private int mPlayerPositionHorizontal;
    private int mPlayerPositionVertical;
    
    /**
     * Pozostale zycia
     */
    private int mPlayerLives;
    /**
     * Zdobyte punkty, 
     * liczone jako max(mLevelPoints + floor((mPlayerPositionHorizontal + playerWidth - mTilesMapPositionHorizontal) / tileWidth * playerHorizontalPointsRatio), mPlayerPoints)
     */
    private int mPlayerPoints;
    private int mLevelPoints;

    /**
     * Aktualny widok
     */
    private GAME_STATE mState;
    /**
     * Nastepny widok, uzywany po zakonczeniu poziomu, wygrnej lub smierci 
     */
    private GAME_STATE mAfterRequestState;
    
    /**
     * Flagi obslugi sieci
     */
    private char mStateFlags;
    /**
     * Czy ruch myszy ma ruszac postacia
     */
    private boolean mMouseAsInputSource;
    
    
    private static enum GAME_STATE {ANIMATION, CONFIG_DOWNLOADING, RUNNING_AND_UNDIMMING, RUNNING, PAUSED, DEAD, RANKING_NICKNAME, RANKING_WAITING, WON, NEXT_LEVEL_SCREEN};
    private static enum ANCHOR_POINT {TOP_LEFT, TOP_RIGHT, BOTTOM_CENTER, BOTTOM_LEFT, BOTTOM_RIGHT, TOP_CENTER};
    
    /**
     * Czasy animacji w jednosce totalTime (milisekundach)
     */
    private static final long ANIMATION_PRE_MID_POINT = 3500;
    private static final long ANIMATION_MID_POINT = 4500;
    private static final long ANIMATION_PRE_END_POINT = 5500;
    private static final long ANIMATION_END_POINT = 6000;
    /**
     * Czas znikania przyciemnienia po pobraniu podstawowej konfiguracji
     */
    private static final long UNDIMMING_TIME = 1000;
    
    /** 
     * Flagi sieci do mStateFlags
     */
    private static final char STATE_FLAG_CONFIG_DOWNLOADED = 1 << 1;
    private static final char STATE_FLAG_RANKING_DOWNLOADED = 1 << 2;
    private static final char STATE_FLAG_NEXT_LEVEL_DOWNLOADED = 1 << 3;
    private static final char STATE_FLAG_NEXT_LEVEL_DOWNLOADING_INITED = 1 << 4;
    private static final char STATE_FLAG_RANKING_NO_NETWORK = 1 << 5;
    
    
    /**
     * GameScene initializacja 
     */
    public GameScene() {
    	mInputManager = new InputManager();
    	addKeyListener(mInputManager);
    	addMouseMotionListener(this);
    	
    	mAudioManager = new AudioManager();
    	mAudioManager.playSound(AudioManager.TYPE_BACKGROUND);
    	
    	mAnimator = new Animator();
    	mAnimator.setFrameListener(this);
    	
    	//mNetworkManager = new NetworkManager();
    	//mNetworkManager.setNetworkListener(this);

    	NetworkManager.initRequestTask(this, NetworkManager.REQUESTS.PING, buildRequestParameters(NetworkManager.REQUESTS.PING));
    	
    	mRanking = new Ranking();
    	
    	registerFonts();
    	
    	mState = GAME_STATE.ANIMATION;
    	mMouseAsInputSource = false;
    	initNewGame();
    }
    
    /**
     * GameScene initializacja po dodaniu do okna
     */
    public void initGameScene() {
    	createBufferStrategy(2); 
    	BufferStrategy strategy;
    	do {
    		strategy = getBufferStrategy();
		} while (strategy == null);
    	mAnimator.initAnimator(strategy);
    }
    
    /**
     * Aktualizuj wymiary planszy i skale (mSceneWidth, mSceneHeight, mHorizontalScale, mVerticalScale)
     * @param width
     * @param height
     */
    public void updateDimensions(int width, int height) { 	
    	if (WorldSettings.DEBUG)
    		System.out.print("updateDimensions: width " + width + ", height " + height + "; ");
    	mSceneWidth = WorldSettings.tileWidth * WorldSettings.dimensionHorizontal;
    	mSceneHeight = WorldSettings.tileHeight * WorldSettings.dimensionVertical;
    	mHorizontalScale = (float) width / mSceneWidth;
    	mVerticalScale = (float) height / mSceneHeight;
    	if (WorldSettings.DEBUG)
    		System.out.println("scale " + mHorizontalScale + ", " + mVerticalScale);
	}

    /**
     * Callback z Animator
     * Krok logiki animacji
     */
    @Override
	public void logicStep(long totalTime) {
    	if (mState == GAME_STATE.ANIMATION) {
    		if (totalTime >= ANIMATION_END_POINT) {
    			mState = GAME_STATE.CONFIG_DOWNLOADING;
    			mAnimator.updateTimeBegin();
    		}
    	} else if (mState == GAME_STATE.CONFIG_DOWNLOADING) {
    		if ((mStateFlags & STATE_FLAG_CONFIG_DOWNLOADED) == STATE_FLAG_CONFIG_DOWNLOADED && mWorld != null) {
    			mAnimator.updateTimeBegin();
    			mState = GAME_STATE.RUNNING_AND_UNDIMMING;
    		}
    	} else if (mState == GAME_STATE.RUNNING_AND_UNDIMMING) {
    		if (totalTime > UNDIMMING_TIME) {
    			mState = GAME_STATE.RUNNING;
    		}
    	} else if (mState == GAME_STATE.RANKING_WAITING) {
    		if ((mStateFlags & STATE_FLAG_RANKING_DOWNLOADED) == STATE_FLAG_RANKING_DOWNLOADED ||
    			(mStateFlags & STATE_FLAG_RANKING_NO_NETWORK) == STATE_FLAG_RANKING_NO_NETWORK) {
    			mState = mAfterRequestState;
    		}
    	}
    	
		float stepWorld = mWorld.stepMovement;
    	final float playerPositionStep = 5.0f;
    	Map<Integer, Boolean> keysMap = mInputManager.getKeysMap();
    	for (Map.Entry<Integer, Boolean> entry : keysMap.entrySet()) {
    		if (entry.getValue()) {
    			Integer keyCode = entry.getKey();
        		if (mState == GAME_STATE.RUNNING || mState == GAME_STATE.RUNNING_AND_UNDIMMING) {
        			// Klawisze WSAD przestaja dzialac na MacMini, gdy wiele klawiszy nacisnieto razem
        			// Na innych OS X dziala
        			if (InputManager.isUpKey(keyCode)) {
        				System.out.println("UP");
        				mPlayerPositionVertical -= playerPositionStep;
        			} else if (InputManager.isDownKey(keyCode)) {
        				System.out.println("DO");
        				mPlayerPositionVertical += playerPositionStep;
        			} else if (InputManager.isLeftKey(keyCode)) {
        				System.out.println("LF");
        				mPlayerPositionHorizontal -= playerPositionStep;
        			} else if (InputManager.isRightKey(keyCode)) {
        				System.out.println("RG");
        				mPlayerPositionHorizontal += playerPositionStep;
        			} else if (InputManager.isSpeedKey(keyCode)) {
        				System.out.println("SH");
    					stepWorld += 9.0f;
        			}
        		}
        		
        		if (mInputManager.shouldHandleActionKey(keyCode, totalTime)) {
        			if (InputManager.isEnterKey(keyCode) && mState == GAME_STATE.RANKING_NICKNAME) {
        				mState = GAME_STATE.RANKING_WAITING;
        				NetworkManager.initRequestTask(this, NetworkManager.REQUESTS.RANKING, buildRequestParameters(NetworkManager.REQUESTS.RANKING));
        				WorldSettings.username = mInputManager.getTyppedText();
     					mInputManager.setIsTyping(false);
     					
     				} else if (mInputManager.isEscKey(keyCode)) {
    	    			if (mState == GAME_STATE.ANIMATION) {
    	        			mState = GAME_STATE.CONFIG_DOWNLOADING;
    	        			mAnimator.updateTimeBegin();
    	        			
    	    			} else if (mState == GAME_STATE.RUNNING) {
    	    				mState = GAME_STATE.PAUSED;
    	    				
    	    			} else if (mState == GAME_STATE.PAUSED) {
    	    				mState = GAME_STATE.RUNNING;
    	    				
    	    			} else if (mState == GAME_STATE.DEAD || mState == GAME_STATE.WON || mState == GAME_STATE.NEXT_LEVEL_SCREEN) {
    	    				initNewGame();
    	    				mState = GAME_STATE.RUNNING;
    	    			}
    				} else if (InputManager.isInputChangeKey(keyCode)) {
    					mMouseAsInputSource = !mMouseAsInputSource;
    				}
        		}
    		}
    	}
    	
    	if (mWorld == null) {
    		return;
    	}
    	
    	if (mState == GAME_STATE.RUNNING || mState == GAME_STATE.RUNNING_AND_UNDIMMING) {
    		mTilesMapPositionHorizontal -= stepWorld;
			mPlayerPoints = (int) Math.max(mLevelPoints + Math.max(Math.floor((mPlayerPositionHorizontal + WorldSettings.playerWidth - mTilesMapPositionHorizontal) / WorldSettings.tileWidth * mWorld.pointsRatio), 0), mPlayerPoints);
			detectCollisions();
			 
			// WYGRANA lub nastepny posiom
			if ((mPlayerPositionHorizontal + WorldSettings.playerWidth - mTilesMapPositionHorizontal) / WorldSettings.tileWidth > mWorld.worldWidth + 1) {
				World worldNext = WorldSettings.loadWorld(mWorld.level + 1);
				if (worldNext == null) {
					System.out.println("Wygrana");
					mState = GAME_STATE.RANKING_NICKNAME;
		    		mAfterRequestState = GAME_STATE.WON;
					mInputManager.setTyppedText(WorldSettings.username);
					mAudioManager.stopSound(AudioManager.TYPE_BACKGROUND);
				} else {
					System.out.println("Następny poziom");
					mWorld = worldNext;
					mLevelPoints = mPlayerPoints;
					mState = GAME_STATE.NEXT_LEVEL_SCREEN;
					mAudioManager.stopSound(AudioManager.TYPE_BACKGROUND);
				}
			}
			
			// Pobierz mapę nastepnego poziomu
			if ((mStateFlags & STATE_FLAG_NEXT_LEVEL_DOWNLOADING_INITED) == 0) {
				if ((mPlayerPositionHorizontal + WorldSettings.playerWidth - mTilesMapPositionHorizontal) / WorldSettings.tileWidth > (int)(mWorld.worldWidth * 0.6f)) {
					System.out.println("Pobierz mapę nastepnego poziomu");
					mStateFlags |= STATE_FLAG_NEXT_LEVEL_DOWNLOADING_INITED;
					NetworkManager.initRequestTask(this, NetworkManager.REQUESTS.GETWORLD, buildRequestParameters(NetworkManager.REQUESTS.GETWORLD));
				}
			}
    	}
    }

    
    /**
     * Callback z Animator
     * Krok grafiki animacji
     */
    @Override
	public void animationStep(long totalTime, Graphics2D graphics) {
    	if (WorldSettings.antialiasing)
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    	graphics.scale(mHorizontalScale, mVerticalScale);
    	graphics.setColor(Color.white);
    	graphics.fillRect(0, 0, mSceneWidth, mSceneHeight);
    	
    	if (WorldSettings.DEBUG)
			drawTemporaryTileGrid(graphics);
    	
		drawTileGrid(graphics);
		drawPlayer(graphics);

		if (mState != GAME_STATE.RUNNING) {
			final float rectAlpha;
			if (mState == GAME_STATE.RUNNING_AND_UNDIMMING && totalTime <= UNDIMMING_TIME) {
				rectAlpha = 0.8f - 0.8f * Animator.interpolate(totalTime, 0, UNDIMMING_TIME);
			} else { rectAlpha = 0.8f; }
			drawDimmingFrame(graphics, rectAlpha);
		}
		
		drawPointsAndInfo(graphics);
		
		if ((mStateFlags & STATE_FLAG_CONFIG_DOWNLOADED) != STATE_FLAG_CONFIG_DOWNLOADED)
			drawNetworkConnectingText(graphics);

		if (mState == GAME_STATE.ANIMATION) {
			float rectAlpha = 1.0f - Animator.interpolate(totalTime, ANIMATION_MID_POINT, ANIMATION_END_POINT);
			drawDimmingFrame(graphics, rectAlpha);
			drawAnimationScreen(totalTime, graphics);
			
		} else if (mState == GAME_STATE.PAUSED) {
			drawPauseScreen(graphics, totalTime);
	    } else if (mState == GAME_STATE.RANKING_NICKNAME || mState == GAME_STATE.DEAD || mState == GAME_STATE.WON) {
			drawDeadOrWinScreen(graphics, totalTime);
	    } else if (mState == GAME_STATE.NEXT_LEVEL_SCREEN) {
			drawNextLevelScreen(graphics, totalTime);
	    } 
    }
    
    /**
     * Wyrysuj siatkę tła
     * @param graphics
     */
	public void drawTemporaryTileGrid(Graphics2D graphics) {
		graphics.setColor(Color.GRAY);
		for (int i=1; i<WorldSettings.dimensionVertical; i++) {
			final int y = (int) (i * WorldSettings.tileHeight);
			graphics.drawLine(0, y, mSceneWidth, y);
		}
		for (int i=1; i<WorldSettings.dimensionHorizontal; i++) {
			final int x = (int) (i * WorldSettings.tileWidth);
			graphics.drawLine(x, 0, x, mSceneHeight);
		}
	}
    /**
     * Rysuj planszę gry
     * @param graphics
     */
	public void drawTileGrid(Graphics2D graphics) {
		if (mWorld == null)
			return;
		final int colMinimum = (int) Math.floor(-mTilesMapPositionHorizontal / WorldSettings.tileWidth);
		final int colMaximum = colMinimum + WorldSettings.dimensionHorizontal;
		for (Tile tile : mWorld.tilesMap) {
			if (tile.type != Tile.TYPE_EMPTY && tile.col >= colMinimum && tile.col <= colMaximum) {
				final Color color;
				switch (tile.type) {
				case Tile.TYPE_EXTRA_LIVE:
					color = Color.decode("#dd4b50"); break;
				case Tile.TYPE_EXTRA_POINT:
					color = Color.decode("#edaa27"); break;
				case Tile.TYPE_OBSTACLE: default:
					color = Color.decode("#00bacc"); break;
				}
				graphics.setColor(color);
				graphics.fillRect (	tile.col * WorldSettings.tileWidth + mTilesMapPositionHorizontal, 
						tile.row * WorldSettings.tileHeight, 
						WorldSettings.tileWidth, WorldSettings.tileHeight);
			}
		}
	}
    /**
     * Rysuj postać
     * @param graphics
     */
	public void drawPlayer(Graphics2D graphics) {
		final int width = WorldSettings.playerWidth,
		height = WorldSettings.playerHeight,
		x = mPlayerPositionHorizontal,
		y = mPlayerPositionVertical,
		stroke = WorldSettings.playerStroke,
		strokeTwo = stroke + 1;
		
		graphics.setColor(Color.decode("#999999"));
		graphics.fillOval((int) (x - width * 0.5 - strokeTwo), (int) (y  - height * 0.5 - strokeTwo), (int) (width + 2 * strokeTwo), (int) (height + 2 * strokeTwo));
		graphics.setColor(Color.decode("#eeeeee"));
		graphics.fillOval((int) (x - width * 0.5 - stroke), (int) (y  - height * 0.5 - stroke), (int) (width + 2 * stroke), (int) (height + 2 * stroke));
		graphics.setColor(Color.decode("#2ecb93"));
		graphics.fillOval((int) (x - width * 0.5), (int) (y  - height * 0.5), (int) width, (int) height);
	}

    /**
     * Rysuj nagłówek ZYCIA PUNKTY POZIOM
     * @param graphics
     */
	public void drawPointsAndInfo(Graphics2D graphics) {
		final int margins = 10;
		graphics.setFont(new Font("Montserrat", Font.BOLD, 27)); 
		graphics.setColor(Color.decode("#dd4b50"));
		drawText(graphics, mPlayerLives + "xŻYCIA", margins, margins,  ANCHOR_POINT.TOP_LEFT);
		graphics.setColor(Color.decode("#edaa27"));
		drawText(graphics, mPlayerPoints + "xPUNKTY", mSceneWidth - margins, margins,  ANCHOR_POINT.TOP_RIGHT);
		
		if (mWorld == null) return;
		graphics.setColor(Color.decode("#edaa27"));
		drawText(graphics, mWorld.level + "xPOZIOM", mSceneWidth - (int) Math.min(mSceneWidth * 0.2f, 200) - margins, margins,  ANCHOR_POINT.TOP_RIGHT);
	}
    /**
     * Rysuj przyciemnione tło
     * @param graphics
     * @param float rectAlpha, podstawowo <b>0.8f</b>
     */
	private void drawDimmingFrame(Graphics2D graphics, float rectAlpha) {
		graphics.setColor(new Color(8, 8, 8, (int) (255 * rectAlpha)));
		graphics.fillRect(0, 0, mSceneWidth, mSceneHeight);
	}
    /**
     * Rysuj splash screen
     * @param totalTime
     * @param graphics
     */
	private void drawAnimationScreen(long totalTime, Graphics2D graphics) {
		final float textAlphaValue = (totalTime < ANIMATION_MID_POINT) ? 
				Animator.interpolate(totalTime, 0, ANIMATION_PRE_MID_POINT) : 
				(1.0f - Animator.interpolate(totalTime, ANIMATION_MID_POINT, ANIMATION_PRE_END_POINT));
		final int textAlpha = (int) (255 * textAlphaValue);
		graphics.setColor(new Color(255, 255, 255, textAlpha));
		graphics.setFont(new Font("Montserrat", Font.BOLD, 137));
		drawText(graphics, "ALPHA", (int) (mSceneWidth * 0.1f), (int) (mSceneHeight * 0.5f), ANCHOR_POINT.BOTTOM_LEFT);
		graphics.setFont(new Font("Montserrat", Font.BOLD, 19));
		drawText(graphics, "© 2016 mTm Michał Macioła Development", (int) (mSceneWidth * 0.1f), (int) (mSceneHeight * 0.5f), ANCHOR_POINT.TOP_LEFT);
	}
    /**
     * Rysuj informacje o pobieraniu ustawien z sieci
     * @param graphics
     */
	public void drawNetworkConnectingText(Graphics2D graphics) {
		graphics.setColor(Color.white);
		graphics.setFont(new Font("Montserrat", Font.ITALIC, 17));
		drawText(graphics, "Proba laczenia z serwerem...", (int) (mSceneWidth * 0.5f), (int) (mSceneHeight * 0.95f), ANCHOR_POINT.BOTTOM_CENTER);
	}
	
    /**
     * Rysuj tekst
     * @param graphics
     * @param text
     * @param x
     * @param y
     * @param anchor kotwica ANCHOR_POINT
     */
	public void drawText(Graphics2D graphics, String text, int x, int y, ANCHOR_POINT anchor) {
		FontMetrics metrics = graphics.getFontMetrics();
		switch (anchor) {
		case TOP_LEFT: case TOP_RIGHT: case TOP_CENTER:
			y += metrics.getHeight();
			break;
		default:
			break;	
		}
		switch (anchor) {
		case TOP_RIGHT: case BOTTOM_RIGHT:
			x -= metrics.stringWidth(text);
			break;	
		case BOTTOM_CENTER: case TOP_CENTER:
			x -= metrics.stringWidth(text) * 0.5f;
			break;
		default:
			break;	
		}
	    graphics.drawString(text, x, y);
	}
	
    /**
     * Rejestruj czcionki
     */
	private void registerFonts() {
		try {
		     GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		     graphicsEnvironment.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/fonts/Montserrat-Bold.ttf")));
		} catch (IOException|FontFormatException e) {
		     System.err.println(e);
		}
	}
	
    /**
     * Wstrzymaj grę
     * Flaga PAUSED i przerywana muzyka
     */
    public void pauseGame() {
    	mState = GAME_STATE.PAUSED;
		//mAudioManager.setAudioVolume(AudioManager.TYPE_BACKGROUND, WorldSettings.audioBackgroundPausedVolume);
    	mAudioManager.stopSound(AudioManager.TYPE_BACKGROUND);
    }

    /**
     * Inicjalizuj nową grę
     * Ustaw życia, punkty, level 1 i wykonaj initPlayerAndWorldPosition()
     */
    private void initNewGame() {
    	mPlayerLives = 3;
    	mPlayerPoints = 0;
		mStateFlags &= ~STATE_FLAG_NEXT_LEVEL_DOWNLOADING_INITED;
		
		if (mWorld == null || mState == GAME_STATE.DEAD) {
			mWorld = WorldSettings.loadWorld(1);
	    	if (mWorld == null) {
	        	NetworkManager.initRequestTask(this, NetworkManager.REQUESTS.GETWORLD, buildRequestParameters(NetworkManager.REQUESTS.GETWORLD, true));
	    	}
		}
		
		initPlayerAndWorldPosition();
    }
    /**
     * Ustaw pozycje gracza i mapy
     */
    private void initPlayerAndWorldPosition() {
    	mPlayerPositionHorizontal = WorldSettings.playerHorizontalPosition;
    	mPlayerPositionVertical = (int) (WorldSettings.dimensionVertical * WorldSettings.tileHeight * 0.5f);
    	
    	mTilesMapPositionHorizontal = WorldSettings.dimensionHorizontal * WorldSettings.tileWidth;
    }
    /**
     * Wykonywane w przypadku kolizji
     * Odejmij życie. Sprawdz czy gracz przezył. Jesli nie zakoncz gre
     */
    private void onCollisionDetected() {
    	mPlayerLives -= 1;
    	if (mPlayerLives == 0)  {
    		mState = GAME_STATE.RANKING_NICKNAME;
    		mAfterRequestState = GAME_STATE.DEAD;
			mInputManager.setTyppedText(WorldSettings.username);
			mAudioManager.stopSound(AudioManager.TYPE_BACKGROUND);
    	} else {
    		initPlayerAndWorldPosition();
    	}
    }
    
    /**
     * Ekran JESTES MARTWY
     * @param graphics
     * @param totalTime
     */
	private void drawDeadOrWinScreen(Graphics2D graphics, long totalTime) {
		graphics.setColor(new Color(255, 255, 255, 255));
		final int top = getAboveRankingWindowPosition();
		if (mAfterRequestState == GAME_STATE.DEAD) {
			/*FontMetrics metrics = graphics.getFontMetrics();
			graphics.setFont(new Font("Montserrat", Font.BOLD, 61));
			int width = metrics.stringWidth("JESTES");
			graphics.setFont(new Font("Montserrat", Font.BOLD, 81));
			width += metrics.stringWidth("MARTWY");*/

			graphics.setFont(new Font("Montserrat", Font.BOLD, 61));
			drawText(graphics, "JESTES MARTWY", (int) (mSceneWidth * 0.5f), top, ANCHOR_POINT.BOTTOM_CENTER);

			
		} else {
			graphics.setFont(new Font("Montserrat", Font.BOLD, 61));
			drawText(graphics, "WYGRALES", (int) (mSceneWidth * 0.5f), top, ANCHOR_POINT.BOTTOM_CENTER);
		}
		
		graphics.setFont(new Font("Montserrat", Font.ITALIC, 17));
		if (mState == GAME_STATE.RANKING_NICKNAME) {
			drawText(graphics, "Wprowadz nick i nacisnij enter by zapisac", (int) (mSceneWidth * 0.5f), (int) (mSceneHeight * 0.9f), ANCHOR_POINT.TOP_CENTER);
			drawNicknameField(graphics, totalTime);
		} else {
			drawText(graphics, "SPACJA by sprobowac ponownie", (int) (mSceneWidth * 0.5f), (int) (mSceneHeight * 0.9f), ANCHOR_POINT.TOP_CENTER);
			if ((mStateFlags & STATE_FLAG_RANKING_NO_NETWORK) == STATE_FLAG_RANKING_NO_NETWORK) {
				drawNoNetwork(graphics, totalTime);
		    } else {
		    	drawRankingWindow(graphics, totalTime);
		    }
		}
	}
    /**
     * Brak sieci
     * @param graphics
     * @param totalTime
     */
	private void drawNoNetwork(Graphics2D graphics, long totalTime) {
		int top = (int) (mSceneHeight * 0.5f);
		drawText(graphics, "Brak sieci", (int) (mSceneWidth * 0.5f), top, ANCHOR_POINT.BOTTOM_CENTER);
	}
    /**
     * Pozycja ryskowania rankingu
     * @return (mSceneHeight * 0.5f) - 112
     */
	private int getAboveRankingWindowPosition() {
		return (int) (mSceneHeight * 0.5f) - 112;
	}
    /**
     * Rysuj ranking
     * @param graphics
     * @param totalTime
     */
	private void drawRankingWindow(Graphics2D graphics, long totalTime) {
		int top = getAboveRankingWindowPosition() + 20;
		final int row = 32;
		drawText(graphics, "Najlepsi gracze", (int) (mSceneWidth * 0.2f), top, ANCHOR_POINT.TOP_LEFT);
		drawText(graphics, "Poziom", (int) (mSceneWidth * 0.7f), top, ANCHOR_POINT.TOP_RIGHT);
		drawText(graphics, "Punkty", (int) (mSceneWidth * 0.8f), top, ANCHOR_POINT.TOP_RIGHT);
				
		for (Players player : mRanking.getPlayers()) {
			top += row;
			drawText(graphics, "#" + player.position + " " + player.name, (int) (mSceneWidth * 0.2f), top, ANCHOR_POINT.TOP_LEFT);
			drawText(graphics, String.valueOf(player.level), (int) (mSceneWidth * 0.7f), top, ANCHOR_POINT.TOP_RIGHT);
			drawText(graphics, String.valueOf(player.points), (int) (mSceneWidth * 0.8f), top, ANCHOR_POINT.TOP_RIGHT);
		}
	}
    /**
     * Rysuj pole wpisania nicku
     * @param graphics
     * @param totalTime
     */
	private void drawNicknameField(Graphics2D graphics, long totalTime) {
		int bottom = (int) (mSceneHeight * 0.55f);
		int center = (int) (mSceneWidth * 0.5f);
		graphics.setFont(new Font("Montserrat", Font.ITALIC, 19));
		drawText(graphics, mInputManager.getTyppedText(), (int) (mSceneWidth * 0.5f), bottom, ANCHOR_POINT.BOTTOM_CENTER);
		graphics.drawLine(center - 200, bottom + 12, center + 200, bottom + 12);
	}
    /**
     * Menu pausy
     * @param graphics
     * @param totalTime
     */
	private void drawPauseScreen(Graphics2D graphics, long totalTime) {
		graphics.setColor(new Color(255, 255, 255, 255));
		
		graphics.setFont(new Font("Montserrat", Font.BOLD, 61));
		drawText(graphics, "WSTRZYMANE", (int) (mSceneWidth * 0.5f), (int) (mSceneHeight * 0.5f), ANCHOR_POINT.BOTTOM_CENTER);
		graphics.setFont(new Font("Montserrat", Font.ITALIC, 17));
		drawText(graphics, "nacisnij SPACJE by wrocic", (int) (mSceneWidth * 0.5f), (int) (mSceneHeight * 0.6f), ANCHOR_POINT.BOTTOM_CENTER);
		drawText(graphics, "zbieraj zolte punkty i czerwone zycia, omijaj niebieskie pola", (int) (mSceneWidth * 0.5f), (int) (mSceneHeight * 0.9f), ANCHOR_POINT.BOTTOM_CENTER);
		drawText(graphics, "sterowanie: kursor myszy, strzałki lub WSAD, klawisz M by zmienić. SHIFT by przyspieszyc", (int) (mSceneWidth * 0.5f), (int) (mSceneHeight * 0.9f), ANCHOR_POINT.TOP_CENTER);
	}

    /**
     * Menu next lev
     * @param graphics
     * @param totalTime
     */
	private void drawNextLevelScreen(Graphics2D graphics, long totalTime) {
		graphics.setColor(new Color(255, 255, 255, 255));
		
		graphics.setFont(new Font("Montserrat", Font.BOLD, 61));
		drawText(graphics, "NASTEPNY LEVEL", (int) (mSceneWidth * 0.5f), (int) (mSceneHeight * 0.5f), ANCHOR_POINT.BOTTOM_CENTER);
		graphics.setFont(new Font("Montserrat", Font.ITALIC, 17));
		drawText(graphics, "gotowy?", (int) (mSceneWidth * 0.5f), (int) (mSceneHeight * 0.6f), ANCHOR_POINT.BOTTOM_CENTER);
		drawText(graphics, "nacisnij SPACJE by zaczac", (int) (mSceneWidth * 0.5f), (int) (mSceneHeight * 0.9f), ANCHOR_POINT.TOP_CENTER);
	}
	
    /**
     * Sprawdź kolizję ze ścianą i planszą
     */
	public void detectCollisions() {
		if (mPlayerPositionHorizontal - WorldSettings.playerWidth * 0.5f < 0 ||
			mPlayerPositionHorizontal + WorldSettings.playerWidth * 0.5f > WorldSettings.dimensionHorizontal * WorldSettings.tileWidth ||
			mPlayerPositionVertical - WorldSettings.playerHeight * 0.5f < 0 ||	
			mPlayerPositionVertical + WorldSettings.playerHeight * 0.5f > WorldSettings.dimensionVertical * WorldSettings.tileHeight ) {
			onCollisionDetected();
			mAudioManager.playSound(AudioManager.TYPE_DEAD);
			return;
		}
		
		if (mWorld == null)
			return;
		int tileRight = (int) Math.floor((mPlayerPositionHorizontal + WorldSettings.playerWidth * 0.5f + WorldSettings.playerStroke - mTilesMapPositionHorizontal) / WorldSettings.tileWidth);
		int tileLeft = (int) Math.floor((mPlayerPositionHorizontal - WorldSettings.playerWidth * 0.5f - WorldSettings.playerStroke - mTilesMapPositionHorizontal) / WorldSettings.tileWidth);
		int tileTop = (int) Math.floor((mPlayerPositionVertical - WorldSettings.playerHeight * 0.5f - WorldSettings.playerStroke) / WorldSettings.tileHeight);
		int tileBottom = (int) Math.floor((mPlayerPositionVertical + WorldSettings.playerHeight * 0.5f + WorldSettings.playerStroke) / WorldSettings.tileHeight);
		for (Tile tile : mWorld.tilesMap) {
			if (tile.type != Tile.TYPE_EMPTY && tile.row >= tileTop && tile.row <= tileBottom && tile.col >= tileLeft && tile.col <= tileRight) {
				if (tile.type == Tile.TYPE_OBSTACLE) {
					onCollisionDetected();
					mAudioManager.playSound(AudioManager.TYPE_DEAD);
					break;
				} else if (tile.type == Tile.TYPE_EXTRA_POINT) {
					mPlayerPoints += 1;
					tile.type = Tile.TYPE_EMPTY;
				} else if (tile.type == Tile.TYPE_EXTRA_LIVE) {
					mPlayerLives += 1;
					tile.type = Tile.TYPE_EMPTY;
				}
			}
		}
	}

	
	/** 
	 * Obsluga myszy, wylacznie gdy <b>mMouseAsInputSource</b> true<br />
	 * addMouseMotionListener(this) w GameScene()
	 */
	@Override
	public void mouseDragged(MouseEvent e) {}
	@Override
	public void mouseMoved(MouseEvent e) {
		if (!mMouseAsInputSource || !(mState == GAME_STATE.RUNNING || mState == GAME_STATE.RUNNING_AND_UNDIMMING))
			return;
		final int margin = (int) (WorldSettings.playerWidth * 0.5f) + WorldSettings.playerStroke + 3;
        mPlayerPositionHorizontal = getMarginedPosition((int) (e.getX() / mHorizontalScale), mSceneWidth, margin);
        mPlayerPositionVertical = getMarginedPosition((int) (e.getY() / mHorizontalScale), mSceneHeight, margin);
	}
	private static int getMarginedPosition(int value, int edge, int margin) {
		if (value < margin)
			return margin;
		if (value > edge - margin)
			return edge - margin;
		return value;
	}
	
	/**
	 * Obsluga zapytan sieci
	 */
	@Override
	public boolean networkResponse(REQUESTS request, List<String> response) {
		switch (request) {
		case PING: {
			mStateFlags |= STATE_FLAG_CONFIG_DOWNLOADED;
			return true;
		}
		case GETWORLD: {
			mStateFlags |= STATE_FLAG_NEXT_LEVEL_DOWNLOADED;
			if (mWorld == null) {
				mWorld = WorldSettings.loadWorld(1);
			}
			return true;
		}
		case RANKING: {
			/* DEBUG
			for (String line : response) {
				System.out.println(line);
			} */
			if (response == null) {
				if (mWorld != null) {
					mStateFlags |= STATE_FLAG_RANKING_NO_NETWORK;
					return true;
				}
			} else if (mRanking.getFromResponse(response)) {
				mStateFlags |= STATE_FLAG_RANKING_DOWNLOADED;
				return true;
			}
		}
		}
		
		return false;
	}
	private String[] buildRequestParameters(REQUESTS request) {
		return buildRequestParameters(request, false);
	}
	/**
	 * buildRequestParameters
	 * @param request komenda
	 * @param force wylacznie by przekazac GETWORLD, aby pobral 1 level
	 * @return
	 */
	private String[] buildRequestParameters(REQUESTS request, boolean force) {
		switch (request) {
		case PING: {
			mStateFlags &= ~STATE_FLAG_CONFIG_DOWNLOADED;
			String[] params = { String.valueOf(WorldSettings.userid) };
			return params;
		}
		case GETWORLD: {
			mStateFlags &= ~STATE_FLAG_NEXT_LEVEL_DOWNLOADED;
			String[] params = { String.valueOf(force ? ( 1 ) : (mWorld.level + 1)) };
			return params;
		}
		case RANKING: {
			mStateFlags &= ~STATE_FLAG_RANKING_DOWNLOADED;
			mStateFlags &= ~STATE_FLAG_RANKING_NO_NETWORK;
			String[] params = {mInputManager.getTyppedText(), String.valueOf(mWorld.level), String.valueOf(mPlayerPoints)};
			return params;
		}
		}
		return null;
	}
}
