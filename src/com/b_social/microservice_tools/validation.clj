(ns com.b-social.microservice-tools.validation
  (:require
    [clojure.spec.alpha :as spec]
    [camel-snake-kebab.core :refer [->camelCaseString]]))

(defprotocol Validator
  (valid? [_ m])
  (problems-for [_ m]))

(defn problem-calculator-for [spec validation-subject]
  (fn [validate-data]
    (let [context (spec/explain-data spec validate-data)]
      (reduce
        (fn [accumulator problem]
          (let [predicate-details (:pred problem)
                [field problem]
                (if (and
                      (seq? predicate-details)
                      (= 'clojure.core/contains?
                        (get (vec (get (vec predicate-details) 2)) 0)))
                  [(get (vec (get (vec predicate-details) 2)) 2) :missing]
                  [(last (get-in problem [:path])) :invalid])]
            (conj accumulator
              {:type    :validation-failure
               :subject validation-subject
               :field   (if field
                          [(->camelCaseString field)]
                          [(->camelCaseString predicate-details)])
               :problem problem})))
        []
        (::spec/problems context)))))