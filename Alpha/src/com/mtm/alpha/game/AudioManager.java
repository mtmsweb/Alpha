package com.mtm.alpha.game;

import java.io.File;
import java.util.HashMap;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public class AudioManager {

	public static final int TYPE_BACKGROUND = 0;
	public static final int TYPE_DEAD = TYPE_BACKGROUND + 1;
	private static HashMap<Integer, Clip> mClips;
	
	public AudioManager() {
		mClips = new HashMap<Integer, Clip>();
	}
	/**
	 * Odtwórz dźwięk
	 * @param audioType TYPE_BACKGROUND muzyka tła, TYPE_DEAD śmierć postaci
	 */
	public void playSound(int audioType) {
		try {
	        final Clip clip;
	    	if (mClips.containsKey(audioType)) {
	    		clip = mClips.get(audioType);
	    		if (clip.isRunning())
	    			clip.stop();
	    		clip.setFramePosition(0);
	    	} else {
		    	AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getAudioFile(audioType));
		        clip = AudioSystem.getClip();
		        clip.open(audioInputStream);
		        if (audioType == TYPE_BACKGROUND)
		        	clip.loop(Clip.LOOP_CONTINUOUSLY);
		        mClips.put(audioType, clip);
	        }
	        clip.start();
	    } catch(Exception ex) {
	        System.err.println("playSound: error with playing sound");
	        ex.printStackTrace();
	    }
	}
	/**
	 * Zatrzymaj dźwięk
	 * @param audioType TYPE_BACKGROUND muzyka tła, TYPE_DEAD śmierć postaci
	 */
	public void stopSound(int audioType) {
		try {
			if (mClips.containsKey(audioType)) {
				final Clip clip = mClips.get(audioType);
	    		if (clip.isRunning())
	    			clip.stop();
			} else {
				System.out.println("stopSound: audio not found");				
			}
	    } catch(Exception ex) {
	        System.err.println("playSound: error with stopping sound");
	        ex.printStackTrace();
	    }
	}
	/**
	 * Wybierz głośność
	 * @param audioType TYPE_BACKGROUND muzyka tła, TYPE_DEAD śmierć postaci
	 * @param volume dB
	 */
	public void setAudioVolume(int audioType, float volume) {
		try {
			if (mClips.containsKey(audioType)) {
				final Clip clip = mClips.get(audioType);
				final FloatControl floatControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				floatControl.setValue(volume);
				if (WorldSettings.DEBUG)
					System.out.println("setAudioVolume: audio (" + audioType + ") volume changed to " + volume);
			} else {
				System.out.println("setAudioVolume: audio not found");				
			}
	    } catch(Exception ex) {
	        System.err.println("setAudioVolume: error with changing volume");
	        ex.printStackTrace();
	    }
	}
	
	private File getAudioFile(int audioType) {
		switch (audioType) {
		case TYPE_BACKGROUND:
			return new File("res/audio/background.wav").getAbsoluteFile();
		case TYPE_DEAD:
			return new File("res/audio/dead.wav").getAbsoluteFile();
		}
		return null;
	}
	
}
