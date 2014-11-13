
/**
 * org.hermit.android.sound: sound effects for Android.
 * 
 * These classes provide functions to help apps manage their sound effects.
 *
 * <br>Copyright 2009-2010 Ian Cameron Smith
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 * 
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */


package org.hermit.android.sound;


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.util.Log;


/**
 * Main sound effects player class.  This is a pretty thin wrapper around
 * {@link android.media.SoundPool}, but adds some mild usefulness such as
 * per-effect volume.
 */
public class Player
{

    // ******************************************************************** //
    // Package-Shared Types.
    // ******************************************************************** //

    // Class wrapping up a MediaPlayer with our information about its state.
    class PoolPlayer {
    	
    	/**
    	 * Create a PoolPlayer.  This allocates the MediaPlayer.
    	 */
    	private PoolPlayer() {
    		mediaPlayer = new MediaPlayer();
    		loadedEffect = null;
    	}

    	
        /**
         * Play the given sound effect.  The sound won't be played if the volume
         * would be zero or less.
         * 
         * @param   effect      The sound effect to play.
         * @param   vol         Volume for this sound, 0 - 1.
         * @param   loop        If true, loop the sound forever.
         * @return				true iff the sound was loaded OK.
         */
        private boolean load(Effect effect, float vol, boolean loop) {
        	// Set this player up for the given sound.
        	int resId = effect.getResourceId();
        	AssetFileDescriptor afd =
        				appContext.getResources().openRawResourceFd(resId);
        	try {
        		mediaPlayer.reset();
        		mediaPlayer.setDataSource(afd.getFileDescriptor(),
        							 afd.getStartOffset(),
        							 afd.getDeclaredLength());
        		mediaPlayer.prepare();
        	} catch (Exception e) {
        		Log.e(TAG, "Failed to set up media player: " + e.getMessage(), e);
        		return false;
        	} finally {
        		try {
        			afd.close();
        		} catch (IOException e) { }
        	}
    		loadedEffect = effect;

        	// Set the play volume.
        	mediaPlayer.setVolume(vol, vol);

        	// Loop if required.
        	mediaPlayer.setLooping(loop);

    		return true;
        }

        
        /**
         * Start this player.
         */
        private void start() {
        	if (mediaPlayer == null || loadedEffect == null)
        		throw new IllegalStateException("tried to play with no effect");
        	mediaPlayer.start();
        }

        
        /**
         * Stop this player.
         */
        private void stop() {
        	if (mediaPlayer == null || loadedEffect == null)
        		throw new IllegalStateException("tried to stop with no effect");
        	mediaPlayer.stop();
        	loadedEffect = null;
        }


        /**
         * Determine whether this player is playing anything.
         * 
         * @return              True iff something is playing in this player.
         */
        final boolean isPlaying() {
            return mediaPlayer.isPlaying();
        }
        

        /**
         * Determine whether this player is playing the given effect.
         * 
         * @param	e			Effect to check.
         * @return              True iff e is playing in this player.
         */
        final boolean isPlaying(Effect e) {
            return loadedEffect == e && mediaPlayer.isPlaying();
        }
        
        
        /**
         * Releases resources associated with this player and its MediaPlayer
         * object.
         */
        private void release() {
        	if (mediaPlayer == null) {
        		mediaPlayer.release();
        		mediaPlayer = null;
        		loadedEffect = null;
        	}
        }

       
    	// The actual media player.
        private MediaPlayer mediaPlayer;
    	
    	// The effect it is playing.
        private Effect loadedEffect;
    }
    

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a sound effect player that can handle 3 streams at once.
     * 
     * @param   context     Application context we're running in.
     */
    public Player(Context context) {
        this(context, 3);
    }
    

    /**
     * Create a sound effect player.
     * 
     * @param   context     Application context we're running in.
     * @param   streams     Maximum number of sound streams to play
     *                      simultaneously.
     */
    public Player(Context context, int streams) {
        appContext = context;
        numStreams = streams;
        soundEffects = new ArrayList<Effect>();

        // The player pool is null until we resume.
        playerPool = null;
    }


	// ******************************************************************** //
	// Sound Setup.
	// ******************************************************************** //

    /**
     * Add a sound effect to this player.
     * 
     * @param   resId       Resource ID of the sound sample for this effect.
     * @return              An Effect object representing the new effect.
     *                      Use this object to actually play the sound.
     */
    public Effect addEffect(int resId) {
        return addEffect(resId, 1f);
    }


    /**
     * Add a sound effect to this player.
     * 
     * @param   resId       Resource ID of the sound sample for this effect.
     * @param   vol         Base volume for this effect.
     * @return              An Effect object representing the new effect.
     *                      Use this object to actually play the sound.
     */
    public Effect addEffect(int resId, float vol) {
        Effect effect = new Effect(this, resId, vol);
        
        synchronized (this) {
            soundEffects.add(effect);
        }

        return effect;
    }


	// ******************************************************************** //
	// Suspend / Resume.
	// ******************************************************************** //

    /**
     * Resume this player.  This allocates the media resources for all our
     * registered sound effects.  This method must be called before the
     * player can be used, and must be called again after calling
     * {@link #suspend()}
     * 
     * <p>It's a good idea for apps to do this in Activity.onResume().
     */
    public void resume() {
    	synchronized (this) {
    		// Create the player pool.
    		if (playerPool == null) {
    			playerPool = new LinkedList<PoolPlayer>();
    			for (int i = 0; i < numStreams; ++i)
    				playerPool.add(new PoolPlayer());
    		}
    	}
    }


    /**
     * Suspend this player.  This closes down the media resources the
     * player is using.  It's a good idea for apps to do this in
     * Activity.onPause().
     * 
     * <p>Following suspend, {@link #resume()} must be called before this
     * player can be used again.
     */
    public void suspend() {
        synchronized (this) {
            if (playerPool != null) {
            	PoolPlayer pp;
            	while ((pp = playerPool.poll()) != null)
            		pp.release();
            	playerPool = null;
            }
        }
    }


    /**
     * Shut this player down completely.  This frees all resources
     * associated with this Player.
     * 
     * <p>Following shutdown, this instance can not be used again.
     */
    public void shutdown() {
        synchronized (this) {
        	suspend();
        	soundEffects = null;
        	appContext = null;
        }
    }


    // ******************************************************************** //
    // Sound Playing.
    // ******************************************************************** //

    /**
     * Get the overall gain for sounds.
     * 
     * @return		        Current gain.  1 = normal; 0 means don't play
     *                      sounds.
     */
    public float getGain() {
        return soundGain;
    }
    

    /**
     * Set the overall gain for sounds.
     * 
     * @param   gain        Desired gain.  1 = normal; 0 means don't play
     *                      sounds.
     */
    public void setGain(float gain) {
        soundGain = gain;
    }
    

    /**
     * Play the given sound effect.
     * 
     * @param   effect      Sound effect to play.
     */
    public void play(Effect effect) {
        play(effect, 1f, false);
    }
    

    /**
     * Play the given sound effect.  The sound won't be played if the volume
     * would be zero or less.
     * 
     * @param   effect      Sound effect to play.
     * @param   rvol        Relative volume for this sound, 0 - 1.
     * @param   loop        If true, loop the sound forever.
     * @throws  IllegalStateException   Attempting to play before
     *                      {@link #resume()} or after {@link #resume()}.
     */
    public void play(Effect effect, float rvol, boolean loop) {
        synchronized (this) {
            if (playerPool == null)
                throw new IllegalStateException("can't play while suspended");

        	// Calculate the play volume.
        	float vol = soundGain * rvol;
        	if (vol <= 0f) {
        		Log.e(TAG, "Computed volume=" + vol + "; ignoring");
        		return;
        	}
        	if (vol > 1f)
        		vol = 1f;

            // Get the next idle player and push it to the end.  Failing that,
        	// use the least recently used player, which is the first one.
            PoolPlayer player = null;
        	for (PoolPlayer p : playerPool) {
        		if (!p.isPlaying()) {
        			player = p;
        			break;
        		}
        	}
        	if (player == null)
        		player = playerPool.poll();
            playerPool.addLast(player);
     
            // Set this player up for the given sound.
            player.load(effect, vol, loop);

            // Set it playing.
            effect.setPlayer(player);
        	player.start();
        }
    }
	

    /**
     * Stop the given effect, if it's playing.
     * 
     * @param   effect      Sound effect to stop.
     * @throws  IllegalStateException   Attempting to stop before
     *                      {@link #resume()} or after {@link #resume()}.
     */
    public void stop(Effect effect) {
        synchronized (this) {
            if (playerPool == null)
                throw new IllegalStateException("can't stop while suspended");

            PoolPlayer player = effect.getPlayer();
            if (player != null) {
                player.stop();
                effect.setPlayer(null);
            }
        }
    }
    

    /**
     * Stop all streams.
     */
    public void stopAll() {
        synchronized (this) {
            for (Effect e : soundEffects)
                stop(e);
        }
    }
    
    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    private static final String TAG = "sound";


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Our parent application context.
    private Context appContext;
    
    // Maximum number of sound streams to play simultaneously.
    private final int numStreams;
    
    // The pool of players we use to play sounds.  The most recently
    // started player will be at the end; so the least recently used,
    // and the one which should be recycled next, is on top.
    // playerPool is null while we are suspended.
    private LinkedList<PoolPlayer> playerPool;
 
    // All sound effects.
    private ArrayList<Effect> soundEffects;
    
	// Current overall sound gain.  If zero, sounds are suppressed.
	private float soundGain = 1f;

}

