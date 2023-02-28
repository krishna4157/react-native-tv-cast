'use strict';

var ReactNative = require('react-native');
var TVCast = ReactNative.NativeModules.TVCast;
var resolveAssetSource = require("react-native/Libraries/Image/resolveAssetSource");
var eventEmitter = new ReactNative.NativeEventEmitter(TVCast);

var nextKey = 0;

function isRelativePath(path) {
  return !/^(\/|http(s?)|asset|file)/.test(path);
}


TVCast.resetDiscovery = function() {
  // calculates the volumes for left and right channels
  TVCast.execute("resetDiscovery",JSON.stringify([]),(e)=>{});

}

TVCast.stopCasting =  function() {
  TVCast.stopCasting();

      // this.onPlaySubscription = eventEmitter.addListener(
      //   'onPlayChange',
      //   (param) => {
      //     const { isPlaying, playerKey } = param;
      //     if (playerKey === this._key) {
      //       if (isPlaying) {
      //         this._playing = true;
      //       }
      //       else {
      //         this._playing = false;
      //       }
      //     }
      //   },
      // );
    
  }

  TVCast.setSelectedDevice = function(deviceId,obj,func){
    TVCast.setSelectedDevice(deviceId, obj,func);
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
 

TVCast.prototype.isLoaded = function() {
  return this._loaded;
};


TVCast.prototype.stop = function() {
  stopCasting();
};




module.exports = TVCast;
