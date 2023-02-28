// Type definitions for react-native-sound
// Project: https://github.com/krishna4157/react-native-tv-cast
// Definitions by: Krishna4157
// TypeScript Version: 2.3.2

// deviceID
type DeviceId = string;

//StreamData
type StreamData = {
  url: string;
  title: string;
  description: string;
  icon: string;
};

type CallbackType = (error: any) => void;

declare class TVCast {
  /**
   * Reset Discovery of Smart TV Devices List.
   * Use this method to reset the discovery process.
   *
   */
  static resetDiscovery(): void;

  /**
   * Stops Casting on Connected TV.
   * Use this method to stop the casting process.
   *
   */
  static stopCast(): void;

  /**
   * Sets Selected Device For Casting Process.
   *
   * @param deviceId DeviceId String
   * @param StreamData StreamData
   *  {
   *  "url" : '',
   *  "title" : '',
   *  "description" : '',
   *  "icon" : ''
   * }
   * @param callback Can be set to retrieve the response of output values.
   *
   */
  static setDevice(
    deviceId: DeviceId,
    StreamData: StreamData,
    callback: CallbackType
  ): void;

  /**
   * Return true if the sound has been loaded.
   */
  isLoaded(): boolean;

  /**
   * Whether the player is playing or not.
   */
  isPlaying(): boolean;
}

export = TVCast;
