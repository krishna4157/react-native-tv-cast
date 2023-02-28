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
  TVCast.execute("resetDiscovery",JSON.stringify([]),(e)=>{
                   
  },(e) => {

      // console.log("CHECK ERROR : ",e);
  });
}

TVCast.stopCast =  function() {
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

  TVCast.setDevice = function(deviceId,obj,func){
    TVCast.setSelectedDevice(deviceId, obj,func);
  }






module.exports = TVCast;
