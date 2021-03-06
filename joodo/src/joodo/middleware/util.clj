(ns joodo.middleware.util
  (:require [joodo.env :as env]
            [taoensso.timbre :as timbre]))

(defn- attempt-to-load-var [qualified-sym]
  (let [ns-sym (symbol (namespace qualified-sym))
        var-sym (symbol (name qualified-sym))]
    (try
      (require ns-sym)
      (let [ns (the-ns ns-sym)]
        (ns-resolve ns var-sym))
      (catch Exception e
        (timbre/warn "Failed to load var:" var-sym "from ns:" ns-sym e)
        nil))))

(defn attempt-wrap
  "Attempts to wrap the handler with the specified middlware.
  The middleware-sym must be a namespaced symbol (myproject.some-namespace/wrapper-var).
  The namespace will be loaded, the wrapper var will be resolved and applied to the handler.
  If the var can not be loaded or resolved, an error will be logged and the unwrapped handler returned."
  [handler middleware-sym]
  (if-let [wrapper (attempt-to-load-var middleware-sym)]
    (wrapper handler)
    (do
      (timbre/warn "Bypassing" middleware-sym)
      handler)))

(defn wrap-development-maybe [handler]
  (if (env/development?)
    (-> handler
      (attempt-wrap 'joodo.middleware.verbose/wrap-verbose)
      (attempt-wrap 'joodo.middleware.refresh/wrap-refresh))
    handler))