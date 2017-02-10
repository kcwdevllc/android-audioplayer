package com.kcwdev.audio;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollPropertyChange;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollProxyListener;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiConvert;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.webkit.URLUtil;

public class MediaPlayerWrapper
	implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, KrollProxyListener,
	MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener
{
	private static final String LCAT = "AdvancedAudioPlayer";
	private static final boolean DBG = TiConfig.LOGD;
	
	public static final String PROPERTY_VOLUME = "volume";

	public static final int STATE_BUFFERING	= 0;	// current playback is in the buffering from the network state
	public static final int STATE_INITIALIZED = 1;	// current playback is in the initialization state
	public static final int STATE_PAUSED = 2;	// current playback is in the paused state
	public static final int STATE_PLAYING = 3;	// current playback is in the playing state
	public static final int STATE_STARTING = 4;	// current playback is in the starting playback state
	public static final int STATE_STOPPED = 5; // current playback is in the stopped state
	public static final int STATE_STOPPING = 6; // current playback is in the stopping state
	public static final int STATE_WAITING_FOR_DATA = 7;  // current playback is in the waiting for audio data from the network state
	public static final int STATE_WAITING_FOR_QUEUE	= 8; //	current playback is in the waiting for audio data to fill the queue state

	public static final String STATE_BUFFERING_DESC = "buffering";	// current playback is in the buffering from the network state
	public static final String STATE_INITIALIZED_DESC = "initialized";	// current playback is in the initialization state
	public static final String STATE_PAUSED_DESC = "paused";	// current playback is in the paused state
	public static final String STATE_PLAYING_DESC = "playing";	// current playback is in the playing state
	public static final String STATE_STARTING_DESC = "starting";	// current playback is in the starting playback state
	public static final String STATE_STOPPED_DESC = "stopped"; // current playback is in the stopped state
	public static final String STATE_STOPPING_DESC = "stopping"; // current playback is in the stopping state
	public static final String STATE_WAITING_FOR_DATA_DESC = "waiting for data";  // current playback is in the waiting for audio data from the network state
	public static final String STATE_WAITING_FOR_QUEUE_DESC = "waiting for queue"; //	current playback is in the waiting for audio data to fill the queue state

	public static final String EVENT_COMPLETE = "complete";
	public static final String EVENT_ERROR = "error";
	public static final String EVENT_CHANGE = "change";
	public static final String EVENT_PROGRESS = "progress";
	public static final String EVENT_BUFFERING = "buffering";
	
	public static final String EVENT_COMPLETE_JSON = "{ type : '" + EVENT_COMPLETE + "' }";

	private boolean paused = false;
	private boolean looping = false;

	protected KrollProxy proxy;
	protected MediaPlayer mp;
	protected float volume;
	protected boolean speakerphone;
	protected boolean playOnResume;
	protected boolean remote;
	protected Timer progressTimer;

	public MediaPlayerWrapper(KrollProxy proxy) throws IOException
	{
		this.proxy = proxy;
		this.playOnResume = false;
		this.remote = false;
		String url = TiConvert.toString(proxy.getProperty(TiC.PROPERTY_URL));
		if (url != null && url.length() > 0)
		{
			this.initialize();			
		}
	}

	protected void initialize()
		throws IOException
	{
		try {
			setState(STATE_STARTING);
			mp = new MediaPlayer();
			String url = TiConvert.toString(proxy.getProperty(TiC.PROPERTY_URL));
			if (URLUtil.isAssetUrl(url)) {
				Context context = proxy.getActivity().getApplicationContext();
				String path = url.substring(TiConvert.ASSET_URL.length());
				AssetFileDescriptor afd = null;
				try {
					afd = context.getAssets().openFd(path);
					// Why mp.setDataSource(afd) doesn't work is a problem for another day.
					// http://groups.google.com/group/android-developers/browse_thread/thread/225c4c150be92416
					mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
				} catch (IOException e) {
					Log.e(LCAT, "Error setting file descriptor: ", e);
				} finally {
					if (afd != null) {
						afd.close();
					}
				}
			} else {
				Uri uri = Uri.parse(url);
				if (uri.getScheme().equals(TiC.PROPERTY_FILE)) {
					mp.setDataSource(uri.getPath());
				} else {
					Log.d(LCAT,"audio is a remote url." + url);

//					IceCastScraper ic = new IceCastScraper();
//					List<Stream> streams = ic.scrape(URI.create(url));
//					String song = streams.get(0).getCurrentSong();
//					String songUrl = streams.get(0).getUri().toString();
//					Log.d(LCAT,"audio song. " + song);
//					Log.d(LCAT,"audio URL. " + songUrl);
					
//					
//					ShoutCastScraper sc = new ShoutCastScraper();
//					List<Stream> streams = sc.scrape(URI.create(url));
//					
//					Log.d(LCAT,"streams count " + streams.size());
//					
//					String song = streams.get(0).getCurrentSong();
//					String songUrl = streams.get(0).getUri().toString();
//					Log.d(LCAT,"audio song. " + song);
//					Log.d(LCAT,"audio URL. " + songUrl);
					remote = true;
					mp.setDataSource(url);
				}
			}
			
			setSpeakerphoneOn();
			
			mp.setLooping(looping);
			mp.setOnCompletionListener(this);
			mp.setOnErrorListener(this);
			mp.setOnInfoListener(this);
			mp.setOnBufferingUpdateListener(this);
			mp.setOnPreparedListener(this);

			mp.prepare(); // Probably need to allow for Async
			setState(STATE_INITIALIZED);

			setVolume(volume);
			if (proxy.hasProperty(TiC.PROPERTY_TIME)) {
				setTime(TiConvert.toInt(proxy.getProperty(TiC.PROPERTY_TIME)));
			}
		} catch (Throwable t) {
			Log.w(LCAT, "Issue while initializing : " , t);
			release();
			setState(STATE_STOPPED);
		}
	}

	public boolean isLooping()
	{
		return looping;
	}

	public boolean isPaused()
	{
		return paused;
	}

	public boolean isPlaying()
	{
		boolean result = false;
		if (mp != null) {
			result = mp.isPlaying();
		}
		return result;
	}

	public void pause()
	{
		try {
			if (mp != null) {
				if(mp.isPlaying()) {
					if (DBG) {
						Log.d(LCAT,"audio is playing, pause");
					}
					//if (remote) {
						stopProgressTimer();
					//}
					mp.pause();
					paused = true;
					setState(STATE_PAUSED);
				}
			}
		} catch (Throwable t) {
			Log.w(LCAT, "Issue while pausing : " , t);
		}
	}

	public void play()
	{
		try {		
			if (mp != null) {
				if (!isPlaying()) {
					if (DBG) {
						Log.d(LCAT,"audio is not playing, starting.");
					}
					setVolume(volume);
					if (DBG) {
						Log.d(LCAT, "Play: Volume set to " + volume);
					}
					mp.start();
					setState(STATE_PLAYING);
					paused = false;
					//if (remote) {
						startProgressTimer();
					//}
				}
				setState(STATE_PLAYING);
			}					

		} catch (Throwable t) {
			Log.w(LCAT, "Issue while playing : " , t);
			reset();
		}
	}

	public void reset()
	{
		try {
			if (mp != null) {
				//if (remote) {
					stopProgressTimer();
				//}

				setState(STATE_STOPPING);
				mp.stop();
				mp.seekTo(0);
				looping = false;
				paused = false;
				setState(STATE_STOPPED);
			}
		} catch (Throwable t) {
			Log.w(LCAT, "Issue while resetting : " , t);
		}
	}

	public void release()
	{
		try {
			if (mp != null) {

				mp.setOnCompletionListener(null);
				mp.setOnErrorListener(null);
				mp.setOnBufferingUpdateListener(null);
				mp.setOnInfoListener(null);
				mp.setOnPreparedListener(null);

				mp.release();
				mp = null;
				if (DBG) {
					Log.d(LCAT, "Native resources released.");
				}
				remote = false;
			}
		} catch (Throwable t) {
			Log.w(LCAT, "Issue while releasing : " , t);
		}
	}

	public void setLooping(boolean loop)
	{
		try {
			if(loop != looping) {
				if (mp != null) {
					mp.setLooping(loop);
				}
				looping = loop;
			}
		} catch (Throwable t) {
			Log.w(LCAT, "Issue while configuring looping : " , t);
		}
	}

	public void setVolume(float volume)
	{
		try {
			if (volume < 0.0f) {
				this.volume = 0.0f;
				Log.w(LCAT, "Attempt to set volume less than 0.0. Volume set to 0.0");
			} else if (volume > 1.0) {
				this.volume = 1.0f;
				proxy.setProperty(PROPERTY_VOLUME, volume);
				Log.w(LCAT, "Attempt to set volume greater than 1.0. Volume set to 1.0");
			} else {
				this.volume = volume; // Store in 0.0 to 1.0, scale when setting hw
			}
			if (mp != null) {
				float scaledVolume = this.volume;
				mp.setVolume(scaledVolume, scaledVolume);
			}
		} catch (Throwable t) {
			Log.w(LCAT, "Issue while setting volume : " , t);
		}
	}

	public int getDuration()
	{
		int duration = 0;
		if (mp != null) {
			duration = mp.getDuration();
		}
		return duration;
	}

	public int getTime()
	{
		int time = 0;

		if (mp != null) {
			time = mp.getCurrentPosition();
		} else {
			time = TiConvert.toInt(proxy.getProperty(TiC.PROPERTY_TIME));
		}

		return time;
	}

	public void setTime(int position)
	{
		if (position < 0) {
			position = 0;
		}

		if (mp != null) {
			int duration = mp.getDuration();
			if (position > duration) {
				position = duration;
			}

			mp.seekTo(position);
		}

		proxy.setProperty(TiC.PROPERTY_TIME, position);
	}
	
	public void setSpeakerphoneOn() {		
		if (mp != null) {
			
			Context context = proxy.getActivity().getBaseContext();
			AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			
			speakerphone = true; 
			if(proxy.hasProperty("speakerphone")) {
				speakerphone = TiConvert.toBoolean(proxy.getProperty("speakerphone"));
			}
			
			if(speakerphone) {
				mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
				if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.KITKAT) {
					am.setMode(AudioManager.STREAM_MUSIC);
					am.setSpeakerphoneOn(true);				     
				}
				
			} else {
				mp.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
				if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.KITKAT) {
					am.setMode(AudioManager.MODE_IN_CALL);
					am.setSpeakerphoneOn(false);
				}
				
			}
		}
	}

	private void setState(int state)
	{
		proxy.setProperty("state", state);
		String stateDescription = "";

		switch(state) {
			case STATE_BUFFERING :
				stateDescription = STATE_BUFFERING_DESC;
				break;
			case STATE_INITIALIZED :
				stateDescription = STATE_INITIALIZED_DESC;
				break;
			case STATE_PAUSED :
				stateDescription = STATE_PAUSED_DESC;
				break;
			case STATE_PLAYING :
				stateDescription = STATE_PLAYING_DESC;
				break;
			case STATE_STARTING :
				stateDescription = STATE_STARTING_DESC;
				break;
			case STATE_STOPPED :
				stateDescription = STATE_STOPPED_DESC;
				break;
			case STATE_STOPPING :
				stateDescription = STATE_STOPPING_DESC;
				break;
			case STATE_WAITING_FOR_DATA :
				stateDescription = STATE_WAITING_FOR_DATA_DESC;
				break;
			case STATE_WAITING_FOR_QUEUE :
				stateDescription = STATE_WAITING_FOR_QUEUE_DESC;
				break;
		}

		proxy.setProperty("stateDescription", stateDescription);
		if (DBG) {
			Log.d(LCAT, "Audio state changed: " + stateDescription);
		}

		KrollDict data = new KrollDict();
		data.put("state", state);
		data.put("description", stateDescription);
		proxy.fireEvent(EVENT_CHANGE, data);

	}

	public void stop()
	{
		try {
			if (mp != null) {

				if (mp.isPlaying() || isPaused()) {
					if (DBG) {
						Log.d(LCAT, "audio is playing, stop()");
					}
					setState(STATE_STOPPING);
					mp.stop();
					setState(STATE_STOPPED);
					//if (remote) {
						stopProgressTimer();
					//}
					try {
						mp.prepare();
					} catch (IOException e) {
						Log.e(LCAT,"Error while preparing audio after stop(). Ignoring.");
					} catch (IllegalStateException e) {
						Log.w(LCAT, "Error while preparing audio after stop(). Ignoring.");
					}
				}

				if(isPaused()) {
					paused = false;
				}
			}
		} catch (Throwable t) {
			Log.e(LCAT, "Error : " , t);
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp)
	{
		proxy.fireEvent(EVENT_COMPLETE, null);
		stop();
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra)
	{
		String msg = "OnInfo Unknown media issue.";

		switch(what) {
			case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING :
				msg = "Stream not interleaved or interleaved improperly.";
				break;
			case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE :
				msg = "Stream does not support seeking";
				break;
			case MediaPlayer.MEDIA_INFO_UNKNOWN :
				msg = "Unknown media issue";
				break;
			case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING :
				msg = "Video is too complex for decoder, video lagging."; // shouldn't occur, but covering bases.
				break;
			case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
				msg = "Video metadata update.";
				break;
		}
		Log.d(LCAT, "Error " + msg);

		KrollDict data = new KrollDict();
		data.put(TiC.PROPERTY_CODE, 0);
		data.put(TiC.PROPERTY_MESSAGE, msg);
		proxy.fireEvent(EVENT_ERROR, data);

		return true;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra)
	{
		int code = 0;
		String msg = "Unknown media error.";
		if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
			msg = "Media server died";
		}
		release();

		KrollDict data = new KrollDict();
		data.put(TiC.PROPERTY_CODE, code);
		data.put(TiC.PROPERTY_MESSAGE, msg);
		proxy.fireEvent(EVENT_ERROR, data);

		return true;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent)
	{
		if (DBG) {
			Log.d(LCAT, "Buffering: " + percent + "%");
		}
		
		KrollDict data = new KrollDict();
		data.put("percent", percent);
		proxy.fireEvent(EVENT_BUFFERING, data);
	}

	public int getCurrentPosition() {
		if (mp != null) {
			return mp.getCurrentPosition();
		} else {
			return 0;
		}
	}
	
	
	private void startProgressTimer()
	{
		if (progressTimer == null) {
			progressTimer = new Timer(true);
		} else {
			progressTimer.cancel();
			progressTimer = new Timer(true);
		}

		progressTimer.schedule(new TimerTask()
		{
			@Override
			public void run() {
				if (mp != null && mp.isPlaying()) {
					int position = mp.getCurrentPosition();
					if (DBG) {
						Log.d(LCAT, "Progress: " + position);
					}
					KrollDict event = new KrollDict();
					event.put("progress", position);
					proxy.fireEvent(EVENT_PROGRESS, event);
				}
			}
		}, 1000, 1000);
	}

	private void stopProgressTimer()
	{
		if (progressTimer != null) {
			progressTimer.cancel();
			progressTimer = null;
		}
	}

	public void onDestroy()
	{
		if (mp != null) {
			mp.release();
			mp = null;
		}
		// TitaniumMedia clears out the references after onDestroy.
	}

	public void onPause()
	{
		if (mp != null) {
			if (isPlaying()) {
				pause();
				playOnResume = true;
			}
		}
	}

	public void onResume()
	{
		if (mp != null) {
			if (playOnResume) {
				play();
				playOnResume = false;
			}
		}
	}

	@Override
	public void listenerAdded(String type, int count, KrollProxy proxy) { }

	@Override
	public void listenerRemoved(String type, int count, KrollProxy proxy) { }

	@Override
	public void processProperties(KrollDict d)
	{
		if (d.containsKey(PROPERTY_VOLUME)) {
			setVolume(TiConvert.toFloat(d, PROPERTY_VOLUME));
		} else {
			setVolume(0.5f);
		}

		if (d.containsKey(TiC.PROPERTY_TIME)) {
			setTime(TiConvert.toInt(d, TiC.PROPERTY_TIME));
		}
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		if (PROPERTY_VOLUME.equals(key)) {
			setVolume(TiConvert.toFloat(newValue));
		} else if (TiC.PROPERTY_TIME.equals(key)) {
			setTime(TiConvert.toInt(newValue));
		}
	}

	@Override
	public void propertiesChanged(List<KrollPropertyChange> changes, KrollProxy proxy)
	{
		for (KrollPropertyChange change : changes) {
			propertyChanged(change.getName(), change.getOldValue(), change.getNewValue(), proxy);
		}
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		Log.d(LCAT, "In onPrepared");
		// TODO Auto-generated method stub
		//this.setState(STATE_INITIALIZED);
		//startPlay();
	}	
}