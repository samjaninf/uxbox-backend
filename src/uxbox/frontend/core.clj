;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.core
  (:require [uxbox.util.exceptions :as ex]
            [struct.core :as st]))

(defn validate!
  [data schema]
  (let [[errors data] (st/validate data schema)]
    (if (seq errors)
      (throw (ex/ex-info :validation errors))
      data)))
