# Curate

## Description

Curate is a music player app powered by the Spotify API that allows users to collaboratively generate a playlist that is ordered by popularity.


## Product Spec

1. User Stories

* *Required for the MVP*

   * App plays songs via the device's Spotify app
   * Admin can...
    * Create/delete queue
    * Add/delete music
    * Play/pause music
    * View queue
    * Search music


* *Additional Stories for the Target product*

   * Clients can...
    * Join queue with a code
    * View queue
    * Add music
    * Like music
    * Search music
   * Admin can...
    * Skip songs
   * App orders queue based on number of likes


* *Stretch Stories*

   * Clients can...
    * Join queue with geofencing and QR code
    * Use the app and add music without logging in with Spotify
    * Share music through other applications
   * Next song in playlist starts automatically even with app in background
   * Push notifications encourage users to use the app, add/like music
   * Users' playlists update live with a pub/sub model
   * Search is sped up by implementing LRU cache for both search queries and songs


2. Screen Archetypes

* Join page
  * Select fragment (Join/Create buttons)
  * Join party fragment
* Main
  * Queue fragment
  * Search fragment
  * Party info dialog fragment
  * Settings dialog fragment

## Video Walkthrough

Here's a walkthrough of implemented user stories:

<img src='demo.gif' title='Video Walkthrough' width='' alt='Video Walkthrough' />

GIF created with [LiceCap](http://www.cockos.com/licecap/).

## Credits

List an 3rd party libraries, icons, graphics, or other assets you used in your app.

- Butterknife
- Spotify


## License

    Copyright 2019 Caio Costa, Chiara Darnton, and Mario Ruiz

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

