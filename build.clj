(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.string :as str]))

(def lib 'io.github.michalmela/cogni-java-credentials)
(def version (delay
               (let [raw-version (b/git-process {:git-args "describe --tags --always --dirty"})]
                 (str/replace raw-version #"^v" ""))))
(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn pom [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version @version
                :basis @basis
                :src-dirs ["src"]
                :scm {:url "https://github.com/michalmela/cogni-java-credentials"
                      :connection "scm:git:git://github.com/michalmela/cogni-java-credentials.git"
                      :developerConnection "scm:git:ssh://git@github.com/michalmela/cogni-java-credentials.git"
                      :tag @version}
                :pom-data [[:description "AWS credentials provider using JVM SDK for Clojure"]
                           [:licenses
                            [:license
                             [:name "MIT License"]
                             [:url "https://opensource.org/licenses/MIT"]]]]})
  (println "Generated POM at" (b/pom-path {:lib lib :class-dir class-dir})))

(defn jar [_]
  (clean nil)
  (pom nil)
  (let [jar-file (format "target/%s-%s.jar" (name lib) @version)]
    (b/copy-dir {:src-dirs ["src"]
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file jar-file})))

(defn deploy [_]
  (jar nil)
  (let [jar-file (format "target/%s-%s.jar" (name lib) @version)
        pom-file (b/pom-path {:lib lib :class-dir class-dir})]
    (println "Deploying" jar-file "to Clojars...")
    (let [result (b/process {:command-args ["mvn" "deploy:deploy-file"
                                            (str "-Dfile=" jar-file)
                                            (str "-DpomFile=" pom-file)
                                            "-DrepositoryId=clojars"
                                            "-Durl=https://clojars.org/repo"]})]
      (when (not= 0 (:exit result))
        (throw (ex-info "Maven deploy failed" result))))))
