(defproject lights "0.1.0-SNAPSHOT"
  :description "Lights experiment"
  :url "https://github.com/dandaka/lights-experiment"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0-RC5"]
                 [afterglow "0.2.0-SNAPSHOT"]]
  :main ^:skip-aot lights.core
  :target-path "target/%s"
  :profiles {:dev {:repl-options {:init-ns lights.my-show
                                  :welcome (println "my-show loaded.")}
                   :jvm-opts ["-XX:-OmitStackTraceInFastThrow" "-Dapple.awt.UIElement=true"]
                   :env {:dev true}}
             :uberjar {:env {:production true}
                       :aot :all}})
