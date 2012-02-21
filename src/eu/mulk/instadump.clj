;; Copyright 2012, Matthias Andreas Benkard.
;; See file COPYING for license details.

(ns eu.mulk.instadump
  (:refer-clojure)
  (:use clojure.java.io)
  (:import [com.sleepycat.je
            Database DatabaseConfig DatabaseEntry Environment EnvironmentConfig
            OperationStatus]))


;;;; * Internal stuff
(def ^{:dynamic true :private true} *txn*)
(def ^{:dynamic true :private true} *db*)

(declare ^{:private true} dbenv)


(defn- make-db-env [db-directory-name]
  (let [envconfig (doto (EnvironmentConfig.)
                    (.setTransactional            true)
                    (.setAllowCreate              true)
                    (.setTxnSerializableIsolation true))]
    (Environment. (file db-directory-name) envconfig)))

(defn- call-with-transaction [thunk]
  (binding [*txn* (.beginTransaction @dbenv nil nil)]
    (try (let [result (thunk)]
           (.commit *txn*)
           result)
      (finally (.abort *txn*)))))

(defn call-with-db [thunk]
  (let [dbconfig (doto (DatabaseConfig.)
                   (.setTransactional            true)
                   (.setAllowCreate              true)
                   (.setSortedDuplicates         false))]
    (call-with-transaction
      (fn []
        (binding [*db* (.openDatabase @dbenv *txn* "benki" dbconfig)]
          (thunk))))))

(defmacro with-db [& body]
  `(call-with-db (fn [] ~@body)))

(defn- dump-str-for-db [x]
  (binding [*print-dup*  true
            *print-meta* true]
    (pr-str x)))

(defn getkey [key default]
  (let [entry (DatabaseEntry.)]
    (if (= (.get *db* *txn* (DatabaseEntry. (.getBytes key)) entry nil)
           OperationStatus/SUCCESS)
      (-> entry
          (.getData)
          (String.)
          (read-string))
      default)))

(defn- putkey [key val]
  (let [bdbkey key]
    (.put *db*
          *txn*
          (DatabaseEntry. (.getBytes bdbkey))
          (DatabaseEntry. (.getBytes (dump-str-for-db val))))))


(defonce ^{:private true} dbenv (atom nil))
(defonce ^{:private false} state-vars (atom {}))


;;;; * Public API
(defn setup-instadump!
  "Set up the Instadump database at the file system location indicated
  by dirname."
  [dirname]
  (reset! dbenv (make-db-env dirname)))

(defmacro defstate
  "Define a global ref managed by Instadump.  The supplied default
  value is used if the variable cannot be found in the database.
  Otherwise, the value stored in the database is used."
  [sym default]
  (let [dbkey (str *ns* "/" (name sym))]
    `(defonce ~sym
       (let [r# (ref (with-db (getkey ~dbkey ~default)))]
         (swap! state-vars #(assoc % ~dbkey r#))
         r#))))


(defn save-all-global-state!
  "Direct Instadump to dump a snapshot of all variables created by
  defstate into the database.  save-all-global-state! runs in an
  implicit transaction in order to ensure data consistency."
  []
  (with-db
    (dosync
      (doseq [[key r] @state-vars]
        (putkey key (ensure r))))))

(defn reload-all-global-state!
  "Direct Instadump to revert all variables created by defstate to the
  state saved by the last invocation of save-all-global-state!.  Will
  fail if any variables cannot be restored (e.g. if some variables
  have never been saved before)."
  []
  (with-db
    (dosync
      (doseq [[key r] @state-vars]
        (alter r #(getkey key %))))))

(comment
  (setup-instadump! "db")
  (defstate testvar1 150)
  (defstate testvar2 "abc"))
