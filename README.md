# Firestone - A Hearthstone clone made in Clojure

This is the backend part of the game. Its built using REST and the client is requesting the game
state and calls the state manipulator functions.


## In order to run the server:
- Run all tests
- Run the function (start!) in src/firestone/server.clj


Then login to the client at https://www.conjoin-it.se/firestone-ajax/ (Credentials username/password -> clojure/clojure)



## Namespaces

### firestone.construct
Contains all the functions for construction of the game state

### firestone.core
Contains all the functions for handling of the game state

### firestone.server
Contains the server code (back end) of the game.