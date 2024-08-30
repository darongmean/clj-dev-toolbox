# darongmean/dev-clj

A collection of functions for Clojure REPL workflow.

The functions are for automating common development tasks (for example: starting a local web server, running a database
query, turning on/off email sending, etc.)

## Usage

```clj
;;; file user.clj

(ns user)

(require 'dev)

;;; file other.clj

(def User [:map [:first-name :string]])

(comment
 ;; use in comment section to execute in repl
 (dev/exercise User)
 ;=> 
 ;({:first-name ""}
 ; {:first-name ""}
 ; {:first-name ""}
 ; {:first-name "c"}
 ; {:first-name ""}
 ; {:first-name "1535y"}
 ; {:first-name ""}
 ; {:first-name "Rz"}
 ; {:first-name "dTg0j3kF"}
 ; {:first-name ""})
 )
```

## License

Copyright © 2024 Darong MEAN

Distributed under the Eclipse Public License version 1.0.
