# Curate

## Table of Contents

1. Overview
2. Product Spec
3. Wireframes
4. Schema


## Overview

Description

Curate is a music player app powered by the Spotify API that allows users to collaboratively generate a playlist that is ordered by popularity.


## App Evaluation

* *Category:* Music
* *Mobile:*
* *Story:*
* *Market:*  
* *Habit:*
* *Scope:*


## Product Spec

1. User Stories (Required and Optional)

* *Required for the MVP*

   * App plays songs via Spotify API calls
   * Admin can...
    * Create/delete queue
    * Add/delete music
    * Reorder music
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


* *Optional Stretch Stories*

   * Clients can...
    * Join queue with geofencing and/or QR code
    * Save music to Spotify library
    * Use the app and add music without logging in with Spotify
    * Share music through other applications
    * Listen/contribute to a queue remotely
   * App recommends music added in previous queues
   * App orders queue based on additional criteria (e.g. time in queue)
   * Admin can...
    * Set a default playlist
    * Set a music blacklist (e.g. all explicit music)
   * Push notifications encourage users to use the app, add/like music


2. Screen Archetypes

* Login
* Create page
* Main
      * Queue fragment
      * Search fragment

3. Navigation

* Login --> Create OR Main screen with Queue fragment
* Create --> Main screen with Queue fragment
* Main --> Search fragment (when Search bar selected)
* Main --> Queue fragment (when back arrow pressed)

## Wireframes

![Image of Wireframe](./wireframe.png)


## Schema

[This section will be completed in Unit 9]

## Models

[Add table of models]

## Networking

* [Add list of network requests by screen ]
* [Create basic snippets for each Parse network request]
* [OPTIONAL: List endpoints if using existing API such as Yelp]

## Video Walkthrough

Here's a walkthrough of implemented user stories:

<img src='demo.gif' title='Video Walkthrough' width='' alt='Video Walkthrough' />

GIF created with [LiceCap](http://www.cockos.com/licecap/).

## Credits

List an 3rd party libraries, icons, graphics, or other assets you used in your app.

- Butterknife
- Spotify


## License

    Copyright 2019 chiario

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

