Group Project - README Template

**curate**

Table of Contents

1. Overview
2. Product Spec
3. Wireframes
4. Schema


Overview

Curate a music list

Description

An app that allows users to collaboratively construct a playlist for an event.


App Evaluation

* *Category:* Music
* *Mobile:*
* *Story:*
* *Market:*  
* *Habit:*
* *Scope:*


Product Spec

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

Wireframes

![Image of Wireframe](./wireframe.png)


Schema

[This section will be completed in Unit 9]

Models

[Add table of models]

Networking

* [Add list of network requests by screen ]
* [Create basic snippets for each Parse network request]
* [OPTIONAL: List endpoints if using existing API such as Yelp]
