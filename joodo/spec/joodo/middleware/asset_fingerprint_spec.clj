(ns joodo.middleware.asset-fingerprint-spec
  (:require [clojure.java.io :as io]
            [speclj.core :refer :all]
            [joodo.env :as env]
            [joodo.middleware.asset-fingerprint :refer :all]))


(describe "asset fingerprint"

  (around [it]
    (with-redefs [env/development? (constantly false)]
      (it)))

  (it "adds an md5 to a path"
    (should= "/some/path.fpabc123.xyz" (add-fingerprint-to-path "abc123" "/some/path.xyz"))
    (should= "/some/path.fpabc456.xyz" (add-fingerprint-to-path "abc456" "/some/path.xyz"))
    (should= "/some/other.fpabc456.abc" (add-fingerprint-to-path "abc456" "/some/other.abc"))
    (should= "/some/other.else.fpabc456.abc" (add-fingerprint-to-path "abc456" "/some/other.else.abc"))
    (should= "/some/extensionless.fpabc123" (add-fingerprint-to-path "abc123" "/some/extensionless"))
    (should= "/some.thing/extensionless.fpabc123" (add-fingerprint-to-path "abc123" "/some.thing/extensionless"))
    (should= "extensionless.fpabc123" (add-fingerprint-to-path "abc123" "extensionless"))
    (should= "file.fpabc123.xyz" (add-fingerprint-to-path "abc123" "file.xyz")))

  (it "removes fingerprint from path"
    (should= "/some/path.xyz" (remove-fingerprint "/some/path.fpabcdefghijklmnopqrstuvwxyz123456.xyz"))
    (should= "/some/path.xyz" (remove-fingerprint "/some/path.fpabcdefghijklmnopqrstuvwxyz123456.xyz"))
    (should= "/some/other.abc" (remove-fingerprint "/some/other.fpabcdefghijklmnopqrstuvwxyz123456.abc"))
    (should= "/some/other.else.abc" (remove-fingerprint "/some/other.else.fpabcdefghijklmnopqrstuvwxyz123456.abc"))
    (should= "/some/extensionless" (remove-fingerprint "/some/extensionless.fpabcdefghijklmnopqrstuvwxyz123456"))
    (should= "/some.thing/extensionless" (remove-fingerprint "/some.thing/extensionless.fpabcdefghijklmnopqrstuvwxyz123456"))
    (should= "extensionless" (remove-fingerprint "extensionless.fpabcdefghijklmnopqrstuvwxyz123456"))
    (should= "file.xyz" (remove-fingerprint "file.fpabcdefghijklmnopqrstuvwxyz123456.xyz")))

  (it "adds checksum to path in classpath"
    (let [path "/middleware/asset_fingerprint_spec.clj"
          result (add-fingerprint "/middleware/asset_fingerprint_spec.clj" "joodo")]
      (should-not= path result)
      (should= path (remove-fingerprint result))))

  (it "fingerprinting is disabled in development environment"
    (with-redefs [env/development? (constantly true)]
      (let [path "/env_spec.clj"
            result (add-fingerprint "/env_spec.clj" "joodo")]
        (should= path result))))

  (it "missing pathspass through"
    (should= "/some/missing/file" (add-fingerprint "/some/missing/file")))

  (it "reuses known fingerprints"
    (let [tally (atom 0)
          path "/middleware/asset_fingerprint.clj"
          response (io/resource (str "joodo" path))]
      (with-redefs [io/resource (fn [p] (swap! tally inc) response)]
        (let [result (add-fingerprint path "joodo")]
          (should-not= path result)
          (should= result (add-fingerprint path "joodo"))))
      (should= 1 @tally)))

  (it "ignored requests without finger prints"
    (let [request {:stuff :blah :uri "/path/without/fingerprint.abc"}]
      (should= request (resolve-fingerprint-in request))))

  (it "resolves fingerprinted assets in request"
    (let [fingerprint "fpabcdefghijklmnopqrstuvwxyz123456"
          request {:stuff :blah :uri (str "/path/with/fingerprint." fingerprint ".abc")}]
      (should=
        {:stuff :blah :uri "/path/with/fingerprint.abc"}
        (resolve-fingerprint-in request))))

  (it "middleware passes resolved requests"
    (let [uri (atom nil)
          inner-handler (fn [request] (reset! uri (:uri request)))
          wrapped-handler (wrap-asset-fingerprint inner-handler)
          fingerprint "fpabcdefghijklmnopqrstuvwxyz123456"
          request {:uri (str "/path/with/fingerprint." fingerprint ".abc")}]
      (wrapped-handler request)
      (should= "/path/with/fingerprint.abc" @uri))))


(run-specs)





