(ns io.github.michalmela.cogni-java-credentials-test
  (:require [clojure.test :refer :all]
            [cognitect.aws.credentials :as c]
            [io.github.michalmela.cogni-java-credentials :as sut])
  (:import (java.time Duration Instant)
           (java.util Optional)
           (software.amazon.awssdk.auth.credentials.internal SystemSettingsCredentialsProvider)
           (software.amazon.awssdk.core SdkSystemSetting)))

(deftest jvm-default-credentials-provider-test
  (testing "it should fetch credentials and convert them to the aws-api format"
    (let [original-kid (System/getProperty "aws.accessKeyId")
          original-sak (System/getProperty "aws.secretAccessKey")]
      (try
        (System/setProperty "aws.accessKeyId" "dummy-kid")
        (System/setProperty "aws.secretAccessKey" "dummy-sak")
        (let [aws-sd-v2-credprov (sut/as-cognitect-provider)
              cognitect-creds (c/fetch aws-sd-v2-credprov)]
          (is (= "dummy-kid" (:aws/access-key-id cognitect-creds)))
          (is (= "dummy-sak" (:aws/secret-access-key cognitect-creds)))
          (is (= nil (:cognitect.aws.credentials/ttl cognitect-creds))))
        (finally
          (if original-kid
            (System/setProperty "aws.accessKeyId" original-kid)
            (System/clearProperty "aws.accessKeyId"))
          (if original-sak
            (System/setProperty "aws.secretAccessKey" original-sak)
            (System/clearProperty "aws.secretAccessKey")))))))

(deftest explicit-credentials-provider-test
  (testing "it should fetch credentials from an explicit AwsCredentialsProvider"
    (let [dummy-credprov (proxy [SystemSettingsCredentialsProvider] []
                           (provider [] "TestProvider")
                           (loadSetting [^SdkSystemSetting setting-name]
                             (Optional/of ((-> setting-name .property keyword)
                                           {:aws.accountId       "explicit-account-id"
                                            :aws.accessKeyId     "explicit-kid"
                                            :aws.secretAccessKey "explicit-sak"
                                            :aws.sessionToken    "explicit-st"
                                            :aws.expiration      (-> (Instant/now)
                                                                     (.plus (Duration/ofHours 1))
                                                                     (.toString))}))))
          aws-sd-v2-credprov (sut/as-cognitect-provider dummy-credprov)
          cognitect-creds (c/fetch aws-sd-v2-credprov)]
      (is (= "explicit-kid" (:aws/access-key-id cognitect-creds)))
      (is (= "explicit-sak" (:aws/secret-access-key cognitect-creds)))
      (is (= "explicit-st" (:aws/session-token cognitect-creds)))
      (is (< 3550 (:cognitect.aws.credentials/ttl cognitect-creds)))
      (is (< (:cognitect.aws.credentials/ttl cognitect-creds) 3601)))))
