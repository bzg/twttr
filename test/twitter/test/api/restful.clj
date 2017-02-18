(ns twitter.test.api.restful
  (:require [clojure.test :refer :all]
            [twitter.api.restful :refer :all]
            [twitter.test-utils :refer [is-200 user-creds
                                        with-setup-poll-teardown]]))

(def current-user (delay (:body (account-verify-credentials :oauth-creds user-creds))))

(deftest test-account
  (is-200 account-verify-credentials)
  (is-200 application-rate-limit-status)
  (is-200 application-rate-limit-status :app-only)
  (is-200 account-settings))

(deftest test-blocks
  (is-200 blocks-list)
  (is-200 blocks-ids))

(deftest test-timeline
  (is-200 statuses-mentions-timeline)
  (is-200 statuses-user-timeline)
  (is-200 statuses-home-timeline)
  (is-200 statuses-retweets-of-me))

(deftest test-statuses
  (is-200 statuses-lookup :params {:id "20,432656548536401920"})
  (let [status-id (get-in @current-user [:status :id])]
    (is-200 statuses-show-id :params {:id status-id})
    (is-200 statuses-show-id :params {:id status-id} :app-only)
    (is-200 statuses-retweets-id :params {:id status-id})
    (is-200 statuses-retweets-id :params {:id status-id}) :app-only))

(deftest test-search
  (is-200 search-tweets :params {:q "clojure"})
  (is-200 search-tweets :params {:q "clojure"} :app-only))

(deftest test-user
  (let [user-id (:id @current-user)]
    (is-200 users-show :params {:user-id user-id})
    (is-200 users-show :params {:user-id user-id} :app-only)
    (is-200 users-lookup :params {:user-id user-id})
    (is-200 users-lookup :params {:user-id user-id} :app-only)
    (is-200 users-suggestions :params {:q "john smith"})
    (is-200 users-suggestions :params {:q "john smith"} :app-only)
    (is-200 users-suggestions-slug :params {:slug "sports"})
    (is-200 users-suggestions-slug-members :params {:slug "sports"})))

(deftest test-trends
  (is-200 trends-place :params {:id 1})
  (is-200 trends-place :params {:id 1} :app-only)
  (is-200 trends-available)
  (is-200 trends-available :app-only)
  (is-200 trends-closest :params {:lat 37.781157 :long -122.400612831116})
  (is-200 trends-closest :params {:lat 37.781157 :long -122.400612831116} :app-only))

(deftest test-lists-list
  (is-200 lists-list))

(deftest test-lists-memberships
  (is-200 lists-memberships))

(deftest test-lists-subscriptions
  (is-200 lists-subscriptions))

(deftest test-lists-ownerships
  (is-200 lists-ownerships))

(defmacro with-list
  "create a list and then removes it"
  [var-name & body]
  `(with-setup-poll-teardown
     ~var-name
     (get-in (~lists-create :oauth-creds ~user-creds :params {:name "mytestlistblumblum"}) [:body :id])
     (~lists-statuses :oauth-creds ~user-creds :params {:list-id ~var-name})
     (~lists-destroy :oauth-creds ~user-creds :params {:list-id ~var-name})
     ~@body))

(deftest test-lists-statuses
  (with-list list-id
    (is-200 lists-statuses :params {:list-id list-id})))

(deftest test-list-members
  (with-list list-id
    (is-200 lists-members :params {:list-id list-id})
    (is (thrown? Exception (lists-members-show :params {:list-id list-id :screen-name (:screen_name @current-user)})))))

(deftest test-list-subscribers
  (with-list list-id
    (is-200 lists-subscribers :params {:list-id list-id})))

(deftest test-list-subscribers-show
  (with-list list-id
    (is (thrown? Exception (lists-subscribers-show :params {:list-id list-id :screen-name (:screen_name @current-user)})))))

(deftest test-direct-messages
  (is-200 direct-messages)
  (is-200 direct-messages-sent))

(deftest test-friendship
  (is-200 friendships-show :params {:source-screen-name (:screen_name @current-user) :target-screen-name "AdamJWynne"})
  (is-200 friendships-show :params {:source-screen-name (:screen_name @current-user) :target-screen-name "AdamJWynne"} :app-only)
  (is-200 friendships-lookup :params {:screen-name "peat,AdamJWynne"})
  (is-200 friendships-incoming)
  (is-200 friendships-outgoing))

(deftest test-friends-followers
  (is-200 friends-ids)
  (is-200 friends-list)
  (is-200 followers-ids)
  (is-200 followers-list))

(deftest test-favourites
  (let [status-id (get-in @current-user [:status :id])]
    (is-200 favorites-create :params {:id status-id})
    (is-200 favorites-destroy :params {:id status-id})
    (is-200 favorites-list)))

(defmacro with-saved-search
  "create a saved search and then removes it"
  [search-id-name & body]
  `(with-setup-poll-teardown
     ~search-id-name
     (get-in (saved-searches-create :oauth-creds ~user-creds :params {:query "sandwiches"})
             [:body :id])
     (saved-searches-show-id :oauth-creds ~user-creds :params {:id ~search-id-name})
     (saved-searches-destroy-id :oauth-creds ~user-creds :params {:id ~search-id-name})
     ~@body))

(deftest test-saved-searches
  (is-200 saved-searches-list)
  (with-saved-search search-id (is-200 saved-searches-show-id :params {:id search-id})))