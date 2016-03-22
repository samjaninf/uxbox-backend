(ns uxbox.tests.test-projects
  (:require [clojure.test :as t]
            [promesa.core :as p]
            [suricatta.core :as sc]
            [clj-uuid :as uuid]
            [clj-http.client :as http]
            [catacumba.testing :refer (with-server)]
            [catacumba.serializers :as sz]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [uxbox.persistence :as up]
            [uxbox.frontend.routes :as urt]
            [uxbox.services.auth :as usa]
            [uxbox.services.projects :as uspr]
            [uxbox.services.pages :as uspg]
            [uxbox.services :as usv]
            [uxbox.tests.helpers :as th]))

(t/use-fixtures :each th/database-reset)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Services Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn data-encode
  [data]
  (-> (sz/encode data :transit+msgpack)
      (codecs/bytes->base64)))

(defn create-user
  "Helper for create users"
  [conn i]
  (let [data {:username (str "user" i)
              :password  (hashers/encrypt (str "user" i))
              :email (str "user" i "@uxbox.io")}]
    (usa/create-user conn data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Frontend Test
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:const +base-url
  "http://localhost:5050")

(t/deftest test-http-project-list
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url "/api/projects")
              [status data] (th/http-get user uri)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= 1 (count data))))))))

(t/deftest test-http-project-create
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url "/api/projects")
              params {:body {:name "proj1"}}
              [status data] (th/http-post user uri params)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 201 status))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:name data) "proj1")))))))

(t/deftest test-http-project-update
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url "/api/projects/" (:id proj))
              params {:body (assoc proj :name "proj2")}
              [status data] (th/http-put user uri params)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 200 status))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:name data) "proj2")))))))

(t/deftest test-http-project-delete
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url "/api/projects/" (:id proj))
              [status data] (th/http-delete user uri)]
          (t/is (= 204 status))
          (let [sqlv ["SELECT * FROM projects WHERE \"user\"=?" (:id user)]
                result (sc/fetch conn sqlv)]
            (t/is (empty? result))))))))

(t/deftest test-http-page-create
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url "/api/pages")
              params {:body {:project (:id proj)
                             :name "page1"
                             :data [:test]
                             :width 200
                             :height 200
                             :layout "mobile"}}
              [status data] (th/http-post user uri params)]
          ;; (println "RESPONSE:" status data)
          (t/is (= 201 status))
          (t/is (= (:data (:body params)) (:data data)))
          (t/is (= (:user data) (:id user)))
          (t/is (= (:name data) "page1")))))))

(t/deftest test-http-page-update
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})
          data {:id (uuid/v4)
                :user (:id user)
                :project (:id proj)
                :version 0
                :data (data-encode [:test1])
                :name "page1"
                :width 200
                :height 200
                :layout "mobil"}
          page (uspg/create-page conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url (str "/api/pages/" (:id page)))
              params {:body (assoc page :data [:test1 :test2])}
              [status page'] (th/http-put user uri params)]
          ;; (println "RESPONSE:" status page')
          (t/is (= 200 status))
          (t/is (= [:test1 :test2] (:data page')))
          (t/is (= 1 (:version page')))
          (t/is (= (:user page') (:id user)))
          (t/is (= (:name data) "page1")))))))

(t/deftest test-http-page-update-metadata
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})
          data {:id (uuid/v4)
                :user (:id user)
                :project (:id proj)
                :version 0
                :data (data-encode [:test1])
                :name "page1"
                :width 200
                :height 200
                :layout "mobil"}
          page (uspg/create-page conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url (str "/api/pages/" (:id page) "/metadata"))
              params {:body (assoc page :data [:test1 :test2])}
              [status page'] (th/http-put user uri params)]
          ;; (println "RESPONSE:" status page')
          (t/is (= 200 status))
          (t/is (= [:test1] (:data page')))
          (t/is (= 1 (:version page')))
          (t/is (= (:user page') (:id user)))
          (t/is (= (:name data) "page1")))))))


(t/deftest test-http-page-delete
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})
          data {:id (uuid/v4)
                :user (:id user)
                :project (:id proj)
                :version 0
                :data (data-encode [:test1])
                :name "page1"
                :width 200
                :height 200
                :layout "mobil"}
          page (uspg/create-page conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url (str "/api/pages/" (:id page)))
              [status response] (th/http-delete user uri)]
          ;; (println "RESPONSE:" status response)
          (t/is (= 204 status))
          (let [sqlv ["SELECT * FROM pages WHERE \"user\"=?" (:id user)]
                result (sc/fetch conn sqlv)]
            (t/is (empty? result))))))))

(t/deftest test-http-page-list
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj (uspr/create-project conn {:user (:id user) :name "proj1"})
          data {:id (uuid/v4)
                :user (:id user)
                :project (:id proj)
                :version 0
                :data (data-encode [:test1])
                :name "page1"
                :width 200
                :height 200
                :layout "mobil"}
          page (uspg/create-page conn data)]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url (str "/api/pages"))
              [status response] (th/http-get user uri)]
          ;; (println "RESPONSE:" status response)
          (t/is (= 200 status))
          (t/is (= 1 (count response))))))))


(t/deftest test-http-page-list-by-project
  (with-open [conn (up/get-conn)]
    (let [user (create-user conn 1)
          proj1 (uspr/create-project conn {:user (:id user) :name "proj1"})
          proj2 (uspr/create-project conn {:user (:id user) :name "proj2"})
          data {:user (:id user)
                :version 0
                :data (data-encode [])
                :name "page1"
                :width 200
                :height 200
                :layout "mobil"}
          page1 (uspg/create-page conn (assoc data :project (:id proj1)))
          page2 (uspg/create-page conn (assoc data :project (:id proj2)))]
      (with-server {:handler (urt/app)}
        (let [uri (str +base-url (str "/api/projects/" (:id proj1) "/pages"))
              [status response] (th/http-get user uri)]
          ;; (println "RESPONSE:" status response)
          (t/is (= 200 status))
          (t/is (= 1 (count response)))
          (t/is (= (:id (first response)) (:id page1))))))))

