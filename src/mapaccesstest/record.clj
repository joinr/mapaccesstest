(ns mapaccesstest.record
  (:require [spork.util.general]
            [clojure.walk :as walk]))

(defmacro fastrecord
  "Like defrecord, but adds default map-like function application
   semantics to the record.  Fields are checked first in O(1) time,
   then general map lookup is performed.  Users may supply and optional
   ^:static hint for the arg vector, which will enforce the invariant
   that the record always and only has the pre-defined fields, and
   will throw an exception on any operation that tries to access
   fields outside of the predefined static fields.  This moves
   the record into more of a struct-like object.

   Note: this is not a full re-immplementation of defrecord,
   and still leverages the original's code emission.  The main
   difference is the implementation of key-lookup semantics
   ala maps-as-functions, and drop-in performance that should
   be equal-to or superior to the clojure.core/defrecord
   implementation.  Another refinement that makes arraymaps
   superior for fields <= 8, is the avoidance of a case dispatch
   which is slower in practice than a linear scan or a
   sequential evaluation of if identical? expressions.
   Small records defined in this way should be competitive
   in general purpose map operations."
  [name keys & impls]
  (let [fields (map keyword keys)
        binds  (reduce (fn [acc [l r]]
                     (conj acc l r))
                   []
                   (map vector fields (map #(with-meta % {})  keys)))
        [_ name keys & impls] &form
        this (gensym "this")
        k    (gensym "k")
        extmap (with-meta '__extmap {:tag 'clojure.lang.ILookup})
        default (gensym "default")
        n       (count keys)
        caser   'spork.util.general/fast-case
        lookup (fn [method]
                 `[(~method [~this ~k]
                    (~caser ~k
                     ~@binds
                     ~(if (-> keys meta :strict)
                        `(throw (ex-info "key not in strict record" {:key ~k}))
                        `(if ~extmap
                           (~'.valAt ~extmap ~k)))))
                   (~method [~this ~k ~default]
                    (~caser ~k
                     ~@binds
                     ~(if (-> keys meta :strict)
                        `(throw (ex-info "key not in strict record" {:key ~k}))
                        `(if ~extmap
                           (~'.valAt ~extmap ~k ~default)))))])
        replace-val-at (fn [impls]
                         (->> impls
                              (remove (fn [impl]
                                        (and (seq impl)
                                             (#{'valAt  'clojure.core/valAt}
                                              (first impl)))))
                              (concat (lookup 'valAt))))
        replace-deftype (fn [emitted]
                          (->> emitted
                               (reduce (fn [acc x]
                                         (if (and (seq? x)
                                                  (= (first x) 'deftype*))
                                           (let [init (take 6 x)
                                                 impls (drop 6 x)]
                                             (conj acc (concat init
                                                               (replace-val-at impls))))
                                           (conj acc x))) [])
                               seq))
        rform (->> `(~'defrecord ~name ~keys ~@impls
                     ~'clojure.lang.IFn
                     ~@(lookup 'invoke))
                   macroexpand-1
                   replace-deftype
                   (walk/postwalk-replace {'clojure.core/case caser
                                           'case caser}))]
    `(~@rform)))
