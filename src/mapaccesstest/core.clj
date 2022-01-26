(ns mapaccesstest.core
  (:require [mapaccesstest.record :as r]
            [criterium.core :as c]))

(let [opts {:a :foo}] (c/quick-bench (:a opts)))
;;Evaluation count : 63058308 in 6 samples of 10509718 calls.
;;             Execution time mean : 7.798485 ns

(let [opts {:a :foo}] (c/quick-bench (opts :a)))
;;Evaluation count : 86278110 in 6 samples of 14379685 calls.
;;             Execution time mean : 5.187680 ns

(let [opts (java.util.HashMap. {:a 1})] (c/quick-bench (.get ^java.util.Map opts :a)))
;;Evaluation count : 67981740 in 6 samples of 11330290 calls.
;;             Execution time mean : 6.905192 ns

(let [opts (java.util.HashMap. {:a 1})] (c/quick-bench (.get ^java.util.Map opts :a)))
;;Evaluation count : 70373034 in 6 samples of 11728839 calls.
;;             Execution time mean : 6.857820 ns

(defrecord Foo [a])

(let [opts (->Foo 1)] (c/quick-bench (:a opts)))
;;Evaluation count : 93603156 in 6 samples of 15600526 calls.
;;Execution time mean : 4.542764 ns

(let [opts (->Foo 1)] (c/quick-bench (.a ^Foo opts)))
;;Evaluation count : 119118888 in 6 samples of 19853148 calls.
;;Execution time mean : 3.535142 ns

(r/fastrecord FooFast [a])

(let [opts (->FooFast 1)] (c/quick-bench (:a opts)))
;;Evaluation count : 93603156 in 6 samples of 15600526 calls.
;;Execution time mean : 4.542764 ns

(let [opts (->FooFast 1)] (c/quick-bench (opts :a)))
;;Evaluation count : 102556866 in 6 samples of 17092811 calls.
;;Execution time mean : 4.012662 ns

(let [opts (into-array [:foo :bar])]
  (c/quick-bench (aget ^objects opts 0)))
;;Evaluation count : 108937308 in 6 samples of 18156218 calls.
;;Execution time mean : 3.812408 ns
