(ns joodo.default-rendering
  "This namespace is used by default when rendering.  It contains commonly used view helper functions."
  (:use
    [joodo.views :only (render-partial *view-context*)]
    [hiccup.page]
    [hiccup.form]))
