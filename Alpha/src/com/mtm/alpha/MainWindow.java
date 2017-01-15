package com.mtm.alpha;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JFrame;

import com.mtm.alpha.game.WorldSettings;
import com.mtm.alpha.ui.GameScene;

public class MainWindow extends JFrame implements ComponentListener {

	private static final long serialVersionUID = 1L;
	
	private GameScene mGameScene;
	
    public MainWindow() {
    	addComponentListener(this);
    }
    
    /**
     * Initializacja okna głównego
     */
    public void initializeWindow() {
    	setTitle("Alpha by mTm Michał T. Macioła");
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);    	
    	
    	mGameScene = new GameScene();
    	getContentPane().add(mGameScene, BorderLayout.CENTER);
    	
    	WorldSettings.getInstance();
    	final int width = WorldSettings.tileWidth * WorldSettings.dimensionHorizontal;
    	final int height = WorldSettings.tileHeight * WorldSettings.dimensionVertical;

    	Dimension dimension = new Dimension(width, height);
    	getContentPane().setPreferredSize(dimension);
    	pack();
    	setLocationRelativeTo(null);
    	setVisible(true);
    	
    	mGameScene.initGameScene();
    }

	@Override
	public void componentMoved(ComponentEvent e) {}
	@Override
	public void componentResized(ComponentEvent e) {
		final Insets insets = getInsets();
		mGameScene.updateDimensions(getWidth() - insets.left - insets.right, getHeight() - insets.top - insets.bottom);
	}
	@Override
	public void componentShown(ComponentEvent e) {}
	@Override
	public void componentHidden(ComponentEvent e) {
		mGameScene.pauseGame();
		System.out.println("componentHidden");
	}
    
}
