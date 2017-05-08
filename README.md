# Instadump: Semi-automatic disk serialization of Clojure refs

## Summary

While large-scale, mission-critical data that needs high-performance
access warrants the use of dedicated database systems, it is sometimes
useful to have a way of simply dumping the current in-memory state of
an application to disk without sacrificing reliability and data
integrity. Fortunately, Clojure's STM architecture is well-suited to
the task; the only thing it lacks is some way of safely serializing
data to disk.

This is where Instadump comes in. You tell it where to put stuff and
when to save the current state, and it will manage the rest. No need
to explicitly reload the application state the next time your
application starts up—Instadump does that automatically (but if you
need to, you can explicitly revert to the last saved state at any
time).

Data is stored on the disk using [Oracle Berkeley DB Java Edition][].

## Installation

Instadump is available from [Clojars][].

### Leiningen

```clojure
[instadump "0.0.2"]
```

### Maven

```xml
<dependency>
  <groupId>instadump</groupId>
  <artifactId>instadump</artifactId>
  <version>0.0.2</version>
</dependency>
```

## Usage

### Examples

```clojure
(use 'eu.mulk.instadump)

(setup-instadump! "db")  ;tell Instadump where to put the dump files

(defstate testvar1 150)
(defstate testvar2 "abc")

@testvar1  ;=> 150
@testvar2  ;=> "abc"

(dosync (ref-set testvar1 1000))
(save-all-global-state!)

;; ------
;; Restart the Clojure process...
;; ------

;; Then:

(use 'eu.mulk.instadump)

(setup-instadump! "db")

(defstate testvar1 150)
(defstate testvar2 "abc")

@testvar1  ;=> 1000
@testvar2  ;=> "abc"
```

### API

```clojure
; -------------------------
; eu.mulk.instadump/defstate
; ([sym default])
; Macro
;   Define a global ref managed by Instadump.  The supplied default
;   value is used if the variable cannot be found in the database.
;   Otherwise, the value stored in the database is used.
; 
; -------------------------
; eu.mulk.instadump/setup-instadump!
; ([dirname])
;   Set up the Instadump database at the file system location indicated
;   by dirname.
; 
; -------------------------
; eu.mulk.instadump/save-all-global-state!
; ([])
;   Direct Instadump to dump a snapshot of all variables created by
;   defstate into the database.  save-all-global-state! runs in an
;   implicit transaction in order to ensure data consistency.
; 
; -------------------------
; eu.mulk.instadump/reload-all-global-state!
; ([])
;   Direct Instadump to revert all variables created by defstate to the
;   state saved by the last invocation of save-all-global-state!.  Will
;   fail if any variables cannot be restored (e.g. if some variables
;   have never been saved before).
```


[Oracle Berkeley DB Java Edition]: http://www.oracle.com/us/products/database/berkeley-db/je/overview/index.html
[Clojars]:                         https://clojars.org/instadump
