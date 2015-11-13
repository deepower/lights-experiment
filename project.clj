(defproject lights "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [afterglow "0.1.5-SNAPSHOT"]
                 [com.evocomputing/colors "1.0.3"]]
  :main ^:skip-aot lights.core
  :target-path "target/%s"
  :profiles {:dev {:repl-options {:init-ns lights.my-show
                                  :welcome (println "my-show loaded.")}
                   :jvm-opts ["-XX:-OmitStackTraceInFastThrow" "-Dapple.awt.UIElement=true"]
                   :env {:dev true}}
             :uberjar {:env {:production true}
                       :aot :all}})
