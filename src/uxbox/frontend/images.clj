;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2016 Andrey Antukh <niwi@niwi.nz>

(ns uxbox.frontend.images
  (:require [promesa.core :as p]
            [catacumba.http :as http]
            [storages.core :as st]
            [uxbox.media :as media]
            [uxbox.images :as images]
            [uxbox.schema :as us]
            [uxbox.services :as sv]
            [uxbox.services.images :as si]
            [uxbox.util.response :refer (rsp)]
            [uxbox.util.uuid :as uuid]
            [uxbox.util.paths :as paths]))

;; --- Constants & Config

(def validate-form! (partial us/validate! :form/validation))
(def validate-query! (partial us/validate! :query/validation))

;; --- Create Collection

(def create-collection-scheme si/create-collection-scheme)

(defn create-collection
  [{user :identity data :data}]
  (let [data (validate-form! data create-collection-scheme)
        message (assoc data
                       :type :create/image-collection
                       :user user)]
    (->> (sv/novelty message)
         (p/map (fn [result]
                  (let [loc (str "/api/library/images/" (:id result))]
                    (http/created loc (rsp result))))))))

;; --- Update Collection

(def update-collection-scheme si/update-collection-scheme)

(defn update-collection
  [{user :identity params :route-params data :data}]
  (let [data (validate-form! data update-collection-scheme)
        message (assoc data
                       :id (uuid/from-string (:id params))
                       :type :update/image-collection
                       :user user)]
    (-> (sv/novelty message)
        (p/then #(http/ok (rsp %))))))

;; --- Delete Collection

(defn delete-collection
  [{user :identity params :route-params}]
  (let [message {:id (uuid/from-string (:id params))
                 :type :delete/image-collection
                 :user user}]
    (-> (sv/novelty message)
        (p/then (fn [v] (http/no-content))))))

;; --- List collections

(defn list-collections
  [{user :identity}]
  (let [params {:user user :type :list/image-collections}]
    (-> (sv/query params)
        (p/then #(http/ok (rsp %))))))

;; --- Create image

(def create-image-schema
  {:id [us/uuid]
   :file [us/required us/uploaded-file]})

(defn create-image
  [{user :identity data :data}]
  (let [{:keys [file id] :as data} (validate-form! data create-image-schema)
        id (or id (uuid/random))
        filename (paths/base-name file)
        extension (paths/extension filename)
        storage media/images-storage]
    (letfn [(persist-image-entry [path]
              (sv/novelty {:id id
                           :type :create/image
                           :user user
                           :name filename
                           :path (str path)}))

            (populate-thumbnails [entry]
              (let [opts {:src :path
                          :dst :thumbnail
                          :size [300 300]
                          :quality 93
                          :format "webp"}]
                (images/populate-thumbnails entry opts)))

            (populate-urls [v]
              (images/populate-urls v storage :path :url))

            (create-response [entry]
              (let [loc (str "/api/library/images/" (:id entry))]
                (http/created loc (rsp entry))))]

      (->> (st/save storage filename file)
           (p/mapcat persist-image-entry)
           (p/map populate-thumbnails)
           (p/map populate-urls)
           (p/map create-response)))))

;; --- Update Image

(def update-image-schema
  {:id [us/uuid]
   :name [us/required us/string]
   :version [us/required us/integer]})

(defn update-image
  [{user :identity params :route-params data :data}]
  (let [data (validate-form! data update-image-schema)
        message (assoc data
                       :id (uuid/from-string (:id params))
                       :type :update/image
                       :user user)]
    (-> (sv/novelty message)
        (p/then #(http/ok (rsp %))))))

;; --- Delete Image

(defn delete-image
  [{user :identity params :route-params}]
  (let [message {:id (uuid/from-string (:id params))
                 :type :delete/image
                 :user user}]
    (-> (sv/novelty message)
        (p/then (fn [v] (http/no-content))))))

;; --- List collections

(defn list-images
  [{user :identity}]
  (let [params {:user user :type :list/images}]
    (-> (sv/query params)
        (p/then #(http/ok (rsp %))))))
