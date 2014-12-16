package com.kcwdev.audio;

import java.io.IOException;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollInvocation;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiContext.OnLifecycleEvent;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiConvert;


import android.app.Activity;

@Kroll.proxy(creatableInModule=AdvancedAudioPlayerModule.class)
public class AudioPlayerProxy extends KrollProxy
	implements OnLifecycleEvent
{
	private static final String LCAT = "AudioPlayerProxy";
	private static final boolean DBG = TiConfig.LOGD;

	@Kroll.constant public static final int STATE_BUFFERING = MediaPlayerWrapper.STATE_BUFFERING;
	@Kroll.constant public static final int STATE_INITIALIZED = MediaPlayerWrapper.STATE_INITIALIZED;
	@Kroll.constant public static final int STATE_PAUSED = MediaPlayerWrapper.STATE_PAUSED;
	@Kroll.constant public static final int STATE_PLAYING = MediaPlayerWrapper.STATE_PLAYING;
	@Kroll.constant public static final int STATE_STARTING = MediaPlayerWrapper.STATE_STARTING;
	@Kroll.constant public static final int STATE_STOPPED = MediaPlayerWrapper.STATE_STOPPED;
	@Kroll.constant public static final int STATE_STOPPING = MediaPlayerWrapper.STATE_STOPPING;
	@Kroll.constant public static final int STATE_WAITING_FOR_DATA = MediaPlayerWrapper.STATE_WAITING_FOR_DATA;
	@Kroll.constant public static final int STATE_WAITING_FOR_QUEUE = MediaPlayerWrapper.STATE_WAITING_FOR_QUEUE;

	protected MediaPlayerWrapper snd;

	public AudioPlayerProxy(TiContext tiContext)
	{
		super(tiContext);

		tiContext.addOnLifecycleEventListener(this);
		setProperty("volume", 0.5, true);
	}

	@Override
	public void handleCreationDict(KrollDict options) {
		super.handleCreationDict(options);
		if (options.containsKey(TiC.PROPERTY_URL)) {
			setProperty(TiC.PROPERTY_URL, getTiContext().resolveUrl(null, TiConvert.toString(options, TiC.PROPERTY_URL)));
		}
		if (options.containsKey(TiC.PROPERTY_ALLOW_BACKGROUND)) {
			setProperty(TiC.PROPERTY_ALLOW_BACKGROUND, options.get(TiC.PROPERTY_ALLOW_BACKGROUND));
		}
		if (DBG) {
			Log.i(LCAT, "Creating audio player proxy for url: " + TiConvert.toString(getProperty("url")));
		}
	}


	@Kroll.getProperty
	public String getUrl() {
		return TiConvert.toString(getProperty(TiC.PROPERTY_URL));
	}

	@Kroll.setProperty @Kroll.method
	public void setUrl(KrollInvocation kroll, String url) {
		if (url != null) {
			setProperty(TiC.PROPERTY_URL, kroll.getTiContext().resolveUrl(null, TiConvert.toString(url)));			
			release();
			getSound();
		}
	}

	@Kroll.getProperty @Kroll.method
	public boolean isPlaying() {
		MediaPlayerWrapper s = getSound();
		if (s != null) {
			return s.isPlaying();
		}
		return false;
	}

	@Kroll.getProperty @Kroll.method
	public boolean isPaused() {
		MediaPlayerWrapper s = getSound();
		if (s != null) {
			return s.isPaused();
		}
		return false;
	}
	
	@Kroll.getProperty @Kroll.method
	public int getDuration() {
		MediaPlayerWrapper s = getSound();
		if (s != null) {
			return s.getDuration();
		}
		return 0;
		
	}

	// An alias for play so that
	@Kroll.method
	public void start() {
		play();
	}

	@Kroll.method
	public void play() {
		MediaPlayerWrapper s = getSound();
		if (s != null) {
			s.play();
		}
	}

	@Kroll.method
	public void pause() {
		MediaPlayerWrapper s = getSound();
		if (s != null) {
			s.pause();
		}
	}

	@Kroll.method
	public void release() {
		if (snd != null) {
			MediaPlayerWrapper s = getSound();
			if (s != null) {
				s.release();
				snd = null;
			}
		}
	}

	@Kroll.method
	public void destroy() {
		release();
	}

	@Kroll.method
	public void stop() {
		MediaPlayerWrapper s = getSound();
		if (s != null) {
			s.stop();
		}
	}
	
	@Kroll.method
	public void seek(int position) {
		MediaPlayerWrapper s = getSound();
		if (s != null) {
			s.setTime(position);
		}		
	}

	protected MediaPlayerWrapper getSound()
	{
		if (snd == null) {
			try {
				snd = new MediaPlayerWrapper(this);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setModelListener(snd);
		}
		return snd;
	}

	private boolean allowBackground() {
		boolean allow = false;
		if (hasProperty(TiC.PROPERTY_ALLOW_BACKGROUND)) {
			allow = TiConvert.toBoolean(getProperty(TiC.PROPERTY_ALLOW_BACKGROUND));
		}
		return allow;
	}

	public void onStart(Activity activity) {
	}

	public void onResume(Activity activity) {
		if (!allowBackground()) {
			if (snd != null) {
				snd.onResume();
			}
		}
	}

	public void onPause(Activity activity) {
		if (!allowBackground()) {
			if (snd != null) {
				snd.onPause();
			}
		}
	}

	public void onStop(Activity activity) {
	}

	public void onDestroy(Activity activity) {
		if (snd != null) {
			snd.onDestroy();
		}
		snd = null;
	}
}

