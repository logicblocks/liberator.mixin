(ns liberator-mixin.core
  (:require
    [clojure.string :as str]

    [liberator.core :as liberator]
    [liberator.util :as liberator-util]))

(defn is-decision? [k]
  (str/ends-with? (name k) "?"))

(defn is-action? [k]
  (or (= k :initialize-context) (str/ends-with? (name k) "!")))

(defn is-handler? [k]
  (str/starts-with? (name k) "handle"))

(defn is-configuration? [k]
  (let [n (name k)]
    (or
      (= k :patch-content-types)
      (and (not (is-decision? k))
        (or
          (str/starts-with? n "available")
          (str/starts-with? n "allowed")
          (str/starts-with? n "known"))))))

(defn merge-decisions [left right]
  (fn [context]
    (letfn [(if-vector? [thing f]
              (if (vector? thing) (f thing) thing))
            (execute-and-update [[result context] f]
              (let [decision (f context)
                    result (and result (if-vector? decision first))
                    context-update (if-vector? decision second)
                    context (liberator/update-context context context-update)]
                [result context]))]
      (-> [true context]
        (execute-and-update (liberator-util/make-function left))
        (execute-and-update (liberator-util/make-function right))))))

(defn merge-actions [left right]
  (fn [context]
    (letfn [(execute-and-update [context f]
              (liberator/update-context context (f context)))]
      (-> context
        (execute-and-update (liberator-util/make-function left))
        (execute-and-update (liberator-util/make-function right))))))

(defn merge-handlers [left right]
  ; TODO: Can we do better than this
  right)

(defn merge-configurations [left right]
  (fn merged
    ([] (merged {}))
    ([context]
     (liberator-util/combine
       ((liberator-util/make-function left) context)
       ((liberator-util/make-function right) context)))))

(defn merge-resource-definitions
  [& maps]
  (let [definition-pieces (mapcat vec maps)]
    (reduce
      (fn [result [k override]]
        (if-let [current (get result k)]
          (assoc result
            k (cond
                (is-decision? k) (merge-decisions current override)
                (is-action? k) (merge-actions current override)
                (is-handler? k) (merge-handlers current override)
                (is-configuration? k) (merge-configurations current override)
                :else override))
          (assoc result k override)))
      {}
      definition-pieces)))

(defn build-resource [& ms-or-seqs]
  (liberator/resource
    (apply merge-resource-definitions (flatten ms-or-seqs))))
