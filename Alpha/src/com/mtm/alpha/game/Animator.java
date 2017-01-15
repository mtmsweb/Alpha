package com.mtm.alpha.game;

import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;

public class Animator implements Runnable {
    private BufferStrategy mBufferStrategy;
    private Thread mThread;
    private FrameListener mListener;
    private long mTimeBegin;
    
	public static interface FrameListener {
        void logicStep(long totalTime);
        void animationStep(long totalTime, Graphics2D graphics);
    }

	public void initAnimator(BufferStrategy strategy) {
		mBufferStrategy = strategy;
		
    	if (mThread == null)
    		mThread = new Thread(this);
    	
    	updateTimeBegin();
    	if (!mThread.isAlive())
    		mThread.start();
    }
	/**
	 * @param listener FrameListener
	 */
    public void setFrameListener(FrameListener listener) {
        mListener = listener;
    }
    public void updateTimeBegin() {
    	mTimeBegin = System.currentTimeMillis();
    }
    /**
     * odsiez bufor
     * @return
     */
    private boolean updateScreen() {
		try {
			mBufferStrategy.show();
			Toolkit.getDefaultToolkit().sync();
			return (!mBufferStrategy.contentsLost());
		} catch (Exception e) {
			return true;
		}
	}
	
	@Override
	public void run() {
        while (true) {
    		long nanoTimeStart = System.nanoTime();
    		long totalTime = System.currentTimeMillis() - mTimeBegin;
    		
    		try {
				mListener.logicStep(totalTime);
				
				Graphics2D graphics;
				do {
					graphics = (Graphics2D) mBufferStrategy.getDrawGraphics();
					mListener.animationStep(totalTime, graphics);
					graphics.dispose();
				} while (!updateScreen());
	            
				long nanoTimeEnd = System.nanoTime();
				long renderTime = (nanoTimeEnd - nanoTimeStart) / 1000000;
				
				Thread.sleep(Math.max(WorldSettings.threadSleep - renderTime, 2));
		    } catch (Exception e) {
		        // Jezeli cos wyjebalo, dzialaj dalej. Kulka z gowna
		    	System.err.println(e.getMessage());
		    }
		}
	}

    /**
     * Interpolacja x^2
     * @param time aktualny czas
     * @param beginTime czas początku animacji
     * @param endTime czas konca animacji
     * @return float x^2
     */
	public static float interpolate(long time, long beginTime, long endTime) {
		if (time < beginTime) return 0.0f;
		if (time > endTime) return 1.0f;
		return (float) Math.pow((float) (time - beginTime) / (endTime - beginTime), 2);
	}

}
