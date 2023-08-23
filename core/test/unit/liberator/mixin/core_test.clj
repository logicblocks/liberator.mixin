(ns liberator.mixin.core-test
  (:require
   [clojure.test :refer :all]

   [liberator.util :as util]

   [liberator.mixin.core :as core]
   [liberator.mixin.test-support :as ts]))

; how should we merge:
; :location -> fn or constant
; :etag -> fn
; :last-modified -> fn
; :as-response -> fn

(deftest type-checks
  (doseq [decision-name (concat ts/decision-names-and ts/decision-names-or)]
    (is (true? (core/is-decision? decision-name)))
    (is (false? (core/is-action? decision-name)))
    (is (false? (core/is-handler? decision-name)))
    (is (false? (core/is-configuration? decision-name))))

  (doseq [handler-name ts/handler-names]
    (is (false? (core/is-decision? handler-name)))
    (is (false? (core/is-action? handler-name)))
    (is (true? (core/is-handler? handler-name)))
    (is (false? (core/is-configuration? handler-name))))

  (doseq [action-name ts/action-names]
    (is (false? (core/is-decision? action-name)))
    (is (true? (core/is-action? action-name)))
    (is (false? (core/is-handler? action-name)))
    (is (false? (core/is-configuration? action-name))))

  (doseq [configuration-name ts/configuration-names]
    (is (false? (core/is-decision? configuration-name)))
    (is (false? (core/is-action? configuration-name)))
    (is (false? (core/is-handler? configuration-name)))
    (is (true? (core/is-configuration? configuration-name)))))

(defn liberator-fn? [f]
  (or (fn? f)
    (instance? clojure.lang.MultiFn f)
    (keyword? f)))

(defn merge-test
  [& {:keys [description
             context
             left-attribute
             right-attribute
             expected-left-context
             expected-right-context
             result
             comparator]}]
  (let [actual-left-context (atom nil)
        actual-right-context (atom nil)
        left-attribute (if (liberator-fn? left-attribute)
                         (fn [ctx]
                           (reset! actual-left-context ctx)
                           ((util/make-function left-attribute) ctx))
                         left-attribute)
        right-attribute (if (liberator-fn? right-attribute)
                          (fn [ctx]
                            (reset! actual-right-context ctx)
                            ((util/make-function right-attribute) ctx))
                          right-attribute)]
    {:description description
     :context     context
     :left        {:attribute left-attribute
                   :context   {:actual   actual-left-context
                               :expected expected-left-context}}
     :right       {:attribute right-attribute
                   :context   {:actual   actual-right-context
                               :expected expected-right-context}}
     :result      result
     :comparator  comparator}))

(def action-tests
  [(merge-test
     :description "combines results when both actions are fns returning maps"
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] {:first [3 4]})
     :right-attribute (fn [_] {:first [5 6] :third "third"})
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [1 2 3 4] :second "second"}
     :result {:first [1 2 3 4 5 6] :second "second" :third "third"})

   (merge-test
     :description "executes left before right when both actions are fns"
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] {:first ^:replace [3 4]})
     :right-attribute (fn [_] {:first ^:replace [5 6] :third "third"})
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [3 4] :second "second"}
     :result {:first [5 6] :second "second" :third "third"})

   (merge-test
     :description (str "takes result from second fn when both actions are "
                    "fns returning fns")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] (fn [] {:first [3 4]}))
     :right-attribute (fn [_] (fn [] {:first [5 6] :third "third"}))
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [3 4]}
     :result {:first [5 6] :third "third"})

   (merge-test
     :description (str "combines results when first fn returns fn and second "
                    "returns map")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] (fn [] {:first [3 4]}))
     :right-attribute (fn [_] {:first [5 6] :third "third"})
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [3 4]}
     :result {:first [3 4 5 6] :third "third"})

   (merge-test
     :description (str "takes result from second fn when first fn returns map "
                    "and second returns fn")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] {:first [3 4]})
     :right-attribute (fn [_] (fn [] {:first [5 6] :third "third"}))
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [1 2 3 4] :second "second"}
     :result {:first [5 6] :third "third"})

   (merge-test
     :description (str "looks up in context when both actions are fns "
                    "returning keywords")
     :context {:first  [1 2]
               :second {:is {:the "second"}}}
     :left-attribute :second
     :right-attribute :is
     :expected-left-context {:first [1 2] :second {:is {:the "second"}}}
     :expected-right-context {:first  [1 2]
                              :second {:is {:the "second"}}
                              :is     {:the "second"}}
     :result {:first  [1 2]
              :second {:is {:the "second"}}
              :is     {:the "second"}
              :the    "second"})

   (merge-test
     :description (str "looks up in context and combines when first action "
                    "is a fn returning a keyword and second action is a fn "
                    "returning a map")
     :context {:first [1 2] :second {:the "second"}}
     :left-attribute :second
     :right-attribute (fn [_] {:third "third"})
     :expected-left-context {:first [1 2] :second {:the "second"}}
     :expected-right-context {:first  [1 2]
                              :second {:the "second"}
                              :the    "second"}
     :result {:first  [1 2]
              :second {:the "second"}
              :the    "second"
              :third  "third"})

   (merge-test
     :description (str "combines and looks up in context when first action "
                    "is a fn returning a map and second action is a keyword")
     :context {:first [1 2] :second {:the "second"}}
     :left-attribute (fn [_] {:third "third"})
     :right-attribute :second
     :expected-left-context {:first [1 2] :second {:the "second"}}
     :expected-right-context {:first  [1 2]
                              :second {:the "second"}
                              :third  "third"}
     :result {:first  [1 2]
              :second {:the "second"}
              :third  "third"
              :the    "second"})

   (merge-test
     :description "combines when left is map and right is map"
     :context {:first [1 2] :second "second"}
     :left-attribute {:first [3 4]}
     :right-attribute {:first [5 6] :third "third"}
     :expected-left-context nil
     :expected-right-context nil
     :result {:first  [1 2 3 4 5 6]
              :second "second"
              :third  "third"})

   (merge-test
     :description "combines when left is map and right is fn returning map"
     :context {:first [1 2] :second "second"}
     :left-attribute {:first [3 4]}
     :right-attribute (fn [_] {:first [5 6] :third "third"})
     :expected-left-context nil
     :expected-right-context {:first [1 2 3 4] :second "second"}
     :result {:first  [1 2 3 4 5 6]
              :second "second"
              :third  "third"})

   (merge-test
     :description "combines when left is fn returning map and right is map"
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] {:first [3 4]})
     :right-attribute {:first [5 6] :third "third"}
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context nil
     :result {:first  [1 2 3 4 5 6]
              :second "second"
              :third  "third"})])

(deftest merge-actions
  (doseq [action-test action-tests]
    (testing (:description action-test)
      (let [merged (core/merge-actions
                     (get-in action-test [:left :attribute])
                     (get-in action-test [:right :attribute]))]
        (is (= (:result action-test)
              (merged (:context action-test)))
          (str "result in " (:description action-test)))
        (is (= (get-in action-test [:left :context :expected])
              @(get-in action-test [:left :context :actual]))
          (str "left context in " (:description action-test)))
        (is (= (get-in action-test [:right :context :expected])
              @(get-in action-test [:right :context :actual]))
          (str "right context in " (:description action-test)))))))

(def decision-tests
  [(merge-test
     :description (str "returns true and context when left and right both "
                    "return true")
     :context {:important "stuff"}
     :left-attribute (fn [_] true)
     :right-attribute (fn [_] true)
     :expected-left-context {:important "stuff"}
     :expected-right-context {:important "stuff"}
     :result [true nil]
     :comparator core/and-comparator)

   (merge-test
     :description (str "returns false and context when left returns true "
                    "and right returns false")
     :context {:important "stuff"}
     :left-attribute (fn [_] true)
     :right-attribute (fn [_] false)
     :expected-left-context {:important "stuff"}
     :expected-right-context {:important "stuff"}
     :result [true nil]
     :comparator core/or-comparator)

   (merge-test
     :description (str "returns false and context when left returns false "
                    "and right returns true")
     :context {:important "stuff"}
     :left-attribute (fn [_] false)
     :right-attribute (fn [_] true)
     :expected-left-context {:important "stuff"}
     :expected-right-context {:important "stuff"}
     :result [false nil]
     :comparator core/and-comparator)

   (merge-test
     :description (str "returns false and context when left returns true "
                    "and right returns false")
     :context {:important "stuff"}
     :left-attribute (fn [_] true)
     :right-attribute (fn [_] false)
     :expected-left-context {:important "stuff"}
     :expected-right-context {:important "stuff"}
     :result [false nil]
     :comparator core/and-comparator)

   (merge-test
     :description (str "returns false and context when both left and right "
                    "returns false")
     :context {:important "stuff"}
     :left-attribute (fn [_] false)
     :right-attribute (fn [_] false)
     :expected-left-context {:important "stuff"}
     :expected-right-context {:important "stuff"}
     :result [false nil]
     :comparator core/and-comparator)

   (merge-test
     :description (str "returns true and context when left and right both "
                    "return true and context")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] [true {:first [3 4]}])
     :right-attribute (fn [_] [true {:first [5 6] :third "third"}])
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [1 2 3 4] :second "second"}
     :result [true {:first [3 4 5 6]
                    :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "returns false and context when left returns false "
                    "and right returns true")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] [false {:first [3 4]}])
     :right-attribute (fn [_] [true {:first [5 6] :third "third"}])
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [1 2 3 4] :second "second"}
     :result [false {:first [3 4 5 6]
                     :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "returns false and context when left returns false "
                    "and right returns true")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] [true {:first [3 4]}])
     :right-attribute (fn [_] [false {:first [5 6] :third "third"}])
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [1 2 3 4] :second "second"}
     :result [false {:first [3 4 5 6]
                     :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "returns false and context when both left and right "
                    "returns false")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] [false {:first [3 4]}])
     :right-attribute (fn [_] [false {:first [5 6] :third "third"}])
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [1 2 3 4] :second "second"}
     :result [false {:first [3 4 5 6]
                     :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines results when both decisions are fns returning "
                    "vectors with maps in the second position")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] [true {:first [3 4]}])
     :right-attribute (fn [_] [true {:first [5 6] :third "third"}])
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [1 2 3 4] :second "second"}
     :result [true {:first [3 4 5 6]
                    :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description "executes left before right when both decisions are fns"
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] [true {:first ^:replace [3 4]}])
     :right-attribute (fn [_] [true {:first ^:replace [5 6] :third "third"}])
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [3 4] :second "second"}
     :result [true {:first ^:replace [5 6]
                    :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "takes result from second fn when both decisions are "
                    "fns returning vectors with fns in the second position")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] [true (fn [] {:first [3 4]})])
     :right-attribute (fn [_] [true (fn [] {:first [5 6] :third "third"})])
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [3 4]}
     :result [true {:first [5 6]
                    :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines results when first fn returns vector with "
                    "fn in second position and second returns vector with "
                    "map in second position")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] [true (fn [] {:first [3 4]})])
     :right-attribute (fn [_] [true {:first [5 6] :third "third"}])
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [3 4]}
     :result [true {:first [3 4 5 6]
                    :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "takes result from second fn when first fn returns "
                    "vector with map in second position and second returns "
                    "vector with fn in second position")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] [true {:first [3 4]}])
     :right-attribute (fn [_] [true (fn [] {:first [5 6] :third "third"})])
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context {:first [1 2 3 4] :second "second"}
     :result [true {:first [5 6]
                    :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines when both left and right are vectors with "
                    "maps in second position")
     :context {:first [1 2] :second "second"}
     :left-attribute [true {:first [3 4]}]
     :right-attribute [true {:first [5 6] :third "third"}]
     :expected-left-context nil
     :expected-right-context nil
     :result [true {:first [3 4 5 6]
                    :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines when left is vector with map in second "
                    "position and right is vector with fn returning map in "
                    "second position")
     :context {:first [1 2] :second "second"}
     :left-attribute [true {:first [3 4]}]
     :right-attribute (fn [_] [true {:first [5 6] :third "third"}])
     :expected-left-context nil
     :expected-right-context {:first [1 2 3 4] :second "second"}
     :result [true {:first [3 4 5 6]
                    :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines when left is vector with fn returning map in "
                    "second position and right is vector with map in second "
                    "position")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] [true {:first [3 4]}])
     :right-attribute [true {:first [5 6] :third "third"}]
     :expected-left-context {:first [1 2] :second "second"}
     :expected-right-context nil
     :result [true {:first [3 4 5 6]
                    :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines left with context when left is map and right "
                    "is true")
     :context {:first [1 2] :second "second"}
     :left-attribute {:first [3 4] :third "third"}
     :right-attribute true
     :expected-left-context nil
     :expected-right-context nil
     :result [true {:first [3 4] :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines right with context when left is true and "
                    "right is map")
     :context {:first [1 2] :second "second"}
     :left-attribute true
     :right-attribute {:first [3 4] :third "third"}
     :expected-left-context nil
     :expected-right-context nil
     :result [true {:first [3 4] :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines right with context when left is false and "
                    "right is map with or comparator")
     :context {:first [1 2] :second "second"}
     :left-attribute false
     :right-attribute {:first [3 4] :third "third"}
     :expected-left-context nil
     :expected-right-context nil
     :result [true {:first [3 4] :third "third"}]
     :comparator core/or-comparator)

   (merge-test
     :description (str "combines right with context when left is false and "
                    "right is map")
     :context {:first [1 2] :second "second"}
     :left-attribute false
     :right-attribute {:first [3 4] :third "third"}
     :expected-left-context nil
     :expected-right-context nil
     :result [false {:first [3 4] :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines left with context when right is false and "
                    "left is map")
     :context {:first [1 2] :second "second"}
     :left-attribute {:first [3 4] :third "third"}
     :right-attribute false
     :expected-left-context nil
     :expected-right-context nil
     :result [false {:first [3 4] :third "third"}]
     :comparator core/and-comparator)

   (merge-test
     :description "combines left and right with context when both are maps"
     :context {:first [1 2] :second "second"}
     :left-attribute {:first [3 4] :third "third"}
     :right-attribute {:first [5 6] :fourth "fourth"}
     :expected-left-context nil
     :expected-right-context nil
     :result [true {:first  [3 4 5 6]
                    :third  "third"
                    :fourth "fourth"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines left and second slot of right with context "
                    "when left is map and right is vector of true and map")
     :context {:first [1 2] :second "second"}
     :left-attribute {:first [3 4] :third "third"}
     :right-attribute [true {:first [5 6] :fourth "fourth"}]
     :expected-left-context nil
     :expected-right-context nil
     :result [true {:first  [3 4 5 6]
                    :third  "third"
                    :fourth "fourth"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines left and second slot of right with context "
                    "when left is map and right is vector of false and map")
     :context {:first [1 2] :second "second"}
     :left-attribute {:first [3 4] :third "third"}
     :right-attribute [false {:first [5 6] :fourth "fourth"}]
     :expected-left-context nil
     :expected-right-context nil
     :result [false {:first  [3 4 5 6]
                     :third  "third"
                     :fourth "fourth"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines second slot of left and right with context "
                    "when left is vector of true and map and right is map")
     :context {:first [1 2] :second "second"}
     :left-attribute [true {:first [3 4] :third "third"}]
     :right-attribute {:first [5 6] :fourth "fourth"}
     :expected-left-context nil
     :expected-right-context nil
     :result [true {:first  [3 4 5 6]
                    :third  "third"
                    :fourth "fourth"}]
     :comparator core/and-comparator)

   (merge-test
     :description (str "combines second slot of left and right with context "
                    "when left is vector of false and map and right is map")
     :context {:first [1 2] :second "second"}
     :left-attribute [false {:first [3 4] :third "third"}]
     :right-attribute {:first [5 6] :fourth "fourth"}
     :expected-left-context nil
     :expected-right-context nil
     :result [false {:first  [3 4 5 6]
                     :third  "third"
                     :fourth "fourth"}]
     :comparator core/and-comparator)])

(deftest merge-decisions
  (doseq [decision-test decision-tests]
    (testing (:description decision-test)
      (let [merged (core/merge-decisions
                     (get-in decision-test [:left :attribute])
                     (get-in decision-test [:right :attribute])
                     (get decision-test :comparator))]
        (is (= (:result decision-test)
              (merged (:context decision-test)))
          (str "result in " (:description decision-test)))
        (is (= (get-in decision-test [:left :context :expected])
              @(get-in decision-test [:left :context :actual]))
          (str "left context in " (:description decision-test)))
        (is (= (get-in decision-test [:right :context :expected])
              @(get-in decision-test [:right :context :actual]))
          (str "right context in " (:description decision-test)))))))

(def handler-tests
  [(merge-test
     :description (str "returns result from right handler when both handlers "
                    "are fns")
     :context {:first [1 2] :second "second"}
     :left-attribute (fn [_] (throw "Should never get called."))
     :right-attribute (fn [_] {:first [5 6] :third "third"})
     :expected-left-context nil
     :expected-right-context {:first [1 2] :second "second"}
     :result {:first [5 6] :third "third"})])

(deftest merge-handlers
  (doseq [handler-test handler-tests]
    (testing (:description handler-test)
      (let [merged (core/merge-handlers
                     (get-in handler-test [:left :attribute])
                     (get-in handler-test [:right :attribute]))]
        (is (= (:result handler-test)
              (merged (:context handler-test)))
          (str "result in " (:description handler-test)))
        (is (= (get-in handler-test [:left :context :expected])
              @(get-in handler-test [:left :context :actual]))
          (str "left context in " (:description handler-test)))
        (is (= (get-in handler-test [:right :context :expected])
              @(get-in handler-test [:right :context :actual]))
          (str "right context in " (:description handler-test)))))))

(def configuration-tests
  [(merge-test
     :description (str "reverse concatenates sequences when both right and "
                    "left are sequences")
     :context {}
     :left-attribute [:first :second :third]
     :right-attribute [:fourth :fifth :sixth]
     :expected-left-context nil
     :expected-right-context nil
     :result [:fourth :fifth :sixth :first :second :third])

   (merge-test
     :description (str "reverse concatenates sequences when left is a "
                    "sequence and right is a function")
     :context {:important :things}
     :left-attribute [:first :second :third]
     :right-attribute (fn [_] [:fourth :fifth :sixth])
     :expected-left-context nil
     :expected-right-context {:important :things}
     :result [:fourth :fifth :sixth :first :second :third])

   (merge-test
     :description (str "reverse concatenates sequences when left is a "
                    "function and right is a sequence")
     :context {:important :things}
     :left-attribute (fn [_] [:first :second :third])
     :right-attribute [:fourth :fifth :sixth]
     :expected-left-context {:important :things}
     :expected-right-context nil
     :result [:fourth :fifth :sixth :first :second :third])

   (merge-test
     :description (str "reverse concatenates sequences when left is a "
                    "function and right is a function")
     :context {:important :things}
     :left-attribute (fn [_] [:first :second :third])
     :right-attribute (fn [_] [:fourth :fifth :sixth])
     :expected-left-context {:important :things}
     :expected-right-context {:important :things}
     :result [:fourth :fifth :sixth :first :second :third])

   (merge-test
     :description (str "replaces when both sequences and right sequence "
                    "includes replace meta")
     :context {:important :things}
     :left-attribute [:first :second :third]
     :right-attribute ^:replace [:fourth :fifth :sixth]
     :expected-left-context nil
     :expected-right-context nil
     :result [:fourth :fifth :sixth])

   (merge-test
     :description (str "replaces when left is sequence, right is fn and "
                    "right result includes replace meta")
     :context {:important :things}
     :left-attribute [:first :second :third]
     :right-attribute (fn [_] ^:replace [:fourth :fifth :sixth])
     :expected-left-context nil
     :expected-right-context {:important :things}
     :result [:fourth :fifth :sixth])

   (merge-test
     :description (str "replaces when left is fn, right is sequence and "
                    "includes replace meta")
     :context {:important :things}
     :left-attribute (fn [_] [:first :second :third])
     :right-attribute ^:replace [:fourth :fifth :sixth]
     :expected-left-context {:important :things}
     :expected-right-context nil
     :result [:fourth :fifth :sixth])

   (merge-test
     :description (str "replaces when left is fn, right is fn and result"
                    "includes replace meta")
     :context {:important :things}
     :left-attribute (fn [_] [:first :second :third])
     :right-attribute (fn [_] ^:replace [:fourth :fifth :sixth])
     :expected-left-context {:important :things}
     :expected-right-context {:important :things}
     :result [:fourth :fifth :sixth])])

(deftest merge-configurations
  (doseq [configuration-test configuration-tests]
    (testing (:description configuration-test)
      (let [merged (core/merge-configurations
                     (get-in configuration-test [:left :attribute])
                     (get-in configuration-test [:right :attribute]))]
        (is (= (:result configuration-test)
              (merged (:context configuration-test)))
          (str "result in " (:description configuration-test)))
        (is (= (get-in configuration-test [:left :context :expected])
              @(get-in configuration-test [:left :context :actual]))
          (str "left context in " (:description configuration-test)))
        (is (= (get-in configuration-test [:right :context :expected])
              @(get-in configuration-test [:right :context :actual]))
          (str "right context in " (:description configuration-test)))))))

(deftest merge-resource-definitions
  (doseq [action-name ts/action-names
          action-test action-tests]
    (testing (str "for action " action-name " " (:description action-test))
      (let [merged (core/merge-resource-definitions
                     {action-name (get-in action-test [:left :attribute])}
                     {action-name (get-in action-test [:right :attribute])})]
        (is (= (:result action-test)
              ((get merged action-name) (:context action-test)))
          (str "result in " (:description action-test)))
        (is (= (get-in action-test [:left :context :expected])
              @(get-in action-test [:left :context :actual]))
          (str "left context in " (:description action-test)))
        (is (= (get-in action-test [:right :context :expected])
              @(get-in action-test [:right :context :actual]))
          (str "right context in " (:description action-test))))))

  (let [and-tests
        [(merge-test
           :description "return false from and when true and false"
           :context {:important "stuff"}
           :left-attribute (fn [_] true)
           :right-attribute (fn [_] false)
           :expected-left-context {:important "stuff"}
           :expected-right-context {:important "stuff"}
           :result [false nil])
         (merge-test
           :description "return false from and when false and false"
           :context {:important "stuff"}
           :left-attribute (fn [_] false)
           :right-attribute (fn [_] false)
           :expected-left-context {:important "stuff"}
           :expected-right-context {:important "stuff"}
           :result [false nil])
         (merge-test
           :description "return true from and when true and true"
           :context {:important "stuff"}
           :left-attribute (fn [_] true)
           :right-attribute (fn [_] true)
           :expected-left-context {:important "stuff"}
           :expected-right-context {:important "stuff"}
           :result [true nil])]]

    (doseq [decision-name ts/decision-names-and
            decision-test and-tests]
      (testing (str "for decision " decision-name " "
                 (:description decision-test))
        (let [merged (core/merge-resource-definitions
                       {decision-name
                        (get-in decision-test [:left :attribute])}
                       {decision-name
                        (get-in decision-test [:right :attribute])})]
          (is (= (:result decision-test)
                ((get merged decision-name) (:context decision-test)))
            (str "result in " (:description decision-test)))
          (is (= (get-in decision-test [:left :context :expected])
                @(get-in decision-test [:left :context :actual]))
            (str "left context in " (:description decision-test)))
          (is (= (get-in decision-test [:right :context :expected])
                @(get-in decision-test [:right :context :actual]))
            (str "right context in " (:description decision-test)))))))

  (let [or-tests
        [(merge-test
           :description "return true from or when true and false"
           :context {:important "stuff"}
           :left-attribute (fn [_] true)
           :right-attribute (fn [_] false)
           :expected-left-context {:important "stuff"}
           :expected-right-context {:important "stuff"}
           :result [true nil])
         (merge-test
           :description "return false from or when false and false"
           :context {:important "stuff"}
           :left-attribute (fn [_] false)
           :right-attribute (fn [_] false)
           :expected-left-context {:important "stuff"}
           :expected-right-context {:important "stuff"}
           :result [false nil])
         (merge-test
           :description "return true from or when true and true"
           :context {:important "stuff"}
           :left-attribute (fn [_] true)
           :right-attribute (fn [_] true)
           :expected-left-context {:important "stuff"}
           :expected-right-context {:important "stuff"}
           :result [true nil])
         (merge-test
           :description "return true from or when false and map"
           :context {:important "stuff"}
           :left-attribute (fn [_] {:important2 "stuff2"})
           :right-attribute (fn [_] false)
           :expected-left-context {:important "stuff"}
           :expected-right-context {:important "stuff" :important2 "stuff2"}
           :result [true {:important2 "stuff2"}])]]

    (doseq [decision-name ts/decision-names-or
            decision-test or-tests]
      (testing (str "for decision " decision-name " "
                 (:description decision-test))
        (let [merged (core/merge-resource-definitions
                       {decision-name
                        (get-in decision-test [:left :attribute])}
                       {decision-name
                        (get-in decision-test [:right :attribute])})]
          (is (= (:result decision-test)
                ((get merged decision-name) (:context decision-test)))
            (str "result in " (:description decision-test)))
          (is (= (get-in decision-test [:left :context :expected])
                @(get-in decision-test [:left :context :actual]))
            (str "left context in " (:description decision-test)))
          (is (= (get-in decision-test [:right :context :expected])
                @(get-in decision-test [:right :context :actual]))
            (str "right context in " (:description decision-test)))))))

  (doseq [handler-name ts/handler-names
          handler-test handler-tests]
    (testing (str "for handler " handler-name " "
               (:description handler-test))
      (let [merged (core/merge-resource-definitions
                     {handler-name
                      (get-in handler-test [:left :attribute])}
                     {handler-name
                      (get-in handler-test [:right :attribute])})]
        (is (= (:result handler-test)
              ((get merged handler-name) (:context handler-test)))
          (str "result in " (:description handler-test)))
        (is (= (get-in handler-test [:left :context :expected])
              @(get-in handler-test [:left :context :actual]))
          (str "left context in " (:description handler-test)))
        (is (= (get-in handler-test [:right :context :expected])
              @(get-in handler-test [:right :context :actual]))
          (str "right context in " (:description handler-test))))))

  (doseq [configuration-name ts/configuration-names
          configuration-test configuration-tests]
    (testing (str "for configuration " configuration-name " "
               (:description configuration-test))
      (let [merged (core/merge-resource-definitions
                     {configuration-name
                      (get-in configuration-test [:left :attribute])}
                     {configuration-name
                      (get-in configuration-test [:right :attribute])})]
        (is (= (:result configuration-test)
              ((get merged configuration-name) (:context configuration-test)))
          (str "result in " (:description configuration-test)))
        (is (= (get-in configuration-test [:left :context :expected])
              @(get-in configuration-test [:left :context :actual]))
          (str "left context in " (:description configuration-test)))
        (is (= (get-in configuration-test [:right :context :expected])
              @(get-in configuration-test [:right :context :actual]))
          (str "right context in " (:description configuration-test)))))))

(def fake-request
  {:protocol       "HTTP/1.1"
   :server-port    80
   :server-name    "localhost"
   :remote-addr    "127.0.0.1"
   :uri            "/"
   :scheme         :http
   :request-method :get
   :headers        {"host" "localhost"}})

(deftest resource-with-merged-decision-builds-correct-context
  (let [resource (core/build-resource
                   {:initialize-context
                    (fn [_]
                      {:a-vector [1]})

                    :allowed? true}

                   {:allowed? true

                    :handle-ok
                    (fn [context]
                      (str (:a-vector context)))})
        response (resource fake-request)]

    (is (= "[1]" (:body response)))))

(deftest resource-with-merged-action-builds-correct-context
  (let [resource (core/build-resource
                   {:initialize-context
                    (fn [_]
                      {:a-vector [1]})}

                   {:initialize-context
                    (fn [_]
                      {:a-vector [2]})

                    :handle-ok
                    (fn [context]
                      (str (:a-vector context)))})
        response (resource fake-request)]

    (is (= "[1 2]" (:body response)))))

(deftest resource-with-merged-handlers-returns-correct-response
  (let [resource (core/build-resource
                   {:handle-ok
                    (fn [_]
                      (str "bad"))}

                   {:handle-ok
                    (fn [_]
                      "good")})
        response (resource fake-request)]

    (is (= "good" (:body response)))))