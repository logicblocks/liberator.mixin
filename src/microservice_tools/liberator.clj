(ns microservice-tools.liberator
  (:require
    [clojure.string :as str]
    [liberator.core :as liberator]
    [liberator.util
     :refer [make-function]]))

(defn is-decision? [k]
  (str/ends-with? (name k) "?"))

(defn is-action? [k]
  (or (= k :initialize-context) (str/ends-with? (name k) "!")))

(defn is-handler? [k]
  (str/starts-with? (name k) "handle"))

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
             (execute-and-update (make-function left))
             (execute-and-update (make-function right))))))

(defn merge-actions [left right]
  (fn [context]
    (letfn [(execute-and-update [context f]
              (liberator/update-context context (f context)))]
      (-> context
        (execute-and-update (make-function left))
        (execute-and-update (make-function right))))))

(defn merge-handlers [left right]
  ; TODO: Can we do better than this
  right)

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
                :else override))
          (assoc result k override)))
      {}
      definition-pieces)))

(defn build-resource [& ms]
  (liberator/resource
    (apply merge-resource-definitions ms)))
