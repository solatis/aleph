(ns aleph.http.core-test
  (:use
    [clojure test])
  (:require
   [aleph.http.core :as core]
   [aleph.http :as http]
   
   [byte-streams :as bs]

   [clojure.java.io :refer [file]])
  (:import
    [io.netty.handler.codec.http
     DefaultHttpRequest]))

(defn handler [req]
  (println "req = " (pr-str req ))
  (condp = [(:request-method req) (:uri req)]
    [:post "/multipart"]    
    {:status 200 :body (:body req)}))

(defn run-server
  []
  (defonce server
    (http/start-server #'handler {:port 18080})))

(defn localhost [path]
  (str "http://localhost:18080" path))

(deftest ^:integration multipart-form-uploads
  (run-server)
  (let [bytes (bs/to-byte-array "hello, world!")
        stream (bs/to-input-stream bytes)
        resp @(http/post (localhost "/multipart")
                         {:multipart [{:name "a" :content "testFINDMEtest"
                                       :encoding "UTF-8"
                                       :mime-type "application/text"}
                                      {:name "b" :content bytes
                                       :mime-type "application/json"}
                                      {:name "e" :part-name "eggplant"
                                       :content "content"
                                       :mime-type "application/text"}]})
        resp-body (bs/to-string (:body resp))]
    (is (= 200 (:status resp)))
    (is (re-find #"testFINDMEtest" resp-body))
    (is (re-find #"application/json" resp-body))
    (is (re-find #"application/text" resp-body))
    (is (re-find #"UTF-8" resp-body))
    (is (re-find #"byte-test" resp-body))
    (is (re-find #"name=\"c\"" resp-body))
    (is (re-find #"name=\"d\"" resp-body))
    (is (re-find #"name=\"eggplant\"" resp-body))
    (is (re-find #"content" resp-body))))

(deftest test-HeaderMap-keys
  (let [^DefaultHttpRequest req (core/ring-request->netty-request
                                  {:uri "http://example.com"
                                   :request-method "get"
                                   :headers {"Accept" "text/html"
                                             "Authorization" "Basic narfdorfle"}})
        map (core/headers->map (.headers req))
        dissoc-map (dissoc map "authorization")]
    (is (= #{"accept"}  (-> dissoc-map keys set)))))
