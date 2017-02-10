// This is a test harness for your module
// You should do something interesting in this harness
// to test out the module and to provide instructions
// to users on how to use it by example.


// open a single window
var win = Ti.UI.createWindow({
	backgroundColor:'white',
	layout: 'vertical'
});

var label = Ti.UI.createLabel();
var progressLabel = Ti.UI.createLabel();
var durationLabel = Ti.UI.createLabel();
var bufferingLabel = Ti.UI.createLabel();
var stateLabel = Ti.UI.createLabel();
var completeLabel = Ti.UI.createLabel({text: 'Not Complete'});
window.add(label);
window.add(durationLabel);
window.add(stateLabel);
window.add(progressLabel);
window.add(bufferingLabel);
window.add(completeLabel);
window.open();

// TODO: write your module tests here
var advaudioplayer = require('com.kcwdev.audio');
Ti.API.info("module is => " + advaudioplayer);

if (Ti.Platform.name == "android") {
    var audioUrl = "http://www.stephaniequinn.com/Music/Canon.mp3";
    //var audioUrl = "http://yp.shoutcast.com/sbin/tunein-station.pls?id=1966819";
    //var audioUrl = "http://dir.xiph.org/listen/714343/listen.m3u";
    //var audioUrl = "http://dir.xiph.org/listen/714343/listen.xspf";
    var audioUrl2 = "";
	var player = advaudioplayer.createAudioPlayer({
		url: audioUrl,
		speakerphone: true
	});

    player.addEventListener('change', function(e) { stateLabel.text = 'State: ' + e.state + ' ' + e.description; });
    player.addEventListener('progress', function(e) { progressLabel.text = 'Progress: ' + e.progress; });
    player.addEventListener('complete', function(e) { 
        completeLabel.text = 'Complete';
        player.setUrl(audioUrl2);
        durationLabel.text = 'Duration: ' + player.getDuration();
        player.seek(45000);
        player.play();
    } );
    
    player.addEventListener('buffering', function(e) { bufferingLabel.text = 'Buffering Percent:' + e.percent });

	player.play();
    durationLabel.text = 'Duration: ' + player.getDuration();

}