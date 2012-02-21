(defproject instadump "0.0.1"
  :description "Semi-automatic serialization for Clojure vars"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [com.sleepycat/je "4.1.10"]  ;4.0.96 if the Oracle Maven repo scares you
                 ]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]
                     [clj-stacktrace "0.2.3"]]
  :repositories {"oracle"
                 {:url "http://download.oracle.com/maven/"
                  :snapshots false}}
  :source-path "src")

