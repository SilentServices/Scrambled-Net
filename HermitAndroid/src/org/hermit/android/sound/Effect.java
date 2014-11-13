
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


/**
 * Class representing a specific sound effect.  Apps can create an
 * instance by calling {@link Player#addEffect(int)}, or
 * {@link Player#addEffect(int, float)}.
 */
public class Effect
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a sound effect description.  This constructor is not public;
     * outside users get an instance by calling {@link Player#addEffect(int)},
     * or {@link Player#addEffect(int, float)}.
     * 
     * @param   player     The Player this effect belongs to.
     * @param   resId      Resource ID of the sound sample for this effect.
     * @param   vol        Base volume for this effect (used to modify the
     *                     sound file's inherent volume, if needed).  1 is
     *                     normal.
     */
    Effect(Player player, int resId, float vol) {
        soundPlayer = player;
        clipResourceId = resId;
        playerSoundId = -1;
        playVol = vol;
    }
    

	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

    /**
     * Get the resource ID of the sound sample for this effect.
     * 
     * @return              Resource ID of the sound sample for this effect.
     */
    final int getResourceId() {
        return clipResourceId;
    }


    /**
     * Get the player's ID for this sound.
     * 
     * @return              Player's ID of this effect.  -1 f we don't have
     *                      one.
     */
    final int getSoundId() {
        return playerSoundId;
    }

    
    /**
     * Set the player's ID for this sound.
     * 
     * @param   id          Player's ID of this effect.  -1 f we don't have
     *                      one.
     */
    final void setSoundId(int id) {
        playerSoundId = id;
    }
    

    /**
     * Get the player for this sound.
     * 
     * @return              Current player of this effect.  null if not playing.
     */
    final Player.PoolPlayer getPlayer() {
        return player;
    }

    
    /**
     * Set the player for this sound.
     * 
     * @param   p           Current player of this effect.  null if not playing.
     */
    final void setPlayer(Player.PoolPlayer p) {
        player = p;
    }
    

    /**
     * Get the effect's volume.
     * 
     * @return              Base volume for this effect.
     */
    final float getPlayVol() {
        return playVol;
    }

    
    /**
     * Set the effect's volume.
     * 
     * @param   playVol     New base volume.
     */
    final void setPlayVol(float vol) {
        playVol = vol;
    }


    // ******************************************************************** //
    // Playing.
    // ******************************************************************** //

    /**
     * Play this sound effect.
     */
    public void play() {
        soundPlayer.play(this, playVol, false);
    }


    /**
     * Play this sound effect.
     * 
     * @param   rvol            Relative volume for this sound, 0 - 1.
     */
    public void play(float rvol) {
        soundPlayer.play(this, rvol * playVol, false);
    }


    /**
     * Start playing this sound effect in a continuous loop.
     */
    public void loop() {
        soundPlayer.play(this, playVol, true);
    }


    /**
     * Play this sound effect.
     * 
     * @param   rvol            Relative volume for this sound, 0 - 1.
     * @param   loop            If true, loop the sound forever.
     */
    void play(float rvol, boolean loop) {
        soundPlayer.play(this, rvol * playVol, loop);
    }

    
    /**
     * Stop this sound effect immediately.
     */
    public void stop() {
        soundPlayer.stop(this);
    }


    /**
     * Determine whether this effect is playing.
     * 
     * @return              True if this sound effect is playing.
     */
    public final boolean isPlaying() {
        return player != null && player.isPlaying(this);
    }


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// The player this effect is attached to.
	private final Player soundPlayer;

    // Base volume for this effect.
    private float playVol;

    // Resource ID of the effect's audio clip.
    private final int clipResourceId;

    // Sound ID of this effect in the sound player.  -1 if not set; e.g.
    // we don't currently have a media connection.
    private int playerSoundId;

    // The pool player which is playing this effect; null
    // if it's not playing.
    private Player.PoolPlayer player = null;

}

