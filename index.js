'use strict';

var ReactNative = require('react-native');
var TVCast = ReactNative.NativeModules.TVCast;
var IsAndroid = TVCast.IsAndroid;
var IsWindows = TVCast.IsWindows;
var resolveAssetSource = require("react-native/Libraries/Image/resolveAssetSource");
var eventEmitter = new ReactNative.NativeEventEmitter(TVCast);

var nextKey = 0;

function isRelativePath(path) {
  return !/^(\/|http(s?)|asset|file)/.test(path);
}

function calculateRelativeVolume(volume, pan) {
  // calculates a lower volume relative to the pan value
  const relativeVolume = (volume * (1 - Math.abs(pan)));
  return Number(relativeVolume.toFixed(1));
}

function setAndroidVolumes(sound) {
  // calculates the volumes for left and right channels
  if (sound._pan) {
    const relativeVolume = calculateRelativeVolume(sound._volume, sound._pan);
    if (sound._pan < 0) {
      // left is louder
      TVCast.setVolume(sound._key, sound._volume, relativeVolume);
    } else {
      // right is louder
      TVCast.setVolume(sound._key, relativeVolume, sound._volume);
    }
  } else {
    // no panning, same volume on both channels
    TVCast.setVolume(sound._key, sound._volume, sound._volume);
  }
}

function TVCast(filename, basePath, onError, options) {
  var asset = resolveAssetSource(filename);
  if (asset) {
    this._filename = asset.uri;
    onError = basePath;
  } else {
    this._filename = basePath ? basePath + '/' + filename : filename;

    if (IsAndroid && !basePath && isRelativePath(filename)) {
      this._filename = filename.toLowerCase().replace(/\.[^.]+$/, '');
    }
  }

  this.registerOnPlay = function() {
    if (this.onPlaySubscription != null) {
      console.warn('On Play change event listener is already registered');
      return;
    }

    if (!IsWindows) {
      this.onPlaySubscription = eventEmitter.addListener(
        'onPlayChange',
        (param) => {
          const { isPlaying, playerKey } = param;
          if (playerKey === this._key) {
            if (isPlaying) {
              this._playing = true;
            }
            else {
              this._playing = false;
            }
          }
        },
      );
    }
  }

  this._loaded = false;
  this._key = nextKey++;
  this._playing = false;
  this._duration = -1;
  this._numberOfChannels = -1;
  this._volume = 1;
  this._pan = 0;
  this._numberOfLoops = 0;
  this._speed = 1;
  this._pitch = 1;
  TVCast.prepare(this._filename, this._key, options || {}, (error, props) => {
    if (props) {
      if (typeof props.duration === 'number') {
        this._duration = props.duration;
      }
      if (typeof props.numberOfChannels === 'number') {
        this._numberOfChannels = props.numberOfChannels;
      }
    }
    if (error === null) {
      this._loaded = true;
      this.registerOnPlay();
    }
    onError && onError(error, props);
  });
}

TVCast.prototype.isLoaded = function() {
  return this._loaded;
};

TVCast.prototype.play = function(onEnd) {
  if (this._loaded) {
    TVCast.play(this._key, (successfully) => onEnd && onEnd(successfully));
  } else {
    onEnd && onEnd(false);
  }
  return this;
};

TVCast.prototype.pause = function(callback) {
  if (this._loaded) {
    TVCast.pause(this._key, () => {
      this._playing = false;
      callback && callback();
    });
  }
  return this;
};

TVCast.prototype.stop = function(callback) {
  if (this._loaded) {
    TVCast.stop(this._key, () => {
      this._playing = false;
      callback && callback();
    });
  }
  return this;
};

TVCast.prototype.reset = function() {
  if (this._loaded && IsAndroid) {
    TVCast.reset(this._key);
    this._playing = false;
  }
  return this;
};

TVCast.prototype.release = function() {
  if (this._loaded) {
    TVCast.release(this._key);
    this._loaded = false;
    if (!IsWindows) {
      if (this.onPlaySubscription != null) {
        this.onPlaySubscription.remove();
        this.onPlaySubscription = null;
      }
    }
  }
  return this;
};

TVCast.prototype.getFilename = function() {
  return this._filename;
};

TVCast.prototype.getDuration = function() {
  return this._duration;
};

TVCast.prototype.getNumberOfChannels = function() {
  return this._numberOfChannels;
};

TVCast.prototype.getVolume = function() {
  return this._volume;
};

TVCast.prototype.getSpeed = function() {
  return this._speed;
};

TVCast.prototype.getPitch = function() {
  return this._pitch;
};

TVCast.prototype.setVolume = function(value) {
  this._volume = value;
  if (this._loaded) {
    if (IsAndroid) {
      setAndroidVolumes(this)
    } else {
      TVCast.setVolume(this._key, value);
    }
  }
  return this;
};

TVCast.prototype.setPan = function(value) {
  this._pan = value;
  if (this._loaded) {
    if (IsWindows) {
      throw new Error('#setPan not supported on windows');
    } else if (IsAndroid) {
      setAndroidVolumes(this)
    } else {
      TVCast.setPan(this._key, value);
    }
  }
  return this;
};

TVCast.prototype.getSystemVolume = function(callback) {
  if(!IsWindows) {
    TVCast.getSystemVolume(callback);
  }
  return this;
};

TVCast.prototype.setSystemVolume = function(value) {
  if (IsAndroid) {
    TVCast.setSystemVolume(value);
  }
  return this;
};

TVCast.prototype.getPan = function() {
  return this._pan;
};

TVCast.prototype.getNumberOfLoops = function() {
  return this._numberOfLoops;
};

TVCast.prototype.setNumberOfLoops = function(value) {
  this._numberOfLoops = value;
  if (this._loaded) {
    if (IsAndroid || IsWindows) {
      TVCast.setLooping(this._key, !!value);
    } else {
      TVCast.setNumberOfLoops(this._key, value);
    }
  }
  return this;
};

TVCast.prototype.setSpeed = function(value) {
  this._speed = value;
  if (this._loaded) {
    if (!IsWindows) {
      TVCast.setSpeed(this._key, value);
    }
  }
  return this;
};

TVCast.prototype.setPitch = function(value) {
  this._pitch = value;
  if (this._loaded) {
    if (IsAndroid) {
      TVCast.setPitch(this._key, value);
    }
  }
  return this;
};

TVCast.prototype.getCurrentTime = function(callback) {
  if (this._loaded) {
    TVCast.getCurrentTime(this._key, callback);
  }
};

TVCast.prototype.setCurrentTime = function(value) {
  if (this._loaded) {
    TVCast.setCurrentTime(this._key, value);
  }
  return this;
};

// android only
TVCast.prototype.setSpeakerphoneOn = function(value) {
  if (IsAndroid) {
    TVCast.setSpeakerphoneOn(this._key, value);
  }
};

// ios only

// This is deprecated.  Call the static one instead.

TVCast.prototype.setCategory = function(value) {
  TVCast.setCategory(value, false);
}

TVCast.prototype.isPlaying = function() {
  return this._playing;
}

TVCast.enable = function(enabled) {
};

TVCast.enableInSilenceMode = function(enabled) {
  
};

TVCast.setActive = function(value) {
  
};

TVCast.setCategory = function(value, mixWithOthers = false) {
  
};

TVCast.setMode = function(value) {
  
};

TVCast.setSpeakerPhone = function(value) {

}

TVCast.MAIN_BUNDLE = TVCast.MainBundlePath;
TVCast.DOCUMENT = TVCast.NSDocumentDirectory;
TVCast.LIBRARY = TVCast.NSLibraryDirectory;
TVCast.CACHES = TVCast.NSCachesDirectory;

module.exports = TVCast;
