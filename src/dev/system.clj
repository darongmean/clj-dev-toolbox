(ns dev.system
  (:require
   [mount.core :as mount]))


(defonce started-states
  (atom #{}))


(defn start [& states]
  (swap! started-states #(apply conj % states))
  (apply mount/start states))


(defn stop [& states]
  (swap! started-states #(apply disj % states))
  (apply mount/stop states))


(defn restart []
  (let [states @started-states]
    (apply stop states)
    (apply start states)))
