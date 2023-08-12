# react-native-tv-cast

![](gif)

react-native-tv-cast is a plugin to cast content from android phone to any smart tv that is connected to internet with ease, thus increasing flexibility to use any where in the component.

## Getting started

`$ npm install react-native-tv-cast --save`

`$ yarn add react-native-tv-cast`

To use this dependency in your project, the following packages are needed. 
1. 'react'
2. 'react-native'

### Mostly automatic installation

## Modify app/build.gradle
```java
  implementation ( 'com.github.ConnectSDK:Connect-SDK-Android:2.0.0')
```

## Modify MainApplication.java

  


```java
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.service.DeviceService;

  @Override
  public void onCreate() {
    super.onCreate();
    DiscoveryManager.init(getApplicationContext());
    try {
      DiscoveryManager.getInstance().registerDeviceService((Class<DeviceService>) Class.forName("com.connectsdk.service.CastService"), (Class<DiscoveryProvider>)Class.forName("com.connectsdk.discovery.provider.CastDiscoveryProvider"));
    } catch (ClassNotFoundException e) {
      Log.d("CHROME","CHROME CAST NOT REGISTERED");
      e.printStackTrace();
    }
    ...
  }
```
## Important Note
  Amazon FireTv Casting works only when targetSdk is set to 29 or below.
  
  To get the devices list. you need to add listeners
  
  'deviceList' for list of devices 
  
  'duration' for duration


## Usage (Example of react-native-tv-cast )
```javascript



import React, {Component} from 'react';
import {View} from 'react-native';
import TVCast from 'react-native-tv-cast';
/*
    npm install react-native-tv-cast
*/

export default class App extends Component{
    constructor(props){
        super();
    }
    render(){
        return (
       <View>
            
        </View>
        );
    }
}


// TODO: What to do with the module?
TVCast;
```

# Methods :

```javascript

resetDiscovery()
// resets the discovery

stopCast()
// stops the current casting process

setDevice(deviceId: DeviceId, StreamData: streamData, callback: CallbackType)
// Sets the deviceId where we need to cast.
// where streamData will be 
// var streamData = {
//  url: string;
//  title: string;
//  description: string;
//  icon: string;
// };

```
# issues
Alternative you can raise issues here https://github.com/krishna4157/react-native-tv-cast/issues

# Credit goes to : 

1. 'react'
2. 'react-native'
3. 'connect-sdk' 

# Contribution :

if you like my work 😀 u can contribute using
vpa : krishna.santho08-1@okaxis

it will be a great support to me ☺.


