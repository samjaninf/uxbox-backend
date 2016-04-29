;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.util.snappy
  "A lightweight abstraction layer for snappy compression library."
  (:require [buddy.core.codecs :as codecs])
  (:import org.xerial.snappy.Snappy))

(defn compress
  "Compress data unsing snappy compression algorithm."
  [data]
  (-> (codecs/to-bytes data)
      (Snappy/compress)))

(defn uncompress
  "Uncompress data using snappy compression algorithm."
  [data]
  (-> (codecs/to-bytes data)
      (Snappy/uncompress)))